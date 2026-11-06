package pl.szczytomania.ui.screens.peaks

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.szczytomania.R
import pl.szczytomania.ui.components.CenteredMessage
import pl.szczytomania.ui.components.DashedDivider
import pl.szczytomania.ui.components.LoadingBox
import pl.szczytomania.ui.components.PeakRow
import pl.szczytomania.ui.components.PillButton
import pl.szczytomania.ui.components.ScreenHeader
import pl.szczytomania.ui.theme.SzczytomaniaTheme

/** Ekran "Wszystkie szczyty" (mockup 05_Wszystkie_szczyty): wyszukiwarka, sortowanie i lista. */
@Composable
fun PeaksScreen(
    onMenuClick: () -> Unit,
    onPeakClick: (String) -> Unit,
    viewModel: PeaksViewModel = viewModel(factory = PeaksViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
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
                onValueChange = viewModel::onQueryChange,
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
                PeakSort.entries.forEach { sort ->
                    PillButton(stringResource(sort.label), selected = sort == state.sort, onClick = { viewModel.onSortChange(sort) })
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        when {
            state.isLoading -> LoadingBox()
            state.loadError -> CenteredMessage(stringResource(R.string.peaks_load_error))
            state.peaks.isEmpty() -> CenteredMessage(stringResource(R.string.peaks_no_results, state.query.trim()))
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.peaks, key = { it.id }) { peak ->
                    PeakRow(peak = peak, conquered = peak.id in state.conqueredIds, onClick = { onPeakClick(peak.id) })
                    DashedDivider(Modifier.padding(horizontal = 20.dp))
                }
            }
        }
    }
}
