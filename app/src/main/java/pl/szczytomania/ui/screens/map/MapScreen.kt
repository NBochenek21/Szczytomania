package pl.szczytomania.ui.screens.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pl.szczytomania.R
import pl.szczytomania.location.LocationAccess
import pl.szczytomania.location.LocationPermissionState
import pl.szczytomania.location.rememberLocationPermissionState
import pl.szczytomania.map.MapLibreMapView
import pl.szczytomania.map.MapStyle

@Composable
fun MapScreen() {
    val locationPermission = rememberLocationPermissionState()

    // Pytamy o lokalizację raz, przy pierwszym otwarciu mapy (także po obrocie ekranu nie ponawiamy).
    var askedOnStart by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!askedOnStart && locationPermission.access != LocationAccess.PRECISE) {
            askedOnStart = true
            locationPermission.request()
        }
    }

    Box(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        MapLibreMapView(
            styleUrl = MapStyle.url,
            modifier = Modifier.fillMaxSize(),
        )

        if (locationPermission.access != LocationAccess.PRECISE) {
            LocationPermissionBanner(
                state = locationPermission,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
            )
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
