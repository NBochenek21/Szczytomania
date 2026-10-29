package pl.szczytomania.ui.screens.auth

import android.util.Log
import android.util.Patterns
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.szczytomania.R
import pl.szczytomania.data.auth.AuthRepository
import pl.szczytomania.data.firestore.UserRepository

enum class AuthMode { LOGIN, REGISTER }

data class AuthUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    val nick: String = "",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    @param:StringRes val error: Int? = null,
    /** Potwierdzenie wysłania maila z resetem hasła. */
    val resetEmailSent: Boolean = false,
)

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun setMode(mode: AuthMode) = _state.update { it.copy(mode = mode, error = null, resetEmailSent = false) }
    fun setNick(value: String) = _state.update { it.copy(nick = value, error = null) }
    fun setEmail(value: String) = _state.update { it.copy(email = value, error = null) }
    fun setPassword(value: String) = _state.update { it.copy(password = value, error = null) }

    /** Logowanie albo rejestracja – zależnie od wybranej zakładki. Po sukcesie MainActivity sama pokazuje mapę. */
    fun submit() {
        val s = _state.value
        val email = s.email.trim()
        val nick = s.nick.trim()

        val validationError = when {
            s.mode == AuthMode.REGISTER && nick.length !in NICK_LENGTH -> R.string.auth_error_nick_length
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> R.string.auth_error_email_invalid
            s.mode == AuthMode.REGISTER && s.password.length < MIN_PASSWORD_LENGTH -> R.string.auth_error_password_short
            s.password.isEmpty() -> R.string.auth_error_password_empty
            else -> null
        }
        if (validationError != null) {
            _state.update { it.copy(error = validationError) }
            return
        }

        run(s.mode) {
            when (s.mode) {
                AuthMode.LOGIN -> authRepository.signIn(email, s.password)
                AuthMode.REGISTER -> {
                    val user = authRepository.register(email, s.password)
                    userRepository.createProfile(user.uid, nick)
                }
            }
        }
    }

    fun sendPasswordReset() {
        val email = _state.value.email.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _state.update { it.copy(error = R.string.auth_error_reset_needs_email) }
            return
        }
        run(AuthMode.LOGIN) {
            authRepository.sendPasswordReset(email)
            _state.update { it.copy(resetEmailSent = true) }
        }
    }

    private fun run(mode: AuthMode, action: suspend () -> Unit) {
        _state.update { it.copy(isLoading = true, error = null, resetEmailSent = false) }
        viewModelScope.launch {
            runCatching { action() }
                .onFailure { e ->
                    Log.w(TAG, "Błąd Firebase Authentication", e)
                    _state.update { it.copy(error = errorMessage(e, mode)) }
                }
            _state.update { it.copy(isLoading = false) }
        }
    }

    @StringRes
    private fun errorMessage(e: Throwable, mode: AuthMode): Int = when (e) {
        is FirebaseNetworkException -> R.string.auth_error_network
        is FirebaseTooManyRequestsException -> R.string.auth_error_too_many_requests
        is FirebaseAuthUserCollisionException -> R.string.auth_error_email_taken
        // Musi być przed InvalidCredentials, bo dziedziczy po nim.
        is FirebaseAuthWeakPasswordException -> R.string.auth_error_password_short
        is FirebaseAuthInvalidUserException ->
            if (e.errorCode == "ERROR_USER_DISABLED") R.string.auth_error_user_disabled
            else R.string.auth_error_wrong_credentials
        is FirebaseAuthInvalidCredentialsException ->
            if (mode == AuthMode.LOGIN) R.string.auth_error_wrong_credentials
            else R.string.auth_error_email_invalid
        else -> R.string.auth_error_unknown
    }

    companion object {
        private const val TAG = "AuthViewModel"

        /** Wymóg z ekranu logowania ("co najmniej 8 znaków"); Firebase sam wymaga tylko 6. */
        const val MIN_PASSWORD_LENGTH = 8
        val NICK_LENGTH = 3..20

        val Factory = viewModelFactory {
            initializer { AuthViewModel(AuthRepository(), UserRepository()) }
        }
    }
}
