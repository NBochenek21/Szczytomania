package pl.szczytomania.ui.screens.friends

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import pl.szczytomania.data.firestore.ConquestRepository
import pl.szczytomania.data.firestore.FriendRepository
import pl.szczytomania.data.firestore.Friendship
import pl.szczytomania.data.firestore.UserRepository
import pl.szczytomania.data.repository.ConqueredPeaks
import pl.szczytomania.data.repository.Friends
import pl.szczytomania.data.repository.PeakRepository
import pl.szczytomania.ui.screens.sets.SetsData
import pl.szczytomania.ui.screens.sets.loadSetsData
import java.util.Date
import kotlin.coroutines.cancellation.CancellationException

data class FriendProfileUiState(
    val loading: Boolean = true,
    val nick: String = "",
    /** Znajomość – null, gdy już jej nie ma (np. usunięta na drugim telefonie). */
    val friendship: Friendship? = null,
    val since: Date? = null,
    val comparison: FriendComparison? = null,
)

/** Profil znajomego (mockup 09): statystyki, wyścig w Koronie Gór Polski i jego zdobycze. */
class FriendProfileViewModel(
    friendUid: String,
    users: UserRepository,
    private val friendRepository: FriendRepository,
    conquests: ConquestRepository,
    friendships: Flow<List<Friendship>>,
    myPeakIds: Flow<Set<String>>,
    repository: PeakRepository,
) : ViewModel() {

    private val data: Flow<SetsData?> = flow {
        emit(
            try {
                repository.loadSetsData()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("FriendProfileViewModel", "Nie udało się wczytać szczytów", e)
                null
            },
        )
    }

    val state: StateFlow<FriendProfileUiState> = combine(
        users.profile(friendUid),
        friendships,
        conquests.friendConquests(friendUid),
        myPeakIds,
        data,
    ) { profile, list, friendConquests, mine, data ->
        val friendship = list.find { it.friendUid == friendUid }
        FriendProfileUiState(
            loading = false,
            nick = profile?.nick.orEmpty(),
            friendship = friendship,
            since = friendship?.since,
            comparison = data?.let { FriendComparison.of(friendConquests, mine, it.peaksById, it.sets) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FriendProfileUiState())

    fun remove() {
        state.value.friendship?.let(friendRepository::remove)
    }

    companion object {
        fun factory(friendUid: String) = viewModelFactory {
            initializer {
                FriendProfileViewModel(
                    friendUid = friendUid,
                    users = UserRepository(),
                    friendRepository = Friends.repository,
                    conquests = ConquestRepository(),
                    friendships = Friends.friendships,
                    myPeakIds = ConqueredPeaks.ids,
                    repository = PeakRepository.get(checkNotNull(this[APPLICATION_KEY]).assets),
                )
            }
        }
    }
}
