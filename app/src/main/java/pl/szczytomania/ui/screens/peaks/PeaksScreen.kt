package pl.szczytomania.ui.screens.peaks

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.szczytomania.R
import pl.szczytomania.data.model.Peak
import pl.szczytomania.ui.components.DashedDivider
import pl.szczytomania.ui.components.PeakRow
import pl.szczytomania.ui.components.PillButton
import pl.szczytomania.ui.components.ScreenHeader
import pl.szczytomania.ui.theme.SzczytomaniaTheme

/** Ekran "Wszystkie szczyty" (mockup 05_Wszystkie_szczyty): wyszukiwarka, sortowanie i lista. */
@Composable
fun PeaksScreen(
    onMenuClick: () -> Unit = {},
    onPeakClick: (String) -> Unit = {},
    viewModel: PeaksViewModel = viewModel(factory = PeaksViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PeaksContent(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onSortChange = viewModel::onSortChange,
        onMenuClick = onMenuClick,
        onPeakClick = { onPeakClick(it.id) },
    )
}

@Composable
fun PeaksContent(
    state: PeaksUiState,
    onQueryChange: (String) -> Unit,
    onSortChange: (PeakSort) -> Unit,
    onMenuClick: () -> Unit,
    onPeakClick: (Peak) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            ScreenHeader(
                title = stringResource(R.string.peaks_title),
                subtitle = stringResource(R.string.peaks_subtitle),
                navIcon = R.drawable.ic_menu,
                navDescription = stringResource(R.string.menu_open),
                onNavClick = onMenuClick,
            )
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.peaks_search_placeholder)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedBorderColor = SzczytomaniaTheme.colors.conquered,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.peaks_sort_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SortButton(R.string.peaks_sort_elevation, PeakSort.ELEVATION, state.sort, onSortChange)
                SortButton(R.string.peaks_sort_name, PeakSort.NAME, state.sort, onSortChange)
                SortButton(R.string.peaks_sort_range, PeakSort.RANGE, state.sort, onSortChange)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.loadError -> CenteredMessage(stringResource(R.string.peaks_load_error))
            state.peaks.isEmpty() -> CenteredMessage(stringResource(R.string.peaks_no_results, state.query.trim()))
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.peaks, key = { it.id }) { peak ->
                    PeakRow(
                        peak = peak,
                        conquered = peak.id in state.conqueredIds,
                        onClick = { onPeakClick(peak) },
                    )
                    DashedDivider(Modifier.padding(horizontal = 20.dp))
                }
            }
        }
    }
}

@Composable
private fun SortButton(label: Int, sort: PeakSort, current: PeakSort, onSortChange: (PeakSort) -> Unit) {
    PillButton(text = stringResource(label), selected = sort == current, onClick = { onSortChange(sort) })
}

@Composable
private fun CenteredMessage(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PeaksContentPreview() {
    SzczytomaniaTheme {
        PeaksContent(
            state = PeaksUiState(
                isLoading = false,
                peaks = listOf(
                    Peak("node/4366169405", "Rysy", 2499, 49.1795756, 20.0881081, "Tatry", kgp = true),
                    Peak("node/1", "Pilsko", 1557, 49.54, 19.32, "Beskid Żywiecki"),
                    Peak("node/26863429", "Śnieżnik", 1423, 50.2074081, 16.847424, "Masyw Śnieżnika", kgp = true),
                ),
                conqueredIds = setOf("node/1"),
            ),
            onQueryChange = {},
            onSortChange = {},
            onMenuClick = {},
            onPeakClick = {},
        )
    }
}
