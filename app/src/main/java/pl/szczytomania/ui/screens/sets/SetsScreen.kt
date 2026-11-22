package pl.szczytomania.ui.screens.sets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.szczytomania.R
import pl.szczytomania.ui.components.CenteredMessage
import pl.szczytomania.ui.components.LoadingBox
import pl.szczytomania.ui.components.ProgressBar
import pl.szczytomania.ui.components.ScreenHeader
import pl.szczytomania.ui.components.SegmentedProgress
import pl.szczytomania.ui.components.SegmentedTabs
import pl.szczytomania.ui.components.Tag
import pl.szczytomania.ui.components.framed
import pl.szczytomania.ui.theme.NumberStyles
import pl.szczytomania.ui.theme.SzczytomaniaTheme

/** Ekran "Zestawy gór" (mockup 01_Zestawy_gor_gotowe): zakładki Gotowe / Własne i postęp. */
@Composable
fun SetsScreen(
    onMenuClick: () -> Unit,
    onSetClick: (String) -> Unit,
    onCreateSet: () -> Unit,
    viewModel: SetsViewModel = viewModel(factory = SetsViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding()) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ScreenHeader(
                title = stringResource(R.string.nav_sets),
                navIcon = R.drawable.ic_menu,
                navDescription = stringResource(R.string.menu_open),
                onNavClick = onMenuClick,
            )
            SegmentedTabs(
                labels = listOf(stringResource(R.string.sets_tab_ready), stringResource(R.string.sets_tab_own, state.own.size)),
                selectedIndex = tab,
                onSelect = { tab = it },
            )
        }

        when {
            state.isLoading -> LoadingBox()
            state.loadError -> CenteredMessage(stringResource(R.string.sets_load_error))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (tab == 0) {
                    items(state.sets, key = { it.set.id }) { SetCard(it, onClick = { onSetClick(it.set.id) }) }
                } else {
                    items(state.own, key = { it.set.id }) {
                        SetCard(it, onClick = { onSetClick(it.set.id) }, own = true, sharingLine = sharingLine(state, it.set.id))
                    }
                    item { CreateSetButton(onCreateSet) }
                    if (state.sharedWithMe.isNotEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.sets_shared_with_me).uppercase(),
                                style = NumberStyles.tag,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        items(state.sharedWithMe, key = { it.set.id }) {
                            SetCard(it, onClick = { onSetClick(it.set.id) }, own = true, sharingLine = sharingLine(state, it.set.id))
                        }
                    }
                    if (state.own.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.sets_own_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Karta zestawu. Zestaw z plakietką (Korona Gór Polski) jest wyróżniony: opis i kwadraciki postępu. */
@Composable
private fun SetCard(progress: SetProgress, onClick: () -> Unit, own: Boolean = false, sharingLine: String? = null) {
    val featured = progress.set.badge != null
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .framed(borderWidth = if (featured) 2.dp else 1.dp)
            .clickable(onClick = onClick)
            .padding(18.dp),
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
            Text(progress.set.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SegmentedProgress(progress.peaks.map { it.conquered })
        } else {
            ProgressBar(progress.fraction)
        }
        if (own) {
            val (names, more) = progress.previewNames()
            Text(
                text = if (more > 0) stringResource(R.string.sets_own_preview_more, names, more) else names,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (sharingLine != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_person), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    sharingLine,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

/** "Udostępniony: Jakub, Ola" na własnym zestawie albo "Od: Jakub" na zestawie od znajomego. */
@Composable
internal fun sharingLine(state: SetsUiState, setId: String): String? {
    val sharing = state.sharing[setId] ?: return null
    val friendName = stringResource(R.string.sets_friend_unknown)
    return if (sharing.mine) {
        sharing.sharedWith.takeIf { it.isNotEmpty() }
            ?.joinToString(", ") { state.nick(it) ?: friendName }
            ?.let { stringResource(R.string.sets_shared_with, it) }
    } else {
        stringResource(R.string.sets_shared_from, state.nick(sharing.ownerId) ?: friendName)
    }
}

/** "+ Utwórz własny zestaw" – przerywana ramka jak na mockupie 03_Zestawy_gor_wlasne. */
@Composable
private fun CreateSetButton(onClick: () -> Unit) {
    val dash = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .drawBehind {
                drawRoundRect(
                    color = dash,
                    cornerRadius = CornerRadius(18.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
                )
            }
            .clickable(onClick = onClick)
            .padding(vertical = 22.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("+  " + stringResource(R.string.sets_create), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
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
