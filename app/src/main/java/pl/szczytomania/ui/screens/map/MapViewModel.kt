package pl.szczytomania.ui.screens.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.szczytomania.data.model.Peak
import pl.szczytomania.data.repository.PeakRepository
import pl.szczytomania.location.SummitCheckResult
import pl.szczytomania.location.SummitChecker

/** Stan sprawdzania GPS dla wybranego szczytu. */
sealed interface SummitCheckState {
    data object Idle : SummitCheckState
    data object Checking : SummitCheckState
    data class Done(val result: SummitCheckResult) : SummitCheckState
}

class MapViewModel(
    private val peakRepository: PeakRepository,
    private val summitChecker: SummitChecker,
) : ViewModel() {

    private val _peaks = MutableStateFlow<List<Peak>>(emptyList())
    val peaks: StateFlow<List<Peak>> = _peaks.asStateFlow()

    private val _selectedPeak = MutableStateFlow<Peak?>(null)
    val selectedPeak: StateFlow<Peak?> = _selectedPeak.asStateFlow()

    private val _summitCheck = MutableStateFlow<SummitCheckState>(SummitCheckState.Idle)
    val summitCheck: StateFlow<SummitCheckState> = _summitCheck.asStateFlow()

    private var checkJob: Job? = null

    init {
        viewModelScope.launch {
            runCatching { peakRepository.getAll() }
                .onSuccess { _peaks.value = it }
                // Uszkodzony plik szczytów nie powinien wyłączać mapy – pokazujemy ją bez szczytów.
                .onFailure { Log.e(TAG, "Nie udało się wczytać szczytów", it) }
        }
    }

    fun selectPeak(id: String?) {
        // Wynik poprzedniego sprawdzenia nie może trafić na kartę innego szczytu.
        checkJob?.cancel()
        _summitCheck.value = SummitCheckState.Idle
        _selectedPeak.value = _peaks.value.find { it.id == id }
    }

    fun checkSummit() {
        val peak = _selectedPeak.value ?: return
        checkJob?.cancel()
        _summitCheck.value = SummitCheckState.Checking
        checkJob = viewModelScope.launch {
            _summitCheck.value = SummitCheckState.Done(summitChecker.check(peak))
        }
    }

    companion object {
        private const val TAG = "MapViewModel"

        val Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY])
                MapViewModel(PeakRepository(application.assets), SummitChecker(application))
            }
        }
    }
}
