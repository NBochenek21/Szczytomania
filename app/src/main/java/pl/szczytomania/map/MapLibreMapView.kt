package pl.szczytomania.map

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

// Cała Polska na ekranie przy pierwszym uruchomieniu.
val POLAND_CAMERA: CameraPosition = CameraPosition.Builder()
    .target(LatLng(51.9, 19.1))
    .zoom(5.0)
    .build()

/**
 * Mapa MapLibre osadzona w Compose. MapView ma własny cykl życia,
 * więc przekazujemy mu zdarzenia cyklu życia ekranu.
 */
@Composable
fun MapLibreMapView(
    styleUrl: String,
    modifier: Modifier = Modifier,
    initialCamera: CameraPosition = POLAND_CAMERA,
    fallbackStyleJson: String? = null,
    onStyleLoaded: (MapLibreMap, Style) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val currentOnStyleLoaded by rememberUpdatedState(onStyleLoaded)
    val styleLoader = remember { StyleLoader(context, styleUrl, fallbackStyleJson) }
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            getMapAsync { map ->
                map.cameraPosition = initialCamera
                styleLoader.start(this, map) { style -> currentOnStyleLoaded(map, style) }
            }
        }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            // Domykamy cykl życia, jeśli mapa znika z ekranu, a aktywność dalej działa.
            val state = lifecycle.currentState
            if (state.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
            if (state.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
            mapView.onDestroy()
            styleLoader.stop()
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

/**
 * Wczytuje styl mapy. Gdy się nie uda (pierwsze uruchomienie bez internetu, styl nie jest w pamięci
 * telefonu), wczytuje styl zapasowy – szczyty i pozycja użytkownika działają dalej – a po powrocie
 * internetu próbuje jeszcze raz. Jeśli styl nie wczytał się mimo internetu (np. zły klucz MapTiler),
 * zostaje zapasowy: ponawianie w kółko nic by nie dało.
 *
 * Bez sieci MapLibre nie zgłasza błędu, tylko czeka na połączenie – dlatego bez internetu dajemy
 * mu [OFFLINE_WAIT_MS] na wczytanie stylu z pamięci telefonu, a potem przełączamy na zapasowy.
 */
private class StyleLoader(
    private val context: Context,
    private val styleUrl: String,
    private val fallbackJson: String?,
) {
    private val mainThread = Handler(Looper.getMainLooper())
    private var stopWatching: (() -> Unit)? = null
    private var usingFallback = false
    private var styleLoaded = false

    fun start(mapView: MapView, map: MapLibreMap, onLoaded: (Style) -> Unit) {
        mapView.addOnDidFailLoadingMapListener { error ->
            Log.w(TAG, "Nie udało się wczytać stylu mapy: $error")
            useFallback(map, onLoaded)
        }
        loadStyle(map, onLoaded)
        mainThread.postDelayed({ if (!styleLoaded && !context.isOnline()) useFallback(map, onLoaded) }, OFFLINE_WAIT_MS)
    }

    private fun loadStyle(map: MapLibreMap, onLoaded: (Style) -> Unit) {
        map.setStyle(Style.Builder().fromUri(styleUrl)) {
            styleLoaded = true
            onLoaded(it)
        }
    }

    private fun useFallback(map: MapLibreMap, onLoaded: (Style) -> Unit) {
        if (usingFallback || styleLoaded || fallbackJson == null) return
        usingFallback = true
        map.setStyle(Style.Builder().fromJson(fallbackJson)) { onLoaded(it) }
        if (!context.isOnline()) retryWhenOnline(map, onLoaded)
    }

    private fun retryWhenOnline(map: MapLibreMap, onLoaded: (Style) -> Unit) {
        stopWatching?.invoke()
        stopWatching = context.watchConnectivity { online ->
            if (!online) return@watchConnectivity
            mainThread.post {
                if (!usingFallback) return@post
                stop()
                usingFallback = false
                loadStyle(map, onLoaded)
            }
        }
    }

    fun stop() {
        mainThread.removeCallbacksAndMessages(null)
        stopWatching?.invoke()
        stopWatching = null
    }

    private companion object {
        const val TAG = "MapLibreMapView"

        // Styl z pamięci telefonu wczytuje się w ułamku sekundy – po tym czasie go tam nie ma.
        const val OFFLINE_WAIT_MS = 4_000L
    }
}
