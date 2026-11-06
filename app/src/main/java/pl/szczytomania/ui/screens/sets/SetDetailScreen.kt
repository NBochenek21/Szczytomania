package pl.szczytomania.ui.screens.sets

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.szczytomania.R
import pl.szczytomania.ui.components.CenteredMessage
import pl.szczytomania.ui.components.DashedDivider
import pl.szczytomania.ui.components.LoadingBox
import pl.szczytomania.ui.components.PeakRow
import pl.szczytomania.ui.components.PillButton
import pl.szczytomania.ui.components.ProgressBar
import pl.szczytomania.ui.components.ScreenHeader
import pl.szczytomania.ui.components.SegmentedProgress
import pl.szczytomania.ui.components.framed
import pl.szczytomania.ui.theme.NumberStyles

// Przy większej liczbie szczytów kwadraciki byłyby za wąskie – wtedy zwykły pasek.
private const val MAX_SEGMENTS = 40

/** Szczegóły gotowego zestawu (mockup 02_Szczegoly_zestawu): postęp, filtry i lista szczytów. */
@Composable
fun SetDetailScreen(
    setId: String,
    onBack: () -> Unit,
    onPeakClick: (String) -> Unit,
    viewModel: SetsViewModel = viewModel(factory = SetsViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val progress = state.set(setId)
    var filter by rememberSaveable { mutableStateOf(SetFilter.ALL) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding()) {
        ScreenHeader(
            title = progress?.set?.name.orEmpty(),
            overline = stringResource(R.string.set_detail_overline),
            navIcon = R.drawable.ic_arrow_back,
            navDescription = stringResource(R.string.back),
            onNavClick = onBack,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )
        when {
            progress != null -> SetDetailContent(progress, filter, { filter = it }, onPeakClick)
            state.isLoading -> LoadingBox()
            else -> CenteredMessage(stringResource(R.string.sets_load_error))
        }
    }
}

@Composable
private fun SetDetailContent(progress: SetProgress, filter: SetFilter, onFilter: (SetFilter) -> Unit, onPeakClick: (String) -> Unit) {
    Column {
        SummaryCard(progress)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SetFilter.entries.forEach { f ->
                PillButton(stringResource(f.label), selected = f == filter, onClick = { onFilter(f) }, count = progress.peaks.count(f::matches))
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // Numer to miejsce szczytu w całym zestawie – nie zmienia się po filtrowaniu.
        val visible = progress.peaks.withIndex().filter { filter.matches(it.value) }
        if (visible.isEmpty()) {
            CenteredMessage(stringResource(R.string.set_filter_empty))
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(visible, key = { it.value.peak.id }) { (index, item) ->
                    PeakRow(
                        peak = item.peak,
                        conquered = item.conquered,
                        number = index + 1,
                        showKgpBadge = false,
                        showToConquer = true,
                        onClick = { onPeakClick(item.peak.id) },
                    )
                    DashedDivider(Modifier.padding(horizontal = 20.dp))
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(progress: SetProgress) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().framed().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(progress.done.toString(), style = NumberStyles.big, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = "/${progress.total}",
                style = NumberStyles.counter,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            Text(
                text = stringResource(R.string.set_detail_summary, progress.percent),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
            )
        }
        if (progress.total <= MAX_SEGMENTS) SegmentedProgress(progress.peaks.map { it.conquered }) else ProgressBar(progress.fraction)
    }
}
