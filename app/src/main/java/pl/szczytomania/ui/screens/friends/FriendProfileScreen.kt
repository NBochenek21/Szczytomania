package pl.szczytomania.ui.screens.friends

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.szczytomania.R
import pl.szczytomania.ui.components.Avatar
import pl.szczytomania.ui.components.DashedDivider
import pl.szczytomania.ui.components.LoadingBox
import pl.szczytomania.ui.components.MountainThumb
import pl.szczytomania.ui.components.SquareIconButton
import pl.szczytomania.ui.components.Tag
import pl.szczytomania.ui.components.avatarColor
import pl.szczytomania.ui.components.framed
import pl.szczytomania.ui.screens.profile.avatarInitial
import pl.szczytomania.ui.screens.sets.SetProgress
import pl.szczytomania.ui.theme.NumberStyles
import pl.szczytomania.ui.theme.SzczytomaniaTheme
import java.text.SimpleDateFormat
import java.util.Locale

/** Tyle zdobyczy widać od razu; resztę pokazuje "Pokaż wszystkie". */
private const val SHOWN_CONQUESTS = 4

/** Profil znajomego (mockup 09): statystyki, wyścig w Koronie Gór Polski i lista jego zdobyczy. */
@Composable
fun FriendProfileScreen(friendUid: String, onBack: () -> Unit, viewModel: FriendProfileViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAll by rememberSaveable { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }
    val pl = remember { Locale.forLanguageTag("pl") }
    // "września 2026" – miesiąc w dopełniaczu, pasuje do "znajomi od …".
    val since = remember(state.since) { state.since?.let { SimpleDateFormat("MMMM yyyy", pl).format(it) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Jak na mockupie: strzałka wstecz i szare "Znajomi" zamiast tytułu – tytułem jest nick niżej.
        Row(verticalAlignment = Alignment.CenterVertically) {
            SquareIconButton(R.drawable.ic_arrow_back, stringResource(R.string.back), onBack)
            Text(
                stringResource(R.string.nav_friends),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 14.dp),
            )
        }
        if (state.loading) {
            LoadingBox()
            return@Column
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(
                avatarInitial(state.nick),
                size = 88.dp,
                textStyle = MaterialTheme.typography.headlineLarge,
                dashedRing = true,
                color = avatarColor(friendUid),
            )
            Column(Modifier.padding(start = 18.dp)) {
                Text(state.nick, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
                since?.let {
                    Text(stringResource(R.string.friend_since, it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        val comparison = state.comparison ?: return@Column
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).framed().padding(vertical = 16.dp)) {
            Stat(comparison.count.toString(), pluralStringResource(R.plurals.profile_peaks, comparison.count))
            DashedDivider(vertical = true)
            Stat(
                value = comparison.highest?.let { stringResource(R.string.peaks_elevation, it.elevation) } ?: "–",
                label = comparison.highest?.let { stringResource(R.string.profile_highest_peak, it.name) } ?: stringResource(R.string.profile_highest),
                weight = 1.3f,
            )
            DashedDivider(vertical = true)
            Stat(comparison.common.toString(), stringResource(R.string.friend_common))
        }

        val friendKgp = comparison.friendKgp
        val myKgp = comparison.myKgp
        if (friendKgp != null && myKgp != null) KgpRace(state.nick, friendKgp, myKgp, comparison.kgpLead)

        SectionLabel(stringResource(R.string.friend_conquests, state.nick))
        if (comparison.conquests.isEmpty()) {
            Text(stringResource(R.string.friend_no_conquests, state.nick), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val shown = if (showAll) comparison.conquests else comparison.conquests.take(SHOWN_CONQUESTS)
            Column(Modifier.fillMaxWidth().framed()) {
                shown.forEachIndexed { index, row ->
                    if (index > 0) DashedDivider(Modifier.padding(horizontal = 16.dp))
                    ConquestRow(row, pl)
                }
            }
            if (!showAll && comparison.conquests.size > SHOWN_CONQUESTS) {
                Text(
                    text = stringResource(R.string.friend_show_all, comparison.conquests.size),
                    style = MaterialTheme.typography.labelLarge.copy(textDecoration = TextDecoration.Underline),
                    color = SzczytomaniaTheme.colors.conquered,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clickable { showAll = true }.padding(8.dp),
                )
            }
        }

        if (state.friendship != null) {
            Text(
                text = stringResource(R.string.friend_remove),
                style = MaterialTheme.typography.labelLarge,
                color = SzczytomaniaTheme.colors.trail,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable { confirmRemove = true }.padding(12.dp),
            )
        }
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text(stringResource(R.string.friend_remove_title, state.nick)) },
            text = { Text(stringResource(R.string.friend_remove_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmRemove = false
                    viewModel.remove()
                    onBack()
                }) { Text(stringResource(R.string.friend_remove_confirm), color = SzczytomaniaTheme.colors.trail) }
            },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun RowScope.Stat(value: String, label: String, weight: Float = 1f) {
    Column(Modifier.weight(weight).padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = NumberStyles.counter.copy(fontSize = NumberStyles.big.fontSize * 0.75f), color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

/** Wyścig w Koronie Gór Polski: kto prowadzi i dwa paski – znajomego i Twój. */
@Composable
private fun KgpRace(nick: String, friend: SetProgress, me: SetProgress, lead: Int) {
    Column(Modifier.fillMaxWidth().framed().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                friend.set.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            val leader = when {
                lead > 0 -> stringResource(R.string.friend_kgp_friend_leads, nick, lead)
                lead < 0 -> stringResource(R.string.friend_kgp_you_lead, -lead)
                else -> stringResource(R.string.friend_kgp_tie)
            }
            Text(leader, style = MaterialTheme.typography.labelLarge, color = SzczytomaniaTheme.colors.trail)
        }
        RaceBar(nick, friend, SzczytomaniaTheme.colors.conquered)
        RaceBar(stringResource(R.string.friend_you), me, MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * Pasek wyścigu z animacją: po wejściu na profil rośnie od zera do wyniku (animateFloatAsState).
 * Pierwsza klatka ma cel 0, a zaraz potem LaunchedEffect ustawia [started] – Compose sam liczy
 * pośrednie wartości przez 0,9 s i za każdym razem rysuje pasek od nowa.
 */
@Composable
private fun RaceBar(label: String, progress: SetProgress, color: Color) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val fraction by animateFloatAsState(
        targetValue = if (started) progress.fraction else 0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "kgp-$label",
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(88.dp),
        )
        Box(
            Modifier
                .weight(1f)
                .height(14.dp)
                .clip(RoundedCornerShape(50))
                .background(SzczytomaniaTheme.colors.lines.copy(alpha = 0.5f)),
        ) {
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(50)).background(color))
        }
        Text(
            "${progress.done}/${progress.total}",
            style = NumberStyles.counter.copy(fontSize = NumberStyles.elevation.fontSize),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.width(64.dp),
        )
    }
}

/** Zdobycz znajomego: miniatura, nazwa, wysokość i pasmo, data i "Ty też". */
@Composable
private fun ConquestRow(row: FriendConquestRow, pl: Locale) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        MountainThumb(row.peak.id)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(row.peak.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${stringResource(R.string.peaks_elevation, row.peak.elevation)} · ${row.peak.range}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(remember(row.date) { SimpleDateFormat("dd.MM", pl).format(row.date) }, style = monoSmall, color = MaterialTheme.colorScheme.onSurface)
            if (row.youToo) Tag(stringResource(R.string.friend_you_too), SzczytomaniaTheme.colors.conquered)
        }
    }
}
