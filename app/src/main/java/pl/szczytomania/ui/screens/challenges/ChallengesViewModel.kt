package pl.szczytomania.ui.screens.challenges

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import pl.szczytomania.data.firestore.Challenge
import pl.szczytomania.data.firestore.ChallengeMode
import pl.szczytomania.data.firestore.ChallengeRepository
import pl.szczytomania.data.firestore.UserRepository
import pl.szczytomania.data.repository.Challenges
import pl.szczytomania.ui.screens.sets.SetProgress
import java.util.Calendar
import java.util.Date

/** Wyzwanie gotowe do pokazania: ranking z nickami i stan wyścigu. */
data class ChallengeView(val challenge: Challenge, val ranking: List<RankRow>, val finished: Boolean, val mine: Boolean) {
    val total get() = challenge.peakIds.size
    val me get() = ranking.firstOrNull { it.me }

    /** Zwycięzca (pierwsze miejsce) – tylko po zakończeniu wyścigu. */
    val winner get() = if (finished) ranking.firstOrNull() else null
}

data class ChallengesUiState(val isLoading: Boolean = true, val challenges: List<ChallengeView> = emptyList()) {
    fun challenge(id: String) = challenges.firstOrNull { it.challenge.id == id }
}

/** Czas trwania wyścigu "kto więcej do daty" – do wyboru przy zakładaniu. */
enum class ChallengeLength(val days: Int) { WEEK(7), TWO_WEEKS(14), MONTH(30) }

/** Lista wyzwań, szczegóły z rankingiem i zakładanie nowego wyzwania. */
class ChallengesViewModel(
    private val repository: ChallengeRepository,
    challenges: Flow<List<Challenge>>,
    private val myUid: String?,
    users: UserRepository,
) : ViewModel() {

    // Nicki wszystkich uczestników – nie każdy musi być naszym znajomym, więc pytamy o profile (widzi je każdy zalogowany).
    @OptIn(ExperimentalCoroutinesApi::class)
    private val nicks: Flow<Map<String, String>> = challenges
        .map { list -> list.flatMap { it.participants }.toSet() }
        .distinctUntilChanged()
        .flatMapLatest { uids ->
            if (uids.isEmpty()) {
                flowOf(emptyMap())
            } else {
                combine(uids.map { uid -> users.profile(uid).map { uid to it?.nick } }) { pairs ->
                    pairs.mapNotNull { (uid, nick) -> nick?.let { uid to it } }.toMap()
                }
            }
        }

    val state: StateFlow<ChallengesUiState> = combine(challenges, nicks) { list, nickMap ->
        ChallengesUiState(
            isLoading = false,
            challenges = list.map { c ->
                ChallengeView(c, ranking(c, myUid, nickMap), isFinished(c), mine = c.ownerId == myUid)
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChallengesUiState())

    /** Zakłada wyzwanie na wybrany zestaw z wybranymi znajomymi. */
    fun create(set: SetProgress, mode: ChallengeMode, length: ChallengeLength, friends: List<String>) {
        val endsAt = if (mode == ChallengeMode.MOST) endDate(Date(), length) else null
        repository.create(set.set.name, set.peaks.map { it.peak.id }, mode, endsAt, friends.take(ChallengeRepository.MAX_PARTICIPANTS - 1))
    }

    fun delete(challengeId: String) = repository.delete(challengeId)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                ChallengesViewModel(Challenges.repository, Challenges.all, FirebaseAuth.getInstance().currentUser?.uid, UserRepository())
            }
        }
    }
}

/** Koniec wyścigu: o północy po upływie wybranej liczby dni – cały ostatni dzień się liczy. */
fun endDate(start: Date, length: ChallengeLength): Date = Calendar.getInstance().apply {
    time = start
    add(Calendar.DAY_OF_YEAR, length.days + 1)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.time
