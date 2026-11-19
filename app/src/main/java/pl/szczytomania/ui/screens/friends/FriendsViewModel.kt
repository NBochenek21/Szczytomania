package pl.szczytomania.ui.screens.friends

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import pl.szczytomania.data.firestore.ConquestRepository
import pl.szczytomania.data.firestore.FriendRepository
import pl.szczytomania.data.firestore.Friendship
import pl.szczytomania.data.firestore.UserHit
import pl.szczytomania.data.firestore.UserRepository
import pl.szczytomania.data.repository.Friends
import pl.szczytomania.data.repository.PeakRepository
import pl.szczytomania.ui.screens.sets.SetsData
import pl.szczytomania.ui.screens.sets.loadSetsData
import kotlin.coroutines.cancellation.CancellationException

/** Osoba na ekranie Znajomi: zaproszenie, znajomy albo wysłane zaproszenie. */
data class FriendItem(val friendship: Friendship, val nick: String, val summary: FriendSummary?)

data class FriendsUiState(
    val loading: Boolean = true,
    /** Zaproszenia do nas – czekają na Akceptuj / Odrzuć. */
    val invitations: List<FriendItem> = emptyList(),
    val friends: List<FriendItem> = emptyList(),
    /** Nasze zaproszenia, jeszcze nie przyjęte. */
    val sent: List<FriendItem> = emptyList(),
    val recent: List<RecentConquest> = emptyList(),
    val myNick: String = "",
)

/** Osoba w wyszukiwarce i jej relacja z nami (przycisk Zaproś / Akceptuj albo opis). */
data class SearchHit(val uid: String, val nick: String, val relation: Relation, val friendship: Friendship?)

data class SearchUiState(
    val query: String = "",
    val searching: Boolean = false,
    val hits: List<SearchHit> = emptyList(),
    /** Wyszukiwanie nie powiodło się (np. brak zasięgu i nic w pamięci telefonu). */
    val failed: Boolean = false,
)

/** Wynik zapytania do Firestore, zanim połączymy go z naszymi znajomościami. */
private data class RawSearch(val query: String, val searching: Boolean, val hits: List<UserHit>, val failed: Boolean)

/** Ekran Znajomi (mockupy 08 i 10): zaproszenia, ostatnie zdobycze znajomych, lista i wyszukiwarka. */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class FriendsViewModel(
    private val myUid: String?,
    private val users: UserRepository,
    private val friendRepository: FriendRepository,
    private val conquests: ConquestRepository,
    friendships: Flow<List<Friendship>>,
    repository: PeakRepository,
) : ViewModel() {

    private val data: Flow<SetsData?> = flow {
        emit(
            try {
                repository.loadSetsData()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Nie udało się wczytać szczytów", e)
                null
            },
        )
    }

    /** Każda znajomość z nickiem i zdobyczami drugiej osoby – na bieżąco. */
    private val entries: Flow<List<FriendEntry>> = friendships.flatMapLatest { list ->
        if (list.isEmpty()) {
            flowOf(emptyList())
        } else {
            combine(list.map(::entryOf)) { it.toList() }
        }
    }

    val state: StateFlow<FriendsUiState> = combine(
        entries,
        data,
        if (myUid == null) flowOf(null) else users.profile(myUid),
    ) { entries, data, me ->
        val peaks = data?.peaksById.orEmpty()
        val sets = data?.sets.orEmpty()
        val items = entries.map { e ->
            FriendItem(e.friendship, e.nick.orEmpty(), e.conquests?.let { FriendSummary.of(it, peaks, sets) })
        }
        FriendsUiState(
            loading = false,
            invitations = items.filter { !it.friendship.accepted && it.friendship.incoming },
            friends = items.filter { it.friendship.accepted }.sortedBy { it.nick.lowercase() },
            sent = items.filter { !it.friendship.accepted && !it.friendship.incoming },
            recent = recentConquests(entries, peaks),
            myNick = me?.nick.orEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FriendsUiState())

    private val query = MutableStateFlow("")

    private val rawSearch: Flow<RawSearch> = query
        .debounce(300)
        .mapLatest { q ->
            val trimmed = q.trim()
            if (trimmed.length < MIN_QUERY) return@mapLatest RawSearch(q, false, emptyList(), false)
            try {
                RawSearch(q, false, users.searchByNick(trimmed).filter { it.uid != myUid }, false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Nie udało się wyszukać \"$trimmed\"", e)
                RawSearch(q, false, emptyList(), true)
            }
        }

    /** Wyniki wyszukiwania. Status (np. "zaproszenie wysłane") zmienia się od razu po Zaproś. */
    val search: StateFlow<SearchUiState> = combine(query, rawSearch, friendships) { q, raw, list ->
        SearchUiState(
            query = q,
            // Wpisany tekst jeszcze nie wyszukany (czekamy chwilę, aż użytkownik skończy pisać).
            searching = q.trim().length >= MIN_QUERY && raw.query != q,
            hits = raw.hits.map { hit ->
                val (relation, friendship) = relationTo(hit.uid, list)
                SearchHit(hit.uid, hit.nick, relation, friendship)
            },
            failed = raw.failed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun setQuery(value: String) {
        query.value = value
    }

    fun invite(uid: String) = friendRepository.invite(uid)
    fun accept(friendship: Friendship) = friendRepository.accept(friendship)
    fun remove(friendship: Friendship) = friendRepository.remove(friendship)

    private fun entryOf(friendship: Friendship): Flow<FriendEntry> {
        // Zdobycze widać u znajomych i u osób, które nas zaprosiły (tak pozwalają reguły).
        val canSee = friendship.accepted || friendship.incoming
        return combine(
            users.profile(friendship.friendUid),
            if (canSee) conquests.friendConquests(friendship.friendUid) else flowOf(null),
        ) { profile, list -> FriendEntry(friendship, profile?.nick, list) }
    }

    companion object {
        private const val TAG = "FriendsViewModel"

        /** Od tylu znaków zaczynamy szukać – "k" pasowałoby do zbyt wielu osób. */
        const val MIN_QUERY = 2

        val Factory = viewModelFactory {
            initializer {
                FriendsViewModel(
                    myUid = FirebaseAuth.getInstance().currentUser?.uid,
                    users = UserRepository(),
                    friendRepository = Friends.repository,
                    conquests = ConquestRepository(),
                    friendships = Friends.friendships,
                    repository = PeakRepository.get(checkNotNull(this[APPLICATION_KEY]).assets),
                )
            }
        }
    }
}
