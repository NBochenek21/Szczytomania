package pl.szczytomania.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
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
    onMapReady: (MapLibreMap) -> Unit = {},
) {
    val context = LocalContext.current
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            getMapAsync { map ->
                map.cameraPosition = initialCamera
                map.setStyle(styleUrl)
                onMapReady(map)
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
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}
