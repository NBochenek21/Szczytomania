package pl.szczytomania.map

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import org.maplibre.android.location.LocationComponent
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.engine.LocationEngineCallback
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.engine.LocationEngineResult
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import pl.szczytomania.location.LocationAccess
import pl.szczytomania.location.locationAccess

// Przybliżenie, przy którym widać okoliczne szczyty i szlaki.
private const val USER_ZOOM = 12.0
private const val CAMERA_ANIMATION_MS = 750L

/**
 * Pokazuje pozycję użytkownika (niebieska kropka z kierunkiem) – jeśli dał dostęp do lokalizacji.
 * Zwraca, czy kropka jest widoczna.
 */
@SuppressLint("MissingPermission") // Uprawnienie sprawdzamy wyżej, przez locationAccess().
fun MapLibreMap.showUserLocation(context: Context, style: Style): Boolean {
    if (context.locationAccess() == LocationAccess.NONE) return false
    locationComponent.apply {
        if (!isLocationComponentActivated) {
            activateLocationComponent(
                LocationComponentActivationOptions.builder(context, style)
                    .useDefaultLocationEngine(true)
                    .build(),
            )
        }
        isLocationComponentEnabled = true
        renderMode = RenderMode.COMPASS
    }
    return true
}

/**
 * Przesuwa mapę na użytkownika i podąża za nim, dopóki sam nie przesunie mapy.
 * Jeśli pozycja nie jest jeszcze znana (GPS dopiero szuka sygnału), czeka na pierwszą –
 * inaczej mapa przybliżyłaby się w miejscu, w którym akurat stoi kamera.
 */
@SuppressLint("MissingPermission") // Komponent jest włączony tylko z uprawnieniem – patrz showUserLocation().
fun MapLibreMap.followUserLocation() {
    val component = locationComponent
    if (!component.isLocationComponentActivated || !component.isLocationComponentEnabled) return

    component.lastKnownLocation?.let {
        component.trackUser()
        return
    }

    val engine = component.locationEngine ?: return
    engine.requestLocationUpdates(
        LocationEngineRequest.Builder(1_000).build(),
        object : LocationEngineCallback<LocationEngineResult> {
            override fun onSuccess(result: LocationEngineResult) {
                val location: Location = result.lastLocation ?: return
                engine.removeLocationUpdates(this)
                component.forceLocationUpdate(location)
                component.trackUser()
            }

            override fun onFailure(exception: Exception) {
                engine.removeLocationUpdates(this)
            }
        },
        Looper.getMainLooper(),
    )
}

private fun LocationComponent.trackUser() {
    setCameraMode(CameraMode.TRACKING, CAMERA_ANIMATION_MS, USER_ZOOM, null, null, null)
}
