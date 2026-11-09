package pl.szczytomania.ui.screens.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.maplibre.android.camera.CameraPosition
import pl.szczytomania.data.model.Peak
import pl.szczytomania.data.repository.ConqueredPeaks
import pl.szczytomania.data.repository.PeakRepository

/** Mapa: szczyty, wybrany szczyt (karta) i zdobyte szczyty. Zdobywanie jest w ConquestViewModel. */
class MapViewModel(private val peakRepository: PeakRepository) : ViewModel() {

    private val _peaks = MutableStateFlow<List<Peak>>(emptyList())
    val peaks: StateFlow<List<Peak>> = _peaks.asStateFlow()

    private val _selectedPeak = MutableStateFlow<Peak?>(null)
    val selectedPeak: StateFlow<Peak?> = _selectedPeak.asStateFlow()

    /** Zdobyte szczyty – status na karcie szczytu. */
    val conqueredIds: StateFlow<Set<String>> =
        ConqueredPeaks.ids.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /**
     * Ostatnie położenie kamery. Mapa jest tworzona od nowa przy każdym powrocie z innego
     * ekranu menu – dzięki temu wraca tam, gdzie użytkownik ją zostawił.
     */
    var lastCamera: CameraPosition? = null

    init {
        viewModelScope.launch {
            runCatching { peakRepository.getAll() }
                .onSuccess { _peaks.value = it }
                // Uszkodzony plik szczytów nie powinien wyłączać mapy – pokazujemy ją bez szczytów.
                .onFailure { Log.e(TAG, "Nie udało się wczytać szczytów", it) }
        }
    }

    fun selectPeak(id: String?) {
        _selectedPeak.value = _peaks.value.find { it.id == id }
    }

    companion object {
        private const val TAG = "MapViewModel"

        val Factory = viewModelFactory {
            initializer {
                MapViewModel(PeakRepository.get(checkNotNull(this[APPLICATION_KEY]).assets))
            }
        }
    }
}
