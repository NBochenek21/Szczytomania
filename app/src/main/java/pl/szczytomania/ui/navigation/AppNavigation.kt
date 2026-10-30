package pl.szczytomania.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pl.szczytomania.R
import pl.szczytomania.ui.screens.map.MapScreen
import pl.szczytomania.ui.screens.peaks.PeaksScreen

/** Ekrany dostępne z menu (hamburgera). Kolejne pozycje z mockupu dojdą razem z ich ekranami. */
enum class AppScreen(@StringRes val label: Int) {
    MAP(R.string.nav_map),
    PEAKS(R.string.nav_peaks),
}

/**
 * Szuflada menu i przełączanie ekranów po zalogowaniu. Ekranem startowym jest mapa,
 * a przycisk "wstecz" z innego ekranu wraca na mapę.
 */
@Composable
fun AppNavigation() {
    var current by rememberSaveable { mutableStateOf(AppScreen.MAP) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    // Zachowuje stan ekranu (np. to, że już pytaliśmy o lokalizację) po przejściu na inny i z powrotem.
    val stateHolder = rememberSaveableStateHolder()

    val openMenu: () -> Unit = { scope.launch { drawerState.open() } }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }
    BackHandler(enabled = drawerState.isClosed && current != AppScreen.MAP) {
        current = AppScreen.MAP
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        // Na mapie przeciąganie przesuwa mapę, więc szufladę otwiera tam tylko przycisk.
        gesturesEnabled = drawerState.isOpen || current != AppScreen.MAP,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                )
                AppScreen.entries.forEach { screen ->
                    NavigationDrawerItem(
                        label = { Text(stringResource(screen.label)) },
                        selected = screen == current,
                        onClick = {
                            current = screen
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                }
            }
        },
    ) {
        stateHolder.SaveableStateProvider(current) {
            when (current) {
                AppScreen.MAP -> MapScreen(onMenuClick = openMenu)
                AppScreen.PEAKS -> PeaksScreen(onMenuClick = openMenu)
            }
        }
    }
}
