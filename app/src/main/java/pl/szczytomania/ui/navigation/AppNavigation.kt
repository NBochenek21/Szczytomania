package pl.szczytomania.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import pl.szczytomania.BuildConfig
import pl.szczytomania.R
import pl.szczytomania.ui.components.Avatar
import pl.szczytomania.ui.components.DashedDivider
import pl.szczytomania.ui.screens.map.MapScreen
import pl.szczytomania.ui.screens.map.MapViewModel
import pl.szczytomania.ui.screens.map.TrailMark
import pl.szczytomania.ui.screens.peaks.PeaksScreen
import pl.szczytomania.ui.screens.profile.ProfileScreen
import pl.szczytomania.ui.screens.profile.ProfileUiState
import pl.szczytomania.ui.screens.profile.ProfileViewModel
import pl.szczytomania.ui.screens.sets.SetDetailScreen
import pl.szczytomania.ui.screens.sets.SetsScreen
import pl.szczytomania.ui.theme.NumberStyles

/**
 * Ekrany po zalogowaniu. Te z ikoną są w menu (hamburgerze); profil otwiera się z awatara
 * na mapie i z nagłówka menu. Kolejne pozycje z mockupu dojdą razem z ich ekranami.
 */
enum class AppScreen(@param:StringRes val label: Int, @param:DrawableRes val icon: Int? = null) {
    MAP(R.string.nav_map, R.drawable.ic_map),
    SETS(R.string.nav_sets, R.drawable.ic_mountains),
    PEAKS(R.string.peaks_title, R.drawable.ic_list),
    PROFILE(R.string.profile_title),
}

/**
 * Szuflada menu i przełączanie ekranów po zalogowaniu. Ekranem startowym jest mapa,
 * a przycisk "wstecz" zamyka kolejno: menu, szczegóły zestawu, a z innego ekranu wraca na mapę.
 */
@Composable
fun AppNavigation() {
    var current by rememberSaveable { mutableStateOf(AppScreen.MAP) }
    // Otwarty zestaw (szczegóły) w zakładce "Zestawy gór"; null = lista zestawów.
    var openSetId by rememberSaveable { mutableStateOf<String?>(null) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    // Zachowuje stan ekranu (np. to, że już pytaliśmy o lokalizację) po przejściu na inny i z powrotem.
    val stateHolder = rememberSaveableStateHolder()
    // Ten sam ViewModel co w MapScreen (wspólny dla aktywności) – przez niego lista otwiera szczyt na mapie.
    val mapViewModel: MapViewModel = viewModel(factory = MapViewModel.Factory)
    // Kluczem jest uid – po wylogowaniu i zalogowaniu na inne konto powstaje nowy ViewModel profilu.
    val uid = remember { FirebaseAuth.getInstance().currentUser?.uid }
    val profileViewModel: ProfileViewModel = viewModel(key = "profile-$uid", factory = ProfileViewModel.Factory)
    val profile by profileViewModel.state.collectAsStateWithLifecycle()

    val openMenu: () -> Unit = { scope.launch { drawerState.open() } }
    val showPeakOnMap: (String) -> Unit = { peakId ->
        mapViewModel.selectPeak(peakId)
        current = AppScreen.MAP
    }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }
    BackHandler(enabled = drawerState.isClosed && current == AppScreen.SETS && openSetId != null) {
        openSetId = null
    }
    BackHandler(enabled = drawerState.isClosed && current != AppScreen.MAP && openSetId == null) {
        current = AppScreen.MAP
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        // Na mapie przeciąganie przesuwa mapę, więc szufladę otwiera tam tylko przycisk.
        gesturesEnabled = drawerState.isOpen || current != AppScreen.MAP,
        drawerContent = {
            DrawerContent(
                current = current,
                profile = profile,
                onSelect = { screen ->
                    current = screen
                    if (screen == AppScreen.SETS) openSetId = null
                    scope.launch { drawerState.close() }
                },
            )
        },
    ) {
        val setId = openSetId
        val key = if (current == AppScreen.SETS && setId != null) "set/$setId" else current.name
        stateHolder.SaveableStateProvider(key) {
            when (current) {
                AppScreen.MAP -> MapScreen(
                    onMenuClick = openMenu,
                    profileInitial = profile.initial,
                    onProfileClick = { current = AppScreen.PROFILE },
                    viewModel = mapViewModel,
                )
                AppScreen.SETS -> if (setId == null) {
                    SetsScreen(onMenuClick = openMenu, onSetClick = { openSetId = it })
                } else {
                    SetDetailScreen(setId = setId, onBack = { openSetId = null }, onPeakClick = showPeakOnMap)
                }
                AppScreen.PEAKS -> PeaksScreen(onMenuClick = openMenu, onPeakClick = showPeakOnMap)
                AppScreen.PROFILE -> ProfileScreen(onBack = { current = AppScreen.MAP }, viewModel = profileViewModel)
            }
        }
    }
}

/** Zawartość menu (mockup 03_Menu_hamburger): zielony nagłówek, pozycje i stopka. */
@Composable
private fun DrawerContent(current: AppScreen, profile: ProfileUiState, onSelect: (AppScreen) -> Unit) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = RoundedCornerShape(topEnd = 0.dp, bottomEnd = 0.dp),
        // Bez odstępu od paska stanu – zielony nagłówek sięga do górnej krawędzi, jak na mockupie.
        windowInsets = WindowInsets(0),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TrailMark()
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            // Użytkownik jak na mockupie: awatar, nick, liczba szczytów i postęp w Koronie.
            Row(
                modifier = Modifier
                    .padding(top = 20.dp)
                    .clickable { onSelect(AppScreen.PROFILE) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Avatar(
                    initial = profile.initial,
                    size = 48.dp,
                    textStyle = MaterialTheme.typography.titleLarge,
                    inverted = true,
                )
                Column {
                    Text(profile.nick, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
                    profile.stats?.let { stats ->
                        val kgp = stats.shownSets.find { it.set.id == "kgp" }
                        val peaks = "${stats.conqueredCount} ${pluralStringResource(R.plurals.profile_peaks, stats.conqueredCount)}"
                        Text(
                            text = if (kgp != null) "$peaks · KGP ${kgp.done}/${kgp.total}" else peaks,
                            style = NumberStyles.tag.copy(letterSpacing = TextUnit.Unspecified),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        AppScreen.entries.forEach { screen ->
            val icon = screen.icon ?: return@forEach
            NavigationDrawerItem(
                icon = { Icon(painterResource(icon), contentDescription = null) },
                label = { Text(stringResource(screen.label), style = MaterialTheme.typography.bodyLarge) },
                selected = screen == current,
                onClick = { onSelect(screen) },
                shape = RoundedCornerShape(14.dp),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            )
        }

        Spacer(Modifier.weight(1f))
        DashedDivider()
        Text(
            text = stringResource(R.string.drawer_footer, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp),
        )
    }
}
