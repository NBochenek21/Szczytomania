package pl.szczytomania.ui.screens.conquest

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.szczytomania.data.firestore.ConquestRepository
import pl.szczytomania.data.model.Peak
import pl.szczytomania.data.repository.ConqueredPeaks
import pl.szczytomania.data.repository.PeakRepository
import pl.szczytomania.location.SummitCheckResult
import pl.szczytomania.location.SummitChecker
import pl.szczytomania.ui.screens.map.SummitCheckState
import pl.szczytomania.ui.screens.sets.SetsData
import pl.szczytomania.ui.screens.sets.loadSetsData
import java.util.Date

data class ConquestUiState(
    val peak: Peak? = null,
    val check: SummitCheckState = SummitCheckState.Idle,
    /** Po zapisie – ekran "Szczyt zdobyty!". */
    val summary: ConquestSummary? = null,
) {
    /** Wszystkie warunki spełnione – można zrobić zdjęcie i zapisać zdobycz. */
    val passed: Boolean
        get() = ((check as? SummitCheckState.Done)?.result as? SummitCheckResult.Checked)?.check?.passed == true
}

/** Zdobywanie szczytu: sprawdzenie GPS (Kacper), zapis zdobyczy (Jakub) i podsumowanie. */
class ConquestViewModel(
    private val repository: PeakRepository,
    private val checker: SummitChecker,
    private val conquests: ConquestRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ConquestUiState())
    val state: StateFlow<ConquestUiState> = _state.asStateFlow()

    // Zdobyte przed zapisem – do numeru szczytu i rekordu wysokości.
    private val conqueredIds = ConqueredPeaks.ids.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    private var data: SetsData? = null
    private var checkJob: Job? = null

    /** Otwiera zdobywanie szczytu i od razu sprawdza GPS. */
    fun start(peakId: String) {
        checkJob?.cancel()
        _state.value = ConquestUiState()
        viewModelScope.launch {
            val loaded = data ?: runCatching { repository.loadSetsData() }
                .onFailure { Log.e(TAG, "Nie udało się wczytać szczytów", it) }
                .getOrNull()?.also { data = it }
            val peak = loaded?.peaksById?.get(peakId) ?: return@launch
            _state.value = ConquestUiState(peak = peak)
            check()
        }
    }

    fun check() {
        val peak = _state.value.peak ?: return
        checkJob?.cancel()
        _state.update { it.copy(check = SummitCheckState.Checking) }
        checkJob = viewModelScope.launch {
            val result = checker.check(peak)
            _state.update { it.copy(check = SummitCheckState.Done(result)) }
        }
    }

    // Zdjęcie dojdzie z aparatem (Kacper) – na razie zdobycz zapisuje się ze statusem "czeka na zdjęcie".
    fun save() {
        val current = _state.value
        val peak = current.peak ?: return
        val result = (current.check as? SummitCheckState.Done)?.result as? SummitCheckResult.Checked ?: return
        val loaded = data ?: return
        if (!result.check.passed) return
        conquests.save(peak, result.check.reading)
        _state.update { it.copy(summary = ConquestSummary.of(peak, loaded, conqueredIds.value, Date())) }
    }

    companion object {
        private const val TAG = "ConquestViewModel"

        val Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY])
                ConquestViewModel(PeakRepository.get(application.assets), SummitChecker(application), ConqueredPeaks.repository)
            }
        }
    }
}
