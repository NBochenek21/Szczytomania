package pl.szczytomania.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/** Odbiera wiadomości FCM – także gdy aplikacja jest zamknięta (wiadomości z samymi danymi, wysoki priorytet). */
class SzczytomaniaMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) = PushTokens.onNewToken(applicationContext, token)

    // Wywoływane w wątku w tle – Notifications.show może czytać plik szczytów.
    override fun onMessageReceived(message: RemoteMessage) {
        val push = PushMessage.parse(message.data)
        if (push == null) {
            Log.w(TAG, "Nieznana wiadomość: ${message.data["type"]}")
            return
        }
        Notifications.show(applicationContext, push)
    }

    private companion object {
        const val TAG = "MessagingService"
    }
}
