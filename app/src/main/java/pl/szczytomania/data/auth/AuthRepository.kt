package pl.szczytomania.data.auth

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Konta użytkowników w Firebase Authentication (e-mail i hasło). */
class AuthRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {

    /** Zalogowany użytkownik albo null. Firebase pamięta sesję, więc po restarcie aplikacji nie trzeba logować się ponownie. */
    val currentUser: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    /** Zakłada konto i od razu loguje. Zwraca nowego użytkownika (jego uid to klucz profilu w Firestore). */
    suspend fun register(email: String, password: String): FirebaseUser =
        checkNotNull(auth.createUserWithEmailAndPassword(email, password).await().user)

    /** Wysyła mail z linkiem do zmiany hasła (szablon po polsku ustawiony w konsoli Firebase). */
    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    fun signOut() = auth.signOut()

    /** Adres e-mail zalogowanego użytkownika. */
    val email: String? get() = auth.currentUser?.email

    /**
     * Zmiana adresu e-mail. Firebase wymaga potwierdzenia obecnym hasłem, a nowy adres zaczyna działać
     * dopiero po kliknięciu linku, który Firebase wysyła na ten adres.
     */
    suspend fun changeEmail(currentPassword: String, newEmail: String) {
        val user = reauthenticate(currentPassword)
        user.verifyBeforeUpdateEmail(newEmail).await()
    }

    /** Zmiana hasła – po potwierdzeniu obecnym hasłem. */
    suspend fun changePassword(currentPassword: String, newPassword: String) {
        reauthenticate(currentPassword).updatePassword(newPassword).await()
    }

    private suspend fun reauthenticate(password: String): FirebaseUser {
        val user = checkNotNull(auth.currentUser) { "Brak zalogowanego użytkownika" }
        user.reauthenticate(EmailAuthProvider.getCredential(checkNotNull(user.email), password)).await()
        return user
    }
}
