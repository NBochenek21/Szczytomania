package pl.szczytomania.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Tylko w buildzie debug: udaje wiadomość push, żeby sprawdzić powiadomienia bez Cloud Functions.
 *
 * adb shell am broadcast -n pl.szczytomania/.notifications.DebugPushReceiver \
 *   --es type friend_conquest --es uid abc --es nick kuba --es peakId node/4366169405
 */
class DebugPushReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val data = listOf("type", "uid", "nick", "peakId").mapNotNull { key -> intent.getStringExtra(key)?.let { key to it } }.toMap()
        val message = PushMessage.parse(data) ?: return
        // Jak w FirebaseMessagingService – pokazanie czyta plik szczytów, więc poza głównym wątkiem.
        val pending = goAsync()
        Thread {
            Notifications.show(context.applicationContext, message)
            pending.finish()
        }.start()
    }
}
