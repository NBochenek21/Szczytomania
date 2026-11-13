package pl.szczytomania.ui.screens.conquest

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageCapture
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
import pl.szczytomania.camera.ConquestPhotos
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
import kotlin.coroutines.cancellation.CancellationException

data class ConquestUiState(
    val peak: Peak? = null,
    val check: SummitCheckState = SummitCheckState.Idle,
    /** Zmniejszone zdjęcie ze szczytu do podglądu; null – jeszcze nie zrobione. */
    val photo: Bitmap? = null,
    /** Trwa zmniejszanie zdjęcia z aparatu. */
    val processingPhoto: Boolean = false,
    /** Nie udało się odczytać zdjęcia z aparatu – trzeba zrobić je jeszcze raz. */
    val photoFailed: Boolean = false,
    /** Po zapisie – ekran "Szczyt zdobyty!". */
    val summary: ConquestSummary? = null,
) {
    /** Wszystkie warunki spełnione – można zrobić zdjęcie i zapisać zdobycz. */
    val passed: Boolean
        get() = ((check as? SummitCheckState.Done)?.result as? SummitCheckResult.Checked)?.check?.passed == true
}

/** Zdobywanie szczytu: sprawdzenie GPS, zdjęcie, zapis zdobyczy z wysłaniem zdjęcia w tle i podsumowanie. */
class ConquestViewModel(
    private val repository: PeakRepository,
    private val checker: SummitChecker,
    private val conquests: ConquestRepository,
    private val photos: ConquestPhotos,
) : ViewModel() {

    private val _state = MutableStateFlow(ConquestUiState())
    val state: StateFlow<ConquestUiState> = _state.asStateFlow()

    // Zdobyte przed zapisem – do numeru szczytu i rekordu wysokości.
    private val conqueredIds = ConqueredPeaks.ids.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    private var data: SetsData? = null
    private var checkJob: Job? = null
    private var photoJob: Job? = null

    /** Otwiera zdobywanie szczytu i od razu sprawdza GPS. */
    fun start(peakId: String) {
        checkJob?.cancel()
        photoJob?.cancel()
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

    /** Migawka: zdjęcie z podglądu, zmniejszone, trafia do podglądu "Czy zdjęcie jest w porządku?". */
    fun takePhoto(imageCapture: ImageCapture) {
        val peak = _state.value.peak ?: return
        photoJob?.cancel()
        _state.update { it.copy(processingPhoto = true, photoFailed = false) }
        photoJob = viewModelScope.launch {
            val photo = try {
                photos.capture(imageCapture, peak.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Nie udało się zrobić zdjęcia", e)
                null
            }
            _state.update { it.copy(photo = photo, processingPhoto = false, photoFailed = photo == null) }
        }
    }

    /** "Powtórz" – wracamy do podglądu z aparatu. */
    fun retakePhoto() {
        photoJob?.cancel()
        _state.update { it.copy(photo = null, processingPhoto = false, photoFailed = false) }
    }

    /** Zapisuje zdobycz w telefonie (Firestore wyśle ją sam) i zleca wysłanie zdjęcia w tle. */
    fun save() {
        val current = _state.value
        val peak = current.peak ?: return
        val result = (current.check as? SummitCheckState.Done)?.result as? SummitCheckResult.Checked ?: return
        val loaded = data ?: return
        if (!result.check.passed || current.photo == null) return
        conquests.save(peak, result.check.reading)
        photos.upload(peak.id)
        _state.update { it.copy(summary = ConquestSummary.of(peak, loaded, conqueredIds.value, Date())) }
    }

    companion object {
        private const val TAG = "ConquestViewModel"

        val Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY])
                ConquestViewModel(
                    PeakRepository.get(application.assets),
                    SummitChecker(application),
                    ConqueredPeaks.repository,
                    ConquestPhotos(application),
                )
            }
        }
    }
}
