package pl.szczytomania.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import pl.szczytomania.ui.theme.SzczytomaniaTheme
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import pl.szczytomania.notifications.Notifications
import pl.szczytomania.notifications.PushTokens
import pl.szczytomania.ui.components.Avatar
import pl.szczytomania.ui.components.DashedDivider
import pl.szczytomania.data.firestore.UserRepository
import pl.szczytomania.data.repository.Friends
import pl.szczytomania.ui.screens.conquest.ConquestScreen
import pl.szczytomania.ui.screens.friends.FriendProfileScreen
import pl.szczytomania.ui.screens.friends.FriendProfileViewModel
import pl.szczytomania.ui.screens.friends.FriendsScreen
import pl.szczytomania.ui.screens.friends.FriendsViewModel
import pl.szczytomania.ui.screens.conquest.ConquestViewModel
import pl.szczytomania.ui.screens.map.MapScreen
import pl.szczytomania.ui.screens.myconquests.MyConquestsScreen
import pl.szczytomania.ui.screens.map.MapViewModel
import pl.szczytomania.ui.screens.map.TrailMark
import pl.szczytomania.ui.screens.peaks.PeaksScreen
import pl.szczytomania.ui.screens.profile.EditAccountScreen
import pl.szczytomania.ui.screens.profile.EditAccountViewModel
import pl.szczytomania.ui.screens.profile.ProfileScreen
import pl.szczytomania.ui.screens.profile.ProfileUiState
import pl.szczytomania.ui.screens.profile.ProfileViewModel
import pl.szczytomania.ui.screens.sets.NewSetScreen
import pl.szczytomania.ui.screens.sets.SetDetailScreen
import pl.szczytomania.ui.screens.settings.OfflineMapsScreen
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
    MY_CONQUESTS(R.string.nav_my_conquests, R.drawable.ic_check_circle),
    FRIENDS(R.string.nav_friends, R.drawable.ic_friends),
    OFFLINE_MAPS(R.string.nav_offline_maps, R.drawable.ic_download),
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
    // Ekrany "pod spodem": nowy zestaw (w Zestawach gór) i edycja konta (w Profilu).
    var creatingSet by rememberSaveable { mutableStateOf(false) }
    var editingAccount by rememberSaveable { mutableStateOf(false) }
    // Otwarty profil znajomego (uid) w zakładce "Znajomi"; null = lista znajomych.
    var openFriendUid by rememberSaveable { mutableStateOf<String?>(null) }
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
    val friendsViewModel: FriendsViewModel = viewModel(key = "friends-$uid", factory = FriendsViewModel.Factory)
    // Czerwone kółko przy "Znajomi" w menu – liczba zaproszeń czekających na odpowiedź.
    val invitations by Friends.incomingCount.collectAsStateWithLifecycle(initialValue = 0)
    // Konta sprzed wyszukiwania znajomych dostają nickLower i rezerwację nicku (raz, w tle).
    LaunchedEffect(uid) { uid?.let { UserRepository().ensureSearchable(it) } }
    // Zdobywanie szczytu: pełny ekran nad pozostałymi, otwierany z karty szczytu.
    val conquestViewModel: ConquestViewModel = viewModel(factory = ConquestViewModel.Factory)
    var conquering by rememberSaveable { mutableStateOf(false) }
    // Token tego telefonu do powiadomień push (Cloud Functions wysyłają na niego wiadomości).
    val context = LocalContext.current
    LaunchedEffect(uid) { if (uid != null) PushTokens.register(context) }
    // Dotknięte powiadomienie: profil znajomego albo lista znajomych (zaproszenie).
    val notificationTarget by Notifications.target.collectAsStateWithLifecycle()
    LaunchedEffect(notificationTarget) {
        val target = notificationTarget ?: return@LaunchedEffect
        Notifications.consumeTarget()
        conquering = false
        openSetId = null
        creatingSet = false
        editingAccount = false
        current = AppScreen.FRIENDS
        openFriendUid = target.friendUid
        drawerState.close()
    }
    val closeConquest = {
        conquering = false
        mapViewModel.selectPeak(null)
    }

    val openMenu: () -> Unit = { scope.launch { drawerState.open() } }
    val showPeakOnMap: (String) -> Unit = { peakId ->
        mapViewModel.selectPeak(peakId)
        current = AppScreen.MAP
    }

    BackHandler(enabled = conquering) { closeConquest() }
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }
    val subScreenOpen = openSetId != null || creatingSet || editingAccount || openFriendUid != null
    BackHandler(enabled = drawerState.isClosed && subScreenOpen) {
        openSetId = null
        creatingSet = false
        editingAccount = false
        openFriendUid = null
    }
    BackHandler(enabled = drawerState.isClosed && current != AppScreen.MAP && !subScreenOpen) {
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
                invitations = invitations,
                onSelect = { screen ->
                    current = screen
                    openSetId = null
                    creatingSet = false
                    editingAccount = false
                    openFriendUid = null
                    scope.launch { drawerState.close() }
                },
            )
        },
    ) {
        if (conquering) {
            ConquestScreen(
                viewModel = conquestViewModel,
                onClose = closeConquest,
                onMyConquests = {
                    closeConquest()
                    current = AppScreen.MY_CONQUESTS
                },
            )
        } else {
            val setId = openSetId
            val friendUid = openFriendUid
            val key = when {
                current == AppScreen.SETS && creatingSet -> "new-set"
                current == AppScreen.SETS && setId != null -> "set/$setId"
                current == AppScreen.PROFILE && editingAccount -> "account"
                current == AppScreen.FRIENDS && friendUid != null -> "friend/$friendUid"
                else -> current.name
            }
            stateHolder.SaveableStateProvider(key) {
                when (current) {
                    AppScreen.MAP -> MapScreen(
                        onMenuClick = openMenu,
                        profileInitial = profile.initial,
                        onProfileClick = { current = AppScreen.PROFILE },
                        onConquer = { peakId ->
                            conquestViewModel.start(peakId)
                            conquering = true
                        },
                        onOfflineMaps = { current = AppScreen.OFFLINE_MAPS },
                        viewModel = mapViewModel,
                    )
                    AppScreen.SETS -> when {
                        creatingSet -> NewSetScreen(onClose = { creatingSet = false }, onSaved = { creatingSet = false })
                        setId == null -> SetsScreen(onMenuClick = openMenu, onSetClick = { openSetId = it }, onCreateSet = { creatingSet = true })
                        else -> SetDetailScreen(setId = setId, onBack = { openSetId = null }, onPeakClick = showPeakOnMap)
                    }
                    AppScreen.PEAKS -> PeaksScreen(onMenuClick = openMenu, onPeakClick = showPeakOnMap)
                    AppScreen.MY_CONQUESTS -> MyConquestsScreen(onMenuClick = openMenu)
                    AppScreen.FRIENDS -> if (friendUid == null) {
                        FriendsScreen(onMenuClick = openMenu, onFriendClick = { openFriendUid = it }, viewModel = friendsViewModel)
                    } else {
                        FriendProfileScreen(
                            friendUid = friendUid,
                            onBack = { openFriendUid = null },
                            viewModel = viewModel(key = "friend-$uid-$friendUid", factory = FriendProfileViewModel.factory(friendUid)),
                        )
                    }
                    AppScreen.OFFLINE_MAPS -> OfflineMapsScreen(onMenuClick = openMenu)
                    AppScreen.PROFILE -> if (editingAccount) {
                        EditAccountScreen(
                            currentNick = profile.nick,
                            onBack = { editingAccount = false },
                            viewModel = viewModel(key = "account-$uid", factory = EditAccountViewModel.Factory),
                        )
                    } else {
                        ProfileScreen(
                            onBack = { current = AppScreen.MAP },
                            onEditAccount = { editingAccount = true },
                            onOfflineMaps = { current = AppScreen.OFFLINE_MAPS },
                            viewModel = profileViewModel,
                        )
                    }
                }
            }
        }
    }
}

/** Zawartość menu (mockup 03_Menu_hamburger): zielony nagłówek, pozycje i stopka. */
@Composable
private fun DrawerContent(current: AppScreen, profile: ProfileUiState, invitations: Int, onSelect: (AppScreen) -> Unit) {
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
                badge = if (screen == AppScreen.FRIENDS && invitations > 0) {
                    { CountBadge(invitations) }
                } else {
                    null
                },
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

/** Czerwone kółko z liczbą (zaproszenia do znajomych) – jak na mockupie menu. */
@Composable
private fun CountBadge(count: Int) {
    Box(
        Modifier.size(28.dp).clip(CircleShape).background(SzczytomaniaTheme.colors.trail),
        contentAlignment = Alignment.Center,
    ) {
        Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary)
    }
}
