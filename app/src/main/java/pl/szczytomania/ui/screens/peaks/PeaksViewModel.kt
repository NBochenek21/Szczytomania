package pl.szczytomania.ui.screens.peaks

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.szczytomania.data.model.Peak
import pl.szczytomania.data.repository.PeakRepository

data class PeaksUiState(
    val query: String = "",
    /** Szczyty pasujące do zapytania, od najwyższego. */
    val peaks: List<Peak> = emptyList(),
    val isLoading: Boolean = true,
    val loadError: Boolean = false,
)

class PeaksViewModel(private val peakRepository: PeakRepository) : ViewModel() {

    private val _state = MutableStateFlow(PeaksUiState())
    val state: StateFlow<PeaksUiState> = _state.asStateFlow()

    private var allPeaks: List<Peak> = emptyList()

    init {
        viewModelScope.launch {
            runCatching { peakRepository.getAll() }
                .onSuccess { peaks ->
                    allPeaks = peaks
                    _state.update { it.copy(peaks = PeakSearch.filter(peaks, it.query), isLoading = false) }
                }
                .onFailure { error ->
                    Log.e(TAG, "Nie udało się wczytać szczytów", error)
                    _state.update { it.copy(isLoading = false, loadError = true) }
                }
        }
    }

    // Lista ma ok. 200 pozycji, więc filtrujemy od razu przy każdym znaku – pole tekstowe
    // dostaje nową wartość synchronicznie i kursor nie "skacze".
    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query, peaks = PeakSearch.filter(allPeaks, query)) }
    }

    companion object {
        private const val TAG = "PeaksViewModel"

        val Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY])
                PeaksViewModel(PeakRepository(application.assets))
            }
        }
    }
}
