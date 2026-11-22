package pl.szczytomania.ui.screens.sets

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.szczytomania.data.firestore.Friendship
import pl.szczytomania.data.firestore.OwnSetRepository
import pl.szczytomania.data.firestore.UserRepository
import pl.szczytomania.data.repository.Friends
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
    /** Zestawy udostępnione użytkownikowi przez znajomych. */
    val sharedWithMe: List<SetProgress> = emptyList(),
    /** Właściciel i lista udostępnienia każdego własnego i udostępnionego zestawu (po id zestawu). */
    val sharing: Map<String, SetSharing> = emptyMap(),
    /** Zaakceptowani znajomi (uid → nick), od A do Z – do udostępniania. */
    val friends: List<Pair<String, String>> = emptyList(),
    /** Wszystkie szczyty – do tworzenia własnego zestawu. */
    val allPeaks: List<Peak> = emptyList(),
) {
    fun set(id: String): SetProgress? = (sets + own + sharedWithMe).find { it.set.id == id }

    /** Nick znajomego albo null, gdy to nie znajomy (np. usunięty ze znajomych). */
    fun nick(uid: String): String? = friends.firstOrNull { it.first == uid }?.second
}

/** Kto jest właścicielem zestawu i komu go udostępnił. [mine] – zestaw zalogowanego użytkownika. */
data class SetSharing(val ownerId: String, val sharedWith: List<String>, val mine: Boolean)

/** Gotowe i własne zestawy gór z postępem – wspólny dla listy zestawów, szczegółów i nowego zestawu. */
class SetsViewModel(
    repository: PeakRepository,
    private val ownSets: OwnSetRepository,
    conqueredIds: Flow<Set<String>>,
    /** Zaakceptowani znajomi (uid → nick), od A do Z. */
    friendNicks: Flow<List<Pair<String, String>>>,
) : ViewModel() {

    private val loaded = MutableStateFlow<SetsData?>(null)
    private val loadError = MutableStateFlow(false)

    // Własne i udostępnione zestawy razem – jeden obiekt, żeby combine niżej miał mniej argumentów.
    private val firestoreSets = combine(ownSets.ownSets, ownSets.sharedWithMe) { own, shared -> own to shared }

    val state: StateFlow<SetsUiState> = combine(loaded, loadError, conqueredIds, firestoreSets, friendNicks) {
            data, error, conquered, (own, shared), friends ->
        when {
            error -> SetsUiState(isLoading = false, loadError = true)
            data == null -> SetsUiState()
            else -> SetsUiState(
                isLoading = false,
                sets = data.sets.map { SetProgress.of(it, data.peaksById, conquered) },
                own = own.map { SetProgress.of(it.set, data.peaksById, conquered) },
                sharedWithMe = shared.map { SetProgress.of(it.set, data.peaksById, conquered) },
                sharing = (own.map { it to true } + shared.map { it to false })
                    .associate { (s, mine) -> s.set.id to SetSharing(s.ownerId, s.sharedWith, mine) },
                friends = friends,
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
    fun createOwnSet(name: String, peakIds: Set<String>, sharedWith: List<String> = emptyList()) {
        val peaksById = loaded.value?.peaksById ?: return
        ownSets.create(name.trim(), orderedForSet(peakIds, peaksById), sharedWith)
    }

    fun deleteOwnSet(setId: String) {
        if (OwnSetRepository.isOwn(setId)) ownSets.delete(setId)
    }

    /** Udostępnia własny zestaw wybranym znajomym (pusta lista – przestaje udostępniać). */
    fun shareOwnSet(setId: String, friendUids: List<String>) {
        if (OwnSetRepository.isOwn(setId)) ownSets.setSharedWith(setId, friendUids.distinct())
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                SetsViewModel(
                    PeakRepository.get(checkNotNull(this[APPLICATION_KEY]).assets),
                    OwnSetRepository(),
                    ConqueredPeaks.ids,
                    acceptedFriendNicks(Friends.friendships, UserRepository()),
                )
            }
        }
    }
}

/** Zaakceptowani znajomi z nickami (uid → nick), od A do Z – na bieżąco. */
@OptIn(ExperimentalCoroutinesApi::class)
fun acceptedFriendNicks(friendships: Flow<List<Friendship>>, users: UserRepository): Flow<List<Pair<String, String>>> =
    friendships.flatMapLatest { list ->
        val accepted = list.filter { it.accepted }
        if (accepted.isEmpty()) {
            flowOf(emptyList())
        } else {
            combine(accepted.map { f -> users.profile(f.friendUid).map { f.friendUid to it?.nick.orEmpty() } }) { pairs ->
                pairs.filter { it.second.isNotBlank() }.sortedBy { it.second.lowercase() }
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
