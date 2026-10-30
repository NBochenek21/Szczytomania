package pl.szczytomania.ui.screens.peaks

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.szczytomania.R
import pl.szczytomania.data.model.Peak

/** Ekran "Wszystkie szczyty" (mockup 05_Wszystkie_szczyty): lista szczytów z wyszukiwarką. */
@Composable
fun PeaksScreen(
    onMenuClick: () -> Unit = {},
    viewModel: PeaksViewModel = viewModel(factory = PeaksViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PeaksContent(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onMenuClick = onMenuClick,
        // TODO: otwieranie karty szczytu (tydzień 5–6).
        onPeakClick = {},
    )
}

@Composable
fun PeaksContent(
    state: PeaksUiState,
    onQueryChange: (String) -> Unit,
    onMenuClick: () -> Unit,
    onPeakClick: (Peak) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier.fillMaxSize().systemBarsPadding().imePadding()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_menu),
                        contentDescription = stringResource(R.string.menu_open),
                    )
                }
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(stringResource(R.string.peaks_title), style = MaterialTheme.typography.headlineMedium)
                    Text(
                        stringResource(R.string.peaks_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.peaks_search_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        HorizontalDivider()

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.loadError -> CenteredMessage(stringResource(R.string.peaks_load_error))
            state.peaks.isEmpty() -> CenteredMessage(stringResource(R.string.peaks_no_results, state.query.trim()))
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.peaks, key = { it.id }) { peak ->
                    PeakRow(peak = peak, onClick = { onPeakClick(peak) })
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PeakRow(peak: Peak, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(peak.name, style = MaterialTheme.typography.titleMedium)
                if (peak.kgp) KgpBadge(Modifier.padding(start = 8.dp))
            }
            Text(
                peak.range,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            stringResource(R.string.peaks_elevation, peak.elevation),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun KgpBadge(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.error
    Text(
        text = stringResource(R.string.peaks_kgp_badge),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier
            .border(1.dp, color, RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp),
    )
}

@Composable
private fun CenteredMessage(text: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(showBackground = true)
@Composable
private fun PeaksContentPreview() {
    MaterialTheme {
        PeaksContent(
            state = PeaksUiState(
                isLoading = false,
                peaks = listOf(
                    Peak("node/4366169405", "Rysy", 2499, 49.1795756, 20.0881081, "Tatry", kgp = true),
                    Peak("node/1", "Pilsko", 1557, 49.54, 19.32, "Beskid Żywiecki"),
                    Peak("node/26863429", "Śnieżnik", 1423, 50.2074081, 16.847424, "Masyw Śnieżnika", kgp = true),
                ),
            ),
            onQueryChange = {},
            onMenuClick = {},
            onPeakClick = {},
        )
    }
}
