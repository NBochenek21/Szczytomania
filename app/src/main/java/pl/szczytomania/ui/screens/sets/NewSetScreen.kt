package pl.szczytomania.ui.screens.sets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.szczytomania.R
import pl.szczytomania.ui.components.DashedDivider
import pl.szczytomania.ui.components.ScreenHeader
import pl.szczytomania.ui.components.framed
import pl.szczytomania.ui.screens.peaks.PeakSearch
import pl.szczytomania.ui.theme.NumberStyles
import pl.szczytomania.ui.theme.SzczytomaniaTheme

/** Tworzenie własnego zestawu (mockup 04_Nowy_zestaw): nazwa, wybór szczytów z wyszukiwarką, zapis. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewSetScreen(
    onClose: () -> Unit,
    onSaved: () -> Unit,
    viewModel: SetsViewModel = viewModel(factory = SetsViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    // Kolejność wyboru – w takiej kolejności pokazujemy "pigułki".
    var selected by rememberSaveable { mutableStateOf(listOf<String>()) }
    val peaksById = remember(state.allPeaks) { state.allPeaks.associateBy { it.id } }
    val results = remember(state.allPeaks, query) { PeakSearch.filter(state.allPeaks, query) }
    val green = SzczytomaniaTheme.colors.conquered
    val fieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedBorderColor = green,
    )
    val toggle = { id: String -> selected = if (id in selected) selected - id else selected + id }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(
            title = stringResource(R.string.new_set_title),
            navIcon = R.drawable.ic_close,
            navDescription = stringResource(R.string.close),
            onNavClick = onClose,
        )
        Label(stringResource(R.string.new_set_name))
        OutlinedTextField(
            value = name,
            onValueChange = { if (it.length <= 40) name = it },
            placeholder = { Text(stringResource(R.string.new_set_name_placeholder)) },
            textStyle = MaterialTheme.typography.titleMedium,
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth(),
        )
        Label(pluralStringResource(R.plurals.new_set_selected, selected.size, selected.size))
        if (selected.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                selected.forEach { id ->
                    Text(
                        text = "${peaksById[id]?.name ?: id}  ✕",
                        style = MaterialTheme.typography.labelLarge,
                        color = green,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(1.dp, green, RoundedCornerShape(50))
                            .clickable { toggle(id) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.new_set_search), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth(),
        )
        LazyColumn(Modifier.weight(1f).fillMaxWidth().framed()) {
            items(results, key = { it.id }) { peak ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { toggle(peak.id) }.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = peak.id in selected,
                        onCheckedChange = { toggle(peak.id) },
                        colors = CheckboxDefaults.colors(checkedColor = green),
                    )
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(peak.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(peak.range, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(stringResource(R.string.peaks_elevation, peak.elevation), style = NumberStyles.elevation, color = MaterialTheme.colorScheme.onSurface)
                }
                DashedDivider(Modifier.padding(horizontal = 12.dp))
            }
        }
        val valid = isValidSetName(name) && selected.isNotEmpty()
        Button(
            onClick = {
                viewModel.createOwnSet(name, selected.toSet())
                onSaved()
            },
            enabled = valid,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = green),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text(
                text = if (valid) pluralStringResource(R.plurals.new_set_save, selected.size, selected.size) else stringResource(R.string.new_set_save_empty),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
