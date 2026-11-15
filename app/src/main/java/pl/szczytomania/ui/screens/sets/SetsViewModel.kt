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
import pl.szczytomania.data.firestore.OwnSetRepository
import pl.szczytomania.data.model.Peak
import pl.szczytomania.data.repository.ConqueredPeaks
import pl.szczytomania.data.repository.PeakRepository

data class SetsUiState(
    val isLoading: Boolean = true,
    val loadError: Boolean = false,
    /** Gotowe zestawy z postępem, w kolejności z pliku (Korona Gór Polski pierwsza). */
    val sets: List<SetProgress> = emptyList(),
    /** Własne zestawy użytkownika z Firestore, od najnowszych. */
    val own: List<SetProgress> = emptyList(),
    /** Wszystkie szczyty – do tworzenia własnego zestawu. */
    val allPeaks: List<Peak> = emptyList(),
) {
    fun set(id: String): SetProgress? = (sets + own).find { it.set.id == id }
}

/** Gotowe i własne zestawy gór z postępem – wspólny dla listy zestawów, szczegółów i nowego zestawu. */
class SetsViewModel(
    repository: PeakRepository,
    private val ownSets: OwnSetRepository,
    conqueredIds: Flow<Set<String>>,
) : ViewModel() {

    private val loaded = MutableStateFlow<SetsData?>(null)
    private val loadError = MutableStateFlow(false)

    val state: StateFlow<SetsUiState> = combine(loaded, loadError, conqueredIds, ownSets.ownSets) { data, error, conquered, own ->
        when {
            error -> SetsUiState(isLoading = false, loadError = true)
            data == null -> SetsUiState()
            else -> SetsUiState(
                isLoading = false,
                sets = data.sets.map { SetProgress.of(it, data.peaksById, conquered) },
                own = own.map { SetProgress.of(it, data.peaksById, conquered) },
                allPeaks = data.peaksById.values.toList(),
            )
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

    /** Zapisuje własny zestaw; szczyty od najwyższego, jak w gotowych zestawach. */
    fun createOwnSet(name: String, peakIds: Set<String>) {
        val peaksById = loaded.value?.peaksById ?: return
        ownSets.create(name.trim(), orderedForSet(peakIds, peaksById))
    }

    fun deleteOwnSet(setId: String) {
        if (OwnSetRepository.isOwn(setId)) ownSets.delete(OwnSetRepository.documentId(setId))
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                SetsViewModel(
                    PeakRepository.get(checkNotNull(this[APPLICATION_KEY]).assets),
                    OwnSetRepository(),
                    ConqueredPeaks.ids,
                )
            }
        }
    }
}

/** Id wybranych szczytów od najwyższego; nieznane id są pomijane. */
fun orderedForSet(peakIds: Set<String>, peaksById: Map<String, Peak>): List<String> =
    peakIds.mapNotNull { peaksById[it] }.sortedByDescending { it.elevation }.map { it.id }

/** Nazwa nowego zestawu jest poprawna: 1–40 znaków po obcięciu spacji (tak samo sprawdzają reguły Firestore). */
fun isValidSetName(name: String) = name.trim().length in 1..40

/** Podgląd szczytów na karcie własnego zestawu: pierwsze nazwy i ile jeszcze ("… i 6 kolejnych"). */
fun SetProgress.previewNames(shown: Int = 4): Pair<String, Int> =
    peaks.take(shown).joinToString(", ") { it.peak.name } to (total - shown).coerceAtLeast(0)
