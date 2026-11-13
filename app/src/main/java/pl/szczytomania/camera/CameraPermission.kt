package pl.szczytomania.camera

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Uprawnienie do aparatu – tak samo jak lokalizacja (location/LocationPermission.kt). */
@Stable
class CameraPermissionState internal constructor(private val activity: Activity) {

    var granted by mutableStateOf(activity.hasCameraPermission())
        private set

    /** System nie pokaże już okna z pytaniem – zostają tylko ustawienia aplikacji. */
    var permanentlyDenied by mutableStateOf(false)
        private set

    internal lateinit var launcher: ManagedActivityResultLauncher<String, Boolean>

    private val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var rationaleBeforeRequest = false

    /** Pyta o aparat albo, jeśli to już niemożliwe, otwiera ustawienia aplikacji. */
    fun request() {
        if (permanentlyDenied) {
            activity.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.fromParts("package", activity.packageName, null)),
            )
            return
        }
        rationaleBeforeRequest = shouldShowRationale()
        launcher.launch(Manifest.permission.CAMERA)
    }

    internal fun onResult() {
        refresh()
        if (granted) return
        // Jak przy lokalizacji: "bez wyjaśnienia" system zwraca też po zwykłym zamknięciu okna,
        // więc za trwałą odmowę uznajemy to tylko, jeśli użytkownik już wcześniej odmówił.
        val rationaleAfter = shouldShowRationale()
        val deniedBefore = rationaleBeforeRequest || prefs.getBoolean(KEY_DENIED_BEFORE, false)
        if (rationaleAfter) prefs.edit { putBoolean(KEY_DENIED_BEFORE, true) }
        permanentlyDenied = !rationaleAfter && deniedBefore
    }

    internal fun refresh() {
        granted = activity.hasCameraPermission()
        if (granted) permanentlyDenied = false
    }

    private fun shouldShowRationale() = activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)

    private companion object {
        const val PREFS_NAME = "camera_permission"
        const val KEY_DENIED_BEFORE = "denied_before"
    }
}

@Composable
fun rememberCameraPermissionState(): CameraPermissionState {
    val activity = LocalContext.current.findActivity()
    val state = remember(activity) { CameraPermissionState(activity) }
    state.launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { state.onResult() }

    // Użytkownik mógł zezwolić w ustawieniach – sprawdzamy po powrocie do aplikacji.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, state) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) state.refresh() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return state
}

private fun Context.hasCameraPermission() =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private tailrec fun Context.findActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("Brak aktywności w kontekście")
}
