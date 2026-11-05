package pl.szczytomania.ui.screens.sets

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.szczytomania.data.repository.ConqueredPeaks
import pl.szczytomania.data.repository.PeakRepository

data class SetsUiState(
    val isLoading: Boolean = true,
    val loadError: Boolean = false,
    /** Gotowe zestawy z postępem, w kolejności z pliku (Korona Gór Polski pierwsza). */
    val sets: List<SetProgress> = emptyList(),
) {
    fun set(id: String): SetProgress? = sets.find { it.set.id == id }
}

/** Gotowe zestawy gór i postęp użytkownika – wspólny dla listy zestawów i ich szczegółów. */
class SetsViewModel(repository: PeakRepository, conqueredIds: Flow<Set<String>>) : ViewModel() {

    private val loaded = MutableStateFlow<SetsData?>(null)
    private val loadError = MutableStateFlow(false)

    val state: StateFlow<SetsUiState> = combine(loaded, loadError, conqueredIds) { data, error, conquered ->
        when {
            error -> SetsUiState(isLoading = false, loadError = true)
            data == null -> SetsUiState()
            else -> SetsUiState(isLoading = false, sets = data.sets.map { SetProgress.of(it, data.peaksById, conquered) })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SetsUiState())

    init {
        viewModelScope.launch {
            runCatching { repository.loadSetsData() }
                .onSuccess { loaded.value = it }
                .onFailure {
                    Log.e("SetsViewModel", "Nie udało się wczytać zestawów", it)
                    loadError.value = true
                }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                SetsViewModel(PeakRepository.get(checkNotNull(this[APPLICATION_KEY]).assets), ConqueredPeaks.ids)
            }
        }
    }
}
