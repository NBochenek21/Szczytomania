package pl.szczytomania.ui.screens.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.szczytomania.R
import pl.szczytomania.data.model.Peak
import pl.szczytomania.data.model.PeakSet
import pl.szczytomania.ui.components.Avatar
import pl.szczytomania.ui.components.ProgressBar
import pl.szczytomania.ui.components.ScreenHeader
import pl.szczytomania.ui.screens.sets.Counter
import pl.szczytomania.ui.theme.NumberStyles
import pl.szczytomania.ui.theme.SzczytomaniaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Ekran profilu (mockup 11_Profil): awatar, nick, statystyki, postęp w zestawach i wylogowanie.
 * Motyw, edycja konta, ustawienia, mapy regionów i prywatność dochodzą w tygodniu 5.
 */
@Composable
fun ProfileScreen(onBack: () -> Unit, viewModel: ProfileViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ProfileContent(state = state, onBack = onBack, onSignOut = viewModel::signOut)
}

@Composable
fun ProfileContent(
    state: ProfileUiState,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
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
            Avatar(
                initial = state.initial,
                size = 88.dp,
                textStyle = MaterialTheme.typography.headlineLarge,
                dashedRing = true,
            )
            Column(modifier = Modifier.padding(start = 18.dp)) {
                Text(state.nick, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                state.memberSince?.let {
                    Text(
                        text = stringResource(R.string.profile_member_since, rememberMonthYear(it)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        state.stats?.let { stats ->
            StatsCard(stats)
            if (stats.shownSets.isNotEmpty()) SetsCard(stats)
        }

        Card {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSignOut, role = Role.Button)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painterResource(R.drawable.ic_logout),
                    contentDescription = null,
                    tint = SzczytomaniaTheme.colors.trail,
                )
                Text(
                    text = stringResource(R.string.profile_sign_out),
                    style = MaterialTheme.typography.titleMedium,
                    color = SzczytomaniaTheme.colors.trail,
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
        }
    }
}

/** "szczyt" / "szczyty" / "szczytów" – zależnie od liczby. */
@Composable
fun peaksLabel(count: Int): String = stringResource(
    when (polishPlural(count)) {
        PolishPlural.ONE -> R.string.profile_peaks_one
        PolishPlural.FEW -> R.string.profile_peaks_few
        PolishPlural.MANY -> R.string.profile_peaks_many
    },
)

/** "sierpnia 2026" – miesiąc w dopełniaczu, pasuje do "w Szczytomanii od …". */
@Composable
private fun rememberMonthYear(date: Date): String =
    remember(date) { SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("pl")).format(date) }

@Composable
private fun Card(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape),
    ) { content() }
}

@Composable
private fun StatsCard(stats: ProfileStats) {
    Card {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(vertical = 16.dp)) {
            Stat(
                value = stats.conqueredCount.toString(),
                label = peaksLabel(stats.conqueredCount),
                modifier = Modifier.weight(1f),
            )
            VerticalDashedLine()
            Stat(
                value = stats.highest?.let { stringResource(R.string.peaks_elevation, it.elevation) } ?: "–",
                label = stats.highest?.let { stringResource(R.string.profile_highest_peak, it.name) }
                    ?: stringResource(R.string.profile_highest),
                modifier = Modifier.weight(1.3f),
            )
            VerticalDashedLine()
            Stat(
                value = stats.startedSets.toString(),
                label = stringResource(
                    when (polishPlural(stats.startedSets)) {
                        PolishPlural.ONE -> R.string.profile_sets_one
                        PolishPlural.FEW -> R.string.profile_sets_few
                        PolishPlural.MANY -> R.string.profile_sets_many
                    },
                ),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = NumberStyles.counter.copy(fontSize = NumberStyles.big.fontSize * 0.75f), color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun VerticalDashedLine() {
    val color = SzczytomaniaTheme.colors.lines
    Canvas(Modifier.width(1.dp).fillMaxHeight()) {
        drawLine(
            color = color,
            start = Offset(size.width / 2, 0f),
            end = Offset(size.width / 2, size.height),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
        )
    }
}

@Composable
private fun SetsCard(stats: ProfileStats) {
    Card {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
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
}

@Preview(showBackground = true)
@Composable
private fun ProfileContentPreview() {
    val peaks = listOf(
        Peak("node/1", "Rysy", 2499, 49.18, 20.09, "Tatry", kgp = true),
        Peak("node/2", "Śnieżka", 1603, 50.74, 15.74, "Karkonosze", kgp = true),
    ).associateBy { it.id }
    val sets = listOf(PeakSet("kgp", "Korona Gór Polski", "opis", peaks.keys.toList(), "Odznaka klasyczna"))
    SzczytomaniaTheme {
        ProfileContent(
            state = ProfileUiState(
                nick = "Norbert",
                memberSince = Date(),
                stats = ProfileStats.of(peaks, sets, setOf("node/2")),
            ),
            onBack = {},
            onSignOut = {},
        )
    }
}
