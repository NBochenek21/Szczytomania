package pl.szczytomania.ui.screens.sets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.szczytomania.R
import pl.szczytomania.data.model.Peak
import pl.szczytomania.data.model.PeakSet
import pl.szczytomania.ui.components.ProgressBar
import pl.szczytomania.ui.components.ScreenHeader
import pl.szczytomania.ui.components.SegmentedProgress
import pl.szczytomania.ui.components.SegmentedTabs
import pl.szczytomania.ui.components.Tag
import pl.szczytomania.ui.theme.NumberStyles
import pl.szczytomania.ui.theme.SzczytomaniaTheme

/** Ekran "Zestawy gór" (mockup 01_Zestawy_gor_gotowe): zakładki Gotowe / Własne i postęp. */
@Composable
fun SetsScreen(
    onMenuClick: () -> Unit,
    onSetClick: (String) -> Unit,
    viewModel: SetsViewModel = viewModel(factory = SetsViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SetsContent(state = state, onMenuClick = onMenuClick, onSetClick = onSetClick)
}

@Composable
fun SetsContent(
    state: SetsUiState,
    onMenuClick: () -> Unit,
    onSetClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ScreenHeader(
                title = stringResource(R.string.sets_title),
                navIcon = R.drawable.ic_menu,
                navDescription = stringResource(R.string.menu_open),
                onNavClick = onMenuClick,
            )
            SegmentedTabs(
                // Własne zestawy dochodzą w tygodniu 5 – na razie zawsze 0.
                labels = listOf(stringResource(R.string.sets_tab_ready), stringResource(R.string.sets_tab_own, 0)),
                selectedIndex = tab,
                onSelect = { tab = it },
            )
        }

        when {
            tab == 1 -> CenteredMessage(stringResource(R.string.sets_own_empty))
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.loadError -> CenteredMessage(stringResource(R.string.sets_load_error))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(state.sets, key = { it.set.id }) { progress ->
                    SetCard(progress = progress, onClick = { onSetClick(progress.set.id) })
                }
            }
        }
    }
}

/** Karta zestawu. Zestaw z plakietką (Korona Gór Polski) jest wyróżniony: opis i kwadraciki postępu. */
@Composable
private fun SetCard(progress: SetProgress, onClick: () -> Unit) {
    val featured = progress.set.badge != null
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(if (featured) 2.dp else 1.dp, MaterialTheme.colorScheme.outline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        progress.set.badge?.let { Tag(it, SzczytomaniaTheme.colors.trail) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = progress.set.name,
                style = if (featured) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Counter(done = progress.done, total = progress.total, modifier = Modifier.padding(start = 12.dp))
        }
        if (featured) {
            Text(
                text = progress.set.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SegmentedProgress(progress.peaks.map { it.conquered })
        } else {
            ProgressBar(progress.fraction)
        }
    }
}

/** "7/28": zdobyte w kolorze tekstu, liczba wszystkich jaśniej. */
@Composable
internal fun Counter(done: Int, total: Int, modifier: Modifier = Modifier) {
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = buildAnnotatedString {
            append(done.toString())
            withStyle(SpanStyle(color = secondary)) { append("/$total") }
        },
        style = NumberStyles.counter,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

@Composable
internal fun CenteredMessage(text: String) {
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
private fun SetsContentPreview() {
    val peaks = listOf(
        Peak("node/1", "Rysy", 2499, 49.18, 20.09, "Tatry", kgp = true),
        Peak("node/2", "Babia Góra", 1723, 49.57, 19.53, "Beskid Żywiecki", kgp = true),
        Peak("node/3", "Śnieżka", 1603, 50.74, 15.74, "Karkonosze", kgp = true),
    ).associateBy { it.id }
    val kgp = PeakSet("kgp", "Korona Gór Polski", "Najwyższe szczyty 28 pasm górskich Polski.", peaks.keys.toList(), "Odznaka klasyczna")
    val tatry = PeakSet("tatry", "Tatry Polskie", "Tatry", listOf("node/1"))
    SzczytomaniaTheme {
        SetsContent(
            state = SetsUiState(
                isLoading = false,
                sets = listOf(SetProgress.of(kgp, peaks, setOf("node/3")), SetProgress.of(tatry, peaks, emptySet())),
            ),
            onMenuClick = {},
            onSetClick = {},
        )
    }
}
