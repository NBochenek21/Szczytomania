package pl.szczytomania.ui.screens.conquest

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.szczytomania.R
import pl.szczytomania.location.LocationAccess
import pl.szczytomania.location.SummitCheckResult
import pl.szczytomania.location.rememberLocationPermissionState
import pl.szczytomania.ui.components.LoadingBox
import pl.szczytomania.ui.components.PeakStamp
import pl.szczytomania.ui.components.ProgressBar
import pl.szczytomania.ui.components.ScreenHeader
import pl.szczytomania.ui.components.SegmentedProgress
import pl.szczytomania.ui.components.SquareIconButton
import pl.szczytomania.ui.components.framed
import pl.szczytomania.ui.screens.map.SummitCheckState
import pl.szczytomania.ui.screens.map.SummitCheckStatus
import pl.szczytomania.ui.screens.sets.Counter
import pl.szczytomania.ui.theme.SzczytomaniaTheme
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Zdobywanie szczytu (mockupy 04 i 07): kroki Lokalizacja → Zdjęcie → Zapis, wynik GPS,
 * a po zapisie ekran "Szczyt zdobyty!" (mockup 06).
 */
@Composable
fun ConquestScreen(viewModel: ConquestViewModel, onClose: () -> Unit, onMyConquests: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permission = rememberLocationPermissionState()
    val peak = state.peak
    val summary = state.summary

    when {
        summary != null -> ConquestSuccess(summary, onClose, onMyConquests)
        peak == null -> LoadingBox()
        else -> Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ScreenHeader(
                title = stringResource(R.string.conquest_title),
                subtitle = stringResource(R.string.conquest_subtitle, peak.name, peak.elevation),
                navIcon = R.drawable.ic_arrow_back,
                navDescription = stringResource(R.string.back),
                onNavClick = onClose,
            )
            val done = state.check as? SummitCheckState.Done
            Steps(locationOk = if (done == null) null else state.passed)

            Box(Modifier.fillMaxWidth().framed().padding(20.dp)) { SummitCheckStatus(state.check) }

            if (state.passed) {
                // Miejsce na zdjęcie z aparatu (Kacper, tydzień 4) – na razie zapis bez zdjęcia.
                Box(
                    Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(18.dp)).background(Color(0xFF2C3A42)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.conquest_photo_soon),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp),
                    )
                }
                GreenButton(stringResource(R.string.conquest_save), R.drawable.ic_flag, viewModel::save)
            } else if (done != null) {
                Text(
                    text = stringResource(R.string.conquest_locked),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onClose, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f).height(56.dp)) {
                        Text(stringResource(R.string.conquest_back_to_map), style = MaterialTheme.typography.titleMedium)
                    }
                    Box(Modifier.weight(1f)) {
                        GreenButton(stringResource(R.string.summit_check_again), null) {
                            // Bez dokładnej lokalizacji najpierw prosimy o zgodę, potem sprawdzamy.
                            if (done.result is SummitCheckResult.NoPreciseLocation && permission.access != LocationAccess.PRECISE) {
                                permission.request()
                            } else {
                                viewModel.check()
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Kroki z mockupu: Lokalizacja (✓ / ✕ / w toku), Zdjęcie, Zapis. */
@Composable
private fun Steps(locationOk: Boolean?) {
    val colors = SzczytomaniaTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(R.string.conquest_step_location, R.string.conquest_step_photo, R.string.conquest_step_save).forEachIndexed { i, label ->
            val fill = when {
                i == 0 && locationOk == true -> colors.conquered
                i == 0 && locationOk == false -> colors.trail
                else -> null
            }
            val active = fill != null || (i == 1 && locationOk == true)
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(fill ?: Color.Transparent)
                    .border(1.5.dp, fill ?: if (active) MaterialTheme.colorScheme.outline else colors.lines, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when (fill) { null -> "${i + 1}"; colors.conquered -> "✓"; else -> "✕" },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (fill != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.labelLarge,
                color = fill ?: if (active) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (i < 2) Spacer(Modifier.width(4.dp))
        }
    }
}

@Composable
private fun GreenButton(text: String, icon: Int?, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SzczytomaniaTheme.colors.conquered),
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) {
        if (icon != null) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
    }
}

/** "Szczyt zdobyty!" (mockup 06): pieczątka, który to szczyt, zapis w telefonie, postęp w zestawie. */
@Composable
private fun ConquestSuccess(summary: ConquestSummary, onClose: () -> Unit, onMyConquests: () -> Unit) {
    val peak = summary.peak
    val date = remember(summary.date) { SimpleDateFormat("dd.MM.yyyy", Locale.forLanguageTag("pl")).format(summary.date) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        SquareIconButton(R.drawable.ic_close, stringResource(R.string.close), onClose, Modifier.align(Alignment.End))
        PeakStamp(
            name = peak.name,
            date = date,
            elevation = stringResource(R.string.peak_card_elevation, peak.elevation),
            color = SzczytomaniaTheme.colors.trail,
            size = 220.dp,
            ringText = stringResource(if (peak.kgp) R.string.stamp_ring_kgp else R.string.stamp_ring),
        )
        Text(stringResource(R.string.success_title), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground)
        Text(
            text = stringResource(if (summary.isRecord) R.string.success_number_record else R.string.success_number, summary.number),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Column(Modifier.fillMaxWidth().framed().padding(18.dp)) {
            Text(stringResource(R.string.success_saved_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(stringResource(R.string.success_saved_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        summary.set?.let { set ->
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(set.set.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Counter(done = set.done, total = set.total)
                }
                if (set.total <= 40) SegmentedProgress(set.peaks.map { it.conquered }, highlight = summary.indexInSet) else ProgressBar(set.fraction)
                val left = set.total - set.done
                Text(
                    text = if (left == 0) stringResource(R.string.success_set_complete) else pluralStringResource(R.plurals.success_remaining, left, left),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        GreenButton(stringResource(R.string.conquest_back_to_map), null, onClose)
        OutlinedButton(onClick = onMyConquests, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(stringResource(R.string.nav_my_conquests), style = MaterialTheme.typography.titleMedium)
        }
    }
}
