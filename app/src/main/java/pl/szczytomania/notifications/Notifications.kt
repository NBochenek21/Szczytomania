package pl.szczytomania.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import pl.szczytomania.MainActivity
import pl.szczytomania.R
import pl.szczytomania.data.model.Peak
import pl.szczytomania.data.repository.PeakRepository

/** Gdzie przejść po dotknięciu powiadomienia: profil znajomego albo lista znajomych (zaproszenie). */
data class NotificationTarget(val friendUid: String?)

object Notifications {

    private const val CHANNEL_FRIENDS = "znajomi"
    private const val EXTRA_TARGET = "pl.szczytomania.extra.CEL"
    private const val EXTRA_FRIEND_UID = "pl.szczytomania.extra.ZNAJOMY"

    // Zielony z motywu jasnego – kolor ikonki w pasku powiadomień.
    private const val ACCENT = 0xFF2F5D3A.toInt()

    private val _target = MutableStateFlow<NotificationTarget?>(null)

    /** Cel z dotkniętego powiadomienia – AppNavigation przechodzi tam i czyści go przez [consumeTarget]. */
    val target: StateFlow<NotificationTarget?> = _target.asStateFlow()

    fun consumeTarget() {
        _target.value = null
    }

    /** Wywoływane przez MainActivity – przy starcie i gdy aplikacja była już otwarta. */
    fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_TARGET, false) != true) return
        _target.value = NotificationTarget(intent.getStringExtra(EXTRA_FRIEND_UID))
        // Po obrocie ekranu aktywność dostaje ten sam Intent – nie przechodzimy drugi raz.
        intent.removeExtra(EXTRA_TARGET)
    }

    /** Czy aplikacja może pokazać powiadomienie (zgoda w Androidzie 13+ i włączone w ustawieniach systemu). */
    fun canPost(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    /** Pokazuje powiadomienie dla wiadomości push. Szuka szczytu w pliku, więc nie wołać z głównego wątku. */
    fun show(context: Context, message: PushMessage) {
        if (!canPost(context) || !NotificationSettings.isEnabled(context)) return
        createChannel(context)

        val peak = (message as? PushMessage.FriendConquest)?.let { conquest ->
            runBlocking { runCatching { PeakRepository.get(context.assets).getAll() }.getOrNull() }
                ?.find { it.id == conquest.peakId }
        }
        val (title, text) = texts(context, message, peak)
        val notification = NotificationCompat.Builder(context, CHANNEL_FRIENDS)
            .setSmallIcon(R.drawable.ic_mountains)
            .setColor(ACCENT)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setAutoCancel(true)
            .setContentIntent(openApp(context, message))
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notificationId(message), notification)
        } catch (e: SecurityException) {
            // Zgodę cofnięto między sprawdzeniem a pokazaniem – nic nie pokazujemy.
        }
    }

    private fun texts(context: Context, message: PushMessage, peak: Peak?): Pair<String, String> {
        val nick = message.nick.ifBlank { context.getString(R.string.notification_someone) }
        return when (message) {
            is PushMessage.FriendConquest -> context.getString(R.string.notification_conquest_title, nick) to
                (peak?.let { context.getString(R.string.notification_conquest_text, it.name, it.elevation, it.range) }
                    ?: context.getString(R.string.notification_conquest_unknown_peak))
            is PushMessage.FriendInvite -> context.getString(R.string.notification_invite_title) to
                context.getString(R.string.notification_invite_text, nick)
            is PushMessage.FriendAccepted -> context.getString(R.string.notification_accepted_title) to
                context.getString(R.string.notification_accepted_text, nick)
        }
    }

    private fun openApp(context: Context, message: PushMessage): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            // Otwarta aplikacja dostaje Intent w onNewIntent zamiast drugiej kopii ekranu.
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_TARGET, true)
            // Zaproszenie – lista znajomych (tam są Akceptuj / Odrzuć), pozostałe – profil znajomego.
            .putExtra(EXTRA_FRIEND_UID, if (message is PushMessage.FriendInvite) null else message.friendUid)
        return PendingIntent.getActivity(
            context,
            notificationId(message),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    // Osobne powiadomienie na każdą zdobycz i osobę; to samo zdarzenie drugi raz zastępuje poprzednie.
    private fun notificationId(message: PushMessage) = when (message) {
        is PushMessage.FriendConquest -> "zdobycz/${message.friendUid}/${message.peakId}"
        is PushMessage.FriendInvite -> "zaproszenie/${message.friendUid}"
        is PushMessage.FriendAccepted -> "znajomy/${message.friendUid}"
    }.hashCode()

    private fun createChannel(context: Context) {
        NotificationManagerCompat.from(context).createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_FRIENDS, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.notification_channel_friends))
                .setDescription(context.getString(R.string.notification_channel_friends_description))
                .build(),
        )
    }
}
