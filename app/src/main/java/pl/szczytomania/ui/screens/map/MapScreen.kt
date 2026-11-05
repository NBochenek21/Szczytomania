package pl.szczytomania.ui.screens.map

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import pl.szczytomania.R
import pl.szczytomania.data.model.Peak
import pl.szczytomania.location.LocationAccess
import pl.szczytomania.location.LocationPermissionState
import pl.szczytomania.location.rememberLocationPermissionState
import pl.szczytomania.map.MapLibreMapView
import pl.szczytomania.map.MapStyle
import pl.szczytomania.map.POLAND_CAMERA
import pl.szczytomania.map.PeakLayer
import pl.szczytomania.map.followUserLocation
import pl.szczytomania.map.showUserLocation
import pl.szczytomania.ui.components.Avatar
import pl.szczytomania.ui.components.SquareIconButton
import pl.szczytomania.ui.components.framed
import pl.szczytomania.ui.theme.SzczytomaniaTheme

/** Mapa z wczytanym stylem – dopiero wtedy można dodawać warstwy i lokalizację. */
private class LoadedMap(val map: MapLibreMap, val style: Style)

// Przybliżenie przy przejściu do szczytu wybranego z listy.
private const val PEAK_ZOOM = 12.0

@Composable
fun MapScreen(
    onMenuClick: () -> Unit = {},
    profileInitial: String = "",
    onProfileClick: () -> Unit = {},
    viewModel: MapViewModel = viewModel(factory = MapViewModel.Factory),
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val locationPermission = rememberLocationPermissionState()
    val peaks by viewModel.peaks.collectAsStateWithLifecycle()
    val selectedPeak by viewModel.selectedPeak.collectAsStateWithLifecycle()
    val summitCheck by viewModel.summitCheck.collectAsStateWithLifecycle()
    val conqueredIds by viewModel.conqueredIds.collectAsStateWithLifecycle()
    var loadedMap by remember { mutableStateOf<LoadedMap?>(null) }

    BackHandler(enabled = selectedPeak != null) { viewModel.selectPeak(null) }

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

    // Wybrany szczyt poza widokiem (np. otwarty z listy szczytów) – przesuwamy mapę na niego.
    LaunchedEffect(loadedMap, selectedPeak?.id) {
        val loaded = loadedMap ?: return@LaunchedEffect
        val peak = selectedPeak ?: return@LaunchedEffect
        loaded.map.showPeak(peak)
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
            // Mapa powstaje od nowa po powrocie z innego ekranu menu – wraca tam, gdzie była.
            initialCamera = viewModel.lastCamera ?: POLAND_CAMERA,
            onStyleLoaded = { map, style ->
                PeakLayer.addTo(style, context)
                map.addOnCameraIdleListener { viewModel.lastCamera = map.cameraPosition }
                map.addOnMapClickListener { point ->
                    viewModel.selectPeak(PeakLayer.peakIdAt(map, point, density))
                    true
                }
                loadedMap = LoadedMap(map, style)
            },
        )

        Row(
            modifier = Modifier.align(Alignment.TopStart).fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SquareIconButton(R.drawable.ic_menu, stringResource(R.string.menu_open), onMenuClick)
            AppTitlePill()
            Spacer(Modifier.weight(1f))
            // Profil – prawy górny róg, jak w karcie projektu i na mockupie mapy.
            Avatar(
                initial = profileInitial,
                size = 52.dp,
                textStyle = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClickLabel = stringResource(R.string.profile_open), role = Role.Button, onClick = onProfileClick),
            )
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = if (selectedPeak == null) 16.dp else 0.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SquareIconButton(
                    icon = R.drawable.ic_my_location,
                    contentDescription = stringResource(R.string.map_center_on_me),
                    onClick = {
                        if (locationPermission.access == LocationAccess.NONE) {
                            locationPermission.request()
                        } else {
                            loadedMap?.map?.followUserLocation()
                        }
                    },
                    modifier = Modifier.align(Alignment.End).size(56.dp),
                )

                if (locationPermission.access != LocationAccess.PRECISE) {
                    LocationPermissionBanner(state = locationPermission)
                }
            }

            selectedPeak?.let { peak ->
                PeakCard(
                    peak = peak,
                    conquered = peak.id in conqueredIds,
                    checkState = summitCheck,
                    onConquer = {
                        if (locationPermission.access == LocationAccess.PRECISE) {
                            viewModel.checkSummit()
                        } else {
                            locationPermission.request()
                        }
                    },
                    onClose = { viewModel.selectPeak(null) },
                )
            }
        }
    }
}

/** Nazwa aplikacji w ramce ze znakiem szlaku – górny pasek mapy (mockup 02_Mapa_ekran_glowny). */
@Composable
private fun AppTitlePill(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.height(48.dp).framed(radius = 14.dp, borderWidth = 1.5.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TrailMark()
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
    }
}

/** Znak szlaku: biały – czerwony – biały, jak na mockupach (pasek mapy i menu). */
@Composable
fun TrailMark(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(3.dp)
    Column(
        modifier = modifier
            .width(22.dp)
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape),
    ) {
        Box(Modifier.fillMaxWidth().height(7.dp).background(Color.White))
        Box(Modifier.fillMaxWidth().height(7.dp).background(SzczytomaniaTheme.colors.trail))
        Box(Modifier.fillMaxWidth().height(7.dp).background(Color.White))
    }
}

/** Przesuwa mapę na szczyt, jeśli nie jest widoczny, i przestaje wtedy podążać za użytkownikiem. */
private fun MapLibreMap.showPeak(peak: Peak) {
    val target = LatLng(peak.lat, peak.lon)
    if (projection.visibleRegion.latLngBounds.contains(target)) return
    if (locationComponent.isLocationComponentActivated) locationComponent.cameraMode = CameraMode.NONE
    animateCamera(CameraUpdateFactory.newLatLngZoom(target, maxOf(cameraPosition.zoom, PEAK_ZOOM)))
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
