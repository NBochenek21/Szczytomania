package pl.szczytomania.notifications

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Przełącznik "Powiadomienia" w profilu. Włączony = ustawienie w aplikacji i zgoda Androida.
 * Włączenie prosi o zgodę (Android 13+), a po trwałej odmowie otwiera ustawienia powiadomień aplikacji.
 */
@Stable
class NotificationsSwitchState internal constructor(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    var checked by mutableStateOf(isOn())
        private set

    internal var requestPermission: () -> Unit = {}
    private var deniedOnce = false

    fun toggle() {
        if (checked) {
            NotificationSettings.setEnabled(context, false)
            scope.launch { PushTokens.unregister() }
        } else {
            NotificationSettings.setEnabled(context, true)
            scope.launch { PushTokens.register(context) }
            when {
                Notifications.canPost(context) -> Unit
                // Po odmowie system zwykle nie pokaże już okna – zostają ustawienia powiadomień aplikacji.
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || deniedOnce -> openSettings()
                else -> requestPermission()
            }
        }
        refresh()
    }

    internal fun onPermissionResult(granted: Boolean) {
        if (!granted) deniedOnce = true
        refresh()
    }

    internal fun refresh() {
        checked = isOn()
    }

    private fun isOn() = NotificationSettings.isEnabled(context) && Notifications.canPost(context)

    private fun openSettings() {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
        )
    }
}

@Composable
fun rememberNotificationsSwitch(): NotificationsSwitchState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember(context) { NotificationsSwitchState(context, scope) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), state::onPermissionResult)
    state.requestPermission = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }

    // Zgodę można zmienić w ustawieniach telefonu – sprawdzamy po powrocie do aplikacji.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, state) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) state.refresh() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return state
}

/**
 * Pyta o zgodę na powiadomienia raz, przy pierwszym wejściu na ekran Znajomi – powiadomienia dotyczą
 * znajomych, więc tam pytanie jest zrozumiałe. Później zgodę włącza się przełącznikiem w profilu.
 */
@Composable
fun AskForNotificationsOnce() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_ASKED, false) || Notifications.canPost(context) || !NotificationSettings.isEnabled(context)) return@LaunchedEffect
        prefs.edit { putBoolean(KEY_ASKED, true) }
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

private const val PREFS = "ustawienia"
private const val KEY_ASKED = "powiadomienia_zapytano"
