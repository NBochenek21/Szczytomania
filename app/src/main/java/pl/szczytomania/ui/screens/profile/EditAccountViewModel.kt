package pl.szczytomania.ui.screens.profile

import android.util.Patterns
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.szczytomania.R
import pl.szczytomania.data.auth.AuthRepository
import pl.szczytomania.data.firestore.NickTakenException
import pl.szczytomania.data.firestore.UserRepository

/** Część ekranu edycji konta – każda ma własny przycisk i komunikat. */
enum class AccountSection { NICK, EMAIL, PASSWORD }

/** Wynik ostatniej akcji: komunikat w danej części ekranu, sukces (zielony) albo błąd (czerwony). */
data class AccountMessage(val section: AccountSection, @param:StringRes val text: Int, val success: Boolean)

data class EditAccountUiState(
    val email: String = "",
    val busy: AccountSection? = null,
    val message: AccountMessage? = null,
)

/** Nick jak przy rejestracji i w regułach Firestore: 3–20 znaków. */
fun isValidNick(nick: String) = nick.trim().length in 3..20

/** Hasło jak przy rejestracji: co najmniej 8 znaków. */
fun isValidPassword(password: String) = password.length >= 8

/** Edycja konta (karta projektu: nick, e-mail, hasło). Zdjęcie profilowe dojdzie z regułami Storage. */
class EditAccountViewModel(
    private val uid: String?,
    private val auth: AuthRepository,
    private val users: UserRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EditAccountUiState(email = auth.email.orEmpty()))
    val state: StateFlow<EditAccountUiState> = _state.asStateFlow()

    /** Nick musi być wolny, więc zmiana wymaga internetu (patrz [UserRepository.changeNick]). */
    fun saveNick(nick: String) {
        val id = uid ?: return
        if (!isValidNick(nick)) return fail(AccountSection.NICK, R.string.auth_error_nick_length)
        if (!UserRepository.isValidNickFormat(nick)) return fail(AccountSection.NICK, R.string.auth_error_nick_chars)
        run(AccountSection.NICK, R.string.account_nick_saved) { users.changeNick(id, nick.trim()) }
    }

    fun changeEmail(newEmail: String, currentPassword: String) {
        if (!Patterns.EMAIL_ADDRESS.matcher(newEmail.trim()).matches()) return fail(AccountSection.EMAIL, R.string.auth_error_email_invalid)
        if (currentPassword.isEmpty()) return fail(AccountSection.EMAIL, R.string.account_current_password_empty)
        run(AccountSection.EMAIL, R.string.account_email_link_sent) { auth.changeEmail(currentPassword, newEmail.trim()) }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        if (currentPassword.isEmpty()) return fail(AccountSection.PASSWORD, R.string.account_current_password_empty)
        if (!isValidPassword(newPassword)) return fail(AccountSection.PASSWORD, R.string.auth_error_password_short)
        run(AccountSection.PASSWORD, R.string.account_password_changed) { auth.changePassword(currentPassword, newPassword) }
    }

    private fun fail(section: AccountSection, @StringRes text: Int) =
        _state.update { it.copy(message = AccountMessage(section, text, false)) }

    private fun run(section: AccountSection, @StringRes success: Int, action: suspend () -> Unit) {
        _state.update { it.copy(busy = section, message = null) }
        viewModelScope.launch {
            val message = try {
                action()
                AccountMessage(section, success, true)
            } catch (e: Exception) {
                AccountMessage(section, errorMessage(e), false)
            }
            _state.update { it.copy(busy = null, message = message) }
        }
    }

    @StringRes
    private fun errorMessage(e: Exception): Int = when (e) {
        is FirebaseNetworkException -> R.string.auth_error_network
        is NickTakenException -> R.string.auth_error_nick_taken
        is FirebaseFirestoreException ->
            if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) R.string.account_nick_offline else R.string.auth_error_unknown
        is FirebaseTooManyRequestsException -> R.string.auth_error_too_many_requests
        is FirebaseAuthUserCollisionException -> R.string.auth_error_email_taken
        is FirebaseAuthWeakPasswordException -> R.string.auth_error_password_short
        // Przy potwierdzaniu obecnym hasłem ten błąd oznacza złe hasło.
        is FirebaseAuthInvalidCredentialsException -> R.string.account_wrong_password
        else -> R.string.auth_error_unknown
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { EditAccountViewModel(FirebaseAuth.getInstance().currentUser?.uid, AuthRepository(), UserRepository()) }
        }
    }
}
