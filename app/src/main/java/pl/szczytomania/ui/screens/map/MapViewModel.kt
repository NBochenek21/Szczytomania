package pl.szczytomania.ui.screens.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.szczytomania.data.model.Peak
import pl.szczytomania.data.repository.PeakRepository

class MapViewModel(private val peakRepository: PeakRepository) : ViewModel() {

    private val _peaks = MutableStateFlow<List<Peak>>(emptyList())
    val peaks: StateFlow<List<Peak>> = _peaks.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { peakRepository.getAll() }
                .onSuccess { _peaks.value = it }
                // Uszkodzony plik szczytów nie powinien wyłączać mapy – pokazujemy ją bez szczytów.
                .onFailure { Log.e(TAG, "Nie udało się wczytać szczytów", it) }
        }
    }

    companion object {
        private const val TAG = "MapViewModel"

        val Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY])
                MapViewModel(PeakRepository(application.assets))
            }
        }
    }
}
