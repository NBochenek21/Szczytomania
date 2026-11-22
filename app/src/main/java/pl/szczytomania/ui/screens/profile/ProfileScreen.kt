package pl.szczytomania.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import pl.szczytomania.ui.components.PillButton
import pl.szczytomania.ui.theme.ThemeMode
import pl.szczytomania.ui.theme.ThemeSettings
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.szczytomania.R
import pl.szczytomania.notifications.rememberNotificationsSwitch
import pl.szczytomania.ui.components.Avatar
import pl.szczytomania.ui.components.DashedDivider
import pl.szczytomania.ui.components.ProgressBar
import pl.szczytomania.ui.components.ScreenHeader
import pl.szczytomania.ui.components.framed
import pl.szczytomania.ui.screens.sets.Counter
import pl.szczytomania.ui.theme.NumberStyles
import pl.szczytomania.ui.theme.SzczytomaniaTheme
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Ekran profilu (mockup 11_Profil): awatar, nick, statystyki, postęp w zestawach i wylogowanie.
 * Motyw, edycja konta, ustawienia, mapy regionów i prywatność dochodzą w tygodniu 5.
 */
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onEditAccount: () -> Unit,
    onOfflineMaps: () -> Unit,
    viewModel: ProfileViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // "listopada 2026" – miesiąc w dopełniaczu, pasuje do "w Szczytomanii od …".
    val memberSince = remember(state.memberSince) {
        state.memberSince?.let { SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("pl")).format(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ScreenHeader(
            title = stringResource(R.string.profile_title),
            navIcon = R.drawable.ic_arrow_back,
            navDescription = stringResource(R.string.back),
            onNavClick = onBack,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(state.initial, size = 88.dp, textStyle = MaterialTheme.typography.headlineLarge, dashedRing = true)
            Column(modifier = Modifier.padding(start = 18.dp)) {
                Text(state.nick, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                memberSince?.let {
                    Text(
                        text = stringResource(R.string.profile_member_since, it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        state.stats?.let { stats ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).framed().padding(vertical = 16.dp)) {
                Stat(stats.conqueredCount.toString(), pluralStringResource(R.plurals.profile_peaks, stats.conqueredCount))
                DashedDivider(vertical = true)
                Stat(
                    value = stats.highest?.let { stringResource(R.string.peaks_elevation, it.elevation) } ?: "–",
                    label = stats.highest?.let { stringResource(R.string.profile_highest_peak, it.name) }
                        ?: stringResource(R.string.profile_highest),
                    weight = 1.3f,
                )
                DashedDivider(vertical = true)
                Stat(stats.startedSets.toString(), pluralStringResource(R.plurals.profile_sets, stats.startedSets))
            }

            Column(Modifier.fillMaxWidth().framed().padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                stats.shownSets.forEach { progress ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = progress.set.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            Counter(done = progress.done, total = progress.total)
                        }
                        ProgressBar(progress.fraction)
                    }
                }
            }
        }

        // Ustawienia (mockup 11_Profil): motyw, edycja konta, pobrane mapy, wylogowanie.
        Column(Modifier.fillMaxWidth().framed()) {
            SettingsRow(R.drawable.ic_moon, stringResource(R.string.settings_theme)) {
                val context = LocalContext.current
                val dark = when (ThemeSettings.mode.collectAsStateWithLifecycle().value) {
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PillButton(stringResource(R.string.settings_theme_light), selected = !dark, onClick = { ThemeSettings.set(context, ThemeMode.LIGHT) })
                    PillButton(stringResource(R.string.settings_theme_dark), selected = dark, onClick = { ThemeSettings.set(context, ThemeMode.DARK) })
                }
            }
            DashedDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow(R.drawable.ic_person, stringResource(R.string.account_title), onClick = onEditAccount)
            DashedDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow(R.drawable.ic_download, stringResource(R.string.settings_offline_maps), onClick = onOfflineMaps)
            DashedDivider(Modifier.padding(horizontal = 16.dp))
            // Powiadomienia push o znajomych (zdobycze, zaproszenia) – notifications/.
            val notifications = rememberNotificationsSwitch()
            SettingsRow(R.drawable.ic_bell, stringResource(R.string.settings_notifications), onClick = notifications::toggle) {
                Switch(checked = notifications.checked, onCheckedChange = { notifications.toggle() })
            }
            DashedDivider(Modifier.padding(horizontal = 16.dp))
            SettingsRow(R.drawable.ic_logout, stringResource(R.string.profile_sign_out), onClick = viewModel::signOut, danger = true)
        }
    }
}

/** Wiersz karty ustawień: ikona, nazwa i po prawej strzałka albo własna zawartość (np. przełącznik motywu). */
@Composable
private fun SettingsRow(
    icon: Int,
    label: String,
    onClick: (() -> Unit)? = null,
    danger: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val color = if (danger) SzczytomaniaTheme.colors.trail else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick, role = Role.Button) else Modifier)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = color)
        Text(
            text = label,
            style = if (danger) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            color = color,
            modifier = Modifier.weight(1f).padding(start = 16.dp),
        )
        when {
            trailing != null -> trailing()
            onClick != null && !danger -> Text("›", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RowScope.Stat(value: String, label: String, weight: Float = 1f) {
    Column(Modifier.weight(weight).padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = NumberStyles.counter.copy(fontSize = NumberStyles.big.fontSize * 0.75f), color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
