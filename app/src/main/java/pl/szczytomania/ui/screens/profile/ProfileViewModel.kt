package pl.szczytomania.ui.screens.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.szczytomania.data.auth.AuthRepository
import pl.szczytomania.data.firestore.UserRepository
import pl.szczytomania.data.repository.ConqueredPeaks
import pl.szczytomania.data.repository.PeakRepository
import pl.szczytomania.ui.screens.sets.SetsData
import pl.szczytomania.ui.screens.sets.loadSetsData
import java.util.Date

data class ProfileUiState(
    val nick: String = "",
    /** Data założenia konta; null, dopóki profil się nie wczyta. */
    val memberSince: Date? = null,
    /** null, dopóki nie wczytają się szczyty i zestawy. */
    val stats: ProfileStats? = null,
) {
    val initial: String get() = avatarInitial(nick)
}

/** Profil zalogowanego użytkownika: nick z Firestore, statystyki ze zdobyczy, wylogowanie. */
class ProfileViewModel(
    uid: String?,
    email: String?,
    private val authRepository: AuthRepository,
    userRepository: UserRepository,
    repository: PeakRepository,
    conqueredIds: Flow<Set<String>>,
) : ViewModel() {

    private val loaded = MutableStateFlow<SetsData?>(null)

    // Zanim profil dotrze z Firestore (albo gdy go nie ma), pokazujemy początek adresu e-mail.
    private val fallbackNick = email?.substringBefore('@').orEmpty()

    val state: StateFlow<ProfileUiState> = combine(
        if (uid == null) flowOf(null) else userRepository.profile(uid),
        loaded,
        conqueredIds,
    ) { profile, data, conquered ->
        ProfileUiState(
            nick = profile?.nick?.takeIf { it.isNotBlank() } ?: fallbackNick,
            memberSince = profile?.createdAt,
            stats = data?.let { ProfileStats.of(it.peaksById, it.sets, conquered) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState(nick = fallbackNick))

    init {
        viewModelScope.launch {
            runCatching { repository.loadSetsData() }
                .onSuccess { loaded.value = it }
                .onFailure { Log.e("ProfileViewModel", "Nie udało się wczytać szczytów i zestawów", it) }
        }
    }

    /** Po wylogowaniu MainActivity sama pokazuje ekran logowania. */
    fun signOut() = authRepository.signOut()

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val user = FirebaseAuth.getInstance().currentUser
                ProfileViewModel(
                    uid = user?.uid,
                    email = user?.email,
                    authRepository = AuthRepository(),
                    userRepository = UserRepository(),
                    repository = PeakRepository.get(checkNotNull(this[APPLICATION_KEY]).assets),
                    conqueredIds = ConqueredPeaks.ids,
                )
            }
        }
    }
}
