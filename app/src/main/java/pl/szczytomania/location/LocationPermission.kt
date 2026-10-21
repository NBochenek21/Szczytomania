package pl.szczytomania.location

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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Jaki dostęp do lokalizacji dał użytkownik. Do zdobycia szczytu potrzebny jest [PRECISE]. */
enum class LocationAccess { PRECISE, APPROXIMATE, NONE }

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

fun Context.locationAccess(): LocationAccess = when {
    isGranted(Manifest.permission.ACCESS_FINE_LOCATION) -> LocationAccess.PRECISE
    isGranted(Manifest.permission.ACCESS_COARSE_LOCATION) -> LocationAccess.APPROXIMATE
    else -> LocationAccess.NONE
}

private fun Context.isGranted(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

@Stable
class LocationPermissionState internal constructor(private val activity: Activity) {

    var access by mutableStateOf(activity.locationAccess())
        private set

    /** System nie pokaże już okna z pytaniem – zostają tylko ustawienia aplikacji. */
    var permanentlyDenied by mutableStateOf(false)
        private set

    internal lateinit var launcher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>

    /** Pyta o dokładną lokalizację albo, jeśli to już niemożliwe, otwiera ustawienia aplikacji. */
    fun request() {
        if (permanentlyDenied) openAppSettings() else launcher.launch(LOCATION_PERMISSIONS)
    }

    internal fun onResult() {
        refresh()
        permanentlyDenied = access != LocationAccess.PRECISE &&
            !activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    internal fun refresh() {
        access = activity.locationAccess()
        if (access == LocationAccess.PRECISE) permanentlyDenied = false
    }

    private fun openAppSettings() {
        activity.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", activity.packageName, null)),
        )
    }
}

@Composable
fun rememberLocationPermissionState(): LocationPermissionState {
    val activity = LocalContext.current.findActivity()
    val state = remember(activity) { LocationPermissionState(activity) }
    state.launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { state.onResult() }

    // Użytkownik mógł zmienić uprawnienia w ustawieniach – sprawdzamy po powrocie do aplikacji.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, state) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) state.refresh()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return state
}

private tailrec fun Context.findActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("Brak aktywności w kontekście")
}
