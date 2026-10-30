package pl.szczytomania.ui.screens.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import pl.szczytomania.R
import pl.szczytomania.location.LocationAccess
import pl.szczytomania.location.LocationPermissionState
import pl.szczytomania.location.rememberLocationPermissionState
import pl.szczytomania.map.MapLibreMapView
import pl.szczytomania.map.MapStyle
import pl.szczytomania.map.PeakLayer
import pl.szczytomania.map.followUserLocation
import pl.szczytomania.map.showUserLocation

/** Mapa z wczytanym stylem – dopiero wtedy można dodawać warstwy i lokalizację. */
private class LoadedMap(val map: MapLibreMap, val style: Style)

@Composable
fun MapScreen(
    onMenuClick: () -> Unit = {},
    viewModel: MapViewModel = viewModel(factory = MapViewModel.Factory),
) {
    val context = LocalContext.current
    val locationPermission = rememberLocationPermissionState()
    val peaks by viewModel.peaks.collectAsStateWithLifecycle()
    var loadedMap by remember { mutableStateOf<LoadedMap?>(null) }

    // Pytamy o lokalizację raz, przy pierwszym otwarciu mapy (także po obrocie ekranu nie ponawiamy).
    var askedOnStart by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!askedOnStart && locationPermission.access != LocationAccess.PRECISE) {
            askedOnStart = true
            locationPermission.request()
        }
    }

    LaunchedEffect(loadedMap, peaks) {
        loadedMap?.let { PeakLayer.setPeaks(it.style, peaks) }
    }

    // Kropka pojawia się, gdy tylko mamy styl i uprawnienie – także po zgodzie udzielonej później.
    // Za pierwszym razem mapa sama przesuwa się na użytkownika.
    var centeredOnUser by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(loadedMap, locationPermission.access) {
        val loaded = loadedMap ?: return@LaunchedEffect
        if (loaded.map.showUserLocation(context, loaded.style) && !centeredOnUser) {
            centeredOnUser = true
            loaded.map.followUserLocation()
        }
    }

    Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        MapLibreMapView(
            styleUrl = MapStyle.url,
            modifier = Modifier.fillMaxSize(),
            onStyleLoaded = { map, style ->
                PeakLayer.addTo(style, context)
                loadedMap = LoadedMap(map, style)
            },
        )

        SmallFloatingActionButton(
            onClick = onMenuClick,
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_menu),
                contentDescription = stringResource(R.string.menu_open),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FloatingActionButton(
                onClick = {
                    if (locationPermission.access == LocationAccess.NONE) {
                        locationPermission.request()
                    } else {
                        loadedMap?.map?.followUserLocation()
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.align(Alignment.End),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_my_location),
                    contentDescription = stringResource(R.string.map_center_on_me),
                )
            }

            if (locationPermission.access != LocationAccess.PRECISE) {
                LocationPermissionBanner(state = locationPermission)
            }
        }
    }
}

@Composable
private fun LocationPermissionBanner(
    state: LocationPermissionState,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(
                    if (state.access == LocationAccess.APPROXIMATE) {
                        R.string.location_permission_approximate
                    } else {
                        R.string.location_permission_denied
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = state::request,
                modifier = Modifier.align(Alignment.End).padding(top = 8.dp),
            ) {
                Text(
                    stringResource(
                        if (state.permanentlyDenied) {
                            R.string.location_permission_open_settings
                        } else {
                            R.string.location_permission_grant
                        },
                    ),
                )
            }
        }
    }
}
