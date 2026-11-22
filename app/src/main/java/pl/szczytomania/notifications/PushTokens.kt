package pl.szczytomania.notifications

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import pl.szczytomania.data.firestore.UserRepository

/** Ustawienie "Powiadomienia" z profilu – zapamiętane w telefonie, domyślnie włączone. */
object NotificationSettings {
    private const val PREFS = "ustawienia"
    private const val KEY = "powiadomienia"

    fun isEnabled(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, true)

    fun setEnabled(context: Context, enabled: Boolean) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putBoolean(KEY, enabled) }
}

/**
 * Token FCM tego telefonu w `users/{uid}/devices/{token}` – na te tokeny Cloud Function wysyła
 * powiadomienia. Zapisy nie czekają na serwer, więc działają też bez zasięgu.
 */
object PushTokens {
    private const val TAG = "PushTokens"
    private const val DEVICES = "devices"

    // Przy wylogowaniu nie czekamy dłużej – bez zasięgu token i tak przestanie działać (deleteToken).
    private const val SIGN_OUT_TIMEOUT_MS = 3_000L

    /** Zapisuje token zalogowanego użytkownika, jeśli powiadomienia są włączone. */
    suspend fun register(context: Context) {
        if (!NotificationSettings.isEnabled(context)) return
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }
            .onFailure { Log.w(TAG, "Brak tokenu FCM (np. bez internetu) – spróbujemy przy następnym starcie", it) }
            .getOrNull() ?: return
        save(token)
    }

    /** Nowy token od FCM (pierwsze uruchomienie, reinstalacja) – z SzczytomaniaMessagingService. */
    fun onNewToken(context: Context, token: String) {
        if (NotificationSettings.isEnabled(context)) save(token)
    }

    /** Wyłączenie powiadomień: usuwa token tego telefonu u zalogowanego użytkownika. */
    suspend fun unregister() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull() ?: return
        withTimeoutOrNull(SIGN_OUT_TIMEOUT_MS) {
            runCatching { device(uid, token).delete().await() }
                .onFailure { Log.w(TAG, "Nie udało się usunąć tokenu", it) }
        }
    }

    /**
     * Przed wylogowaniem: usuwa token z Firestore i unieważnia go w FCM. Nawet jeśli usunięcie
     * nie dojdzie do serwera (brak zasięgu), funkcja dostanie przy wysyłce błąd i sama usunie token.
     */
    suspend fun signOut() {
        unregister()
        runCatching { FirebaseMessaging.getInstance().deleteToken().await() }
            .onFailure { Log.w(TAG, "Nie udało się unieważnić tokenu", it) }
    }

    private fun save(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        device(uid, token).set(mapOf("updatedAt" to FieldValue.serverTimestamp()))
            .addOnFailureListener { Log.e(TAG, "Nie udało się zapisać tokenu", it) }
    }

    private fun device(uid: String, token: String) =
        FirebaseFirestore.getInstance().collection(UserRepository.USERS).document(uid).collection(DEVICES).document(token)
}
