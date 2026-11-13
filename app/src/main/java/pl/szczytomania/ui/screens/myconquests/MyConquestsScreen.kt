package pl.szczytomania.ui.screens.myconquests

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import pl.szczytomania.R
import pl.szczytomania.camera.ConquestPhotos
import pl.szczytomania.data.firestore.ConquestRepository
import pl.szczytomania.data.repository.ConqueredPeaks
import pl.szczytomania.data.repository.PeakRepository
import pl.szczytomania.ui.components.CenteredMessage
import pl.szczytomania.ui.components.DashedDivider
import pl.szczytomania.ui.components.PeakStamp
import pl.szczytomania.ui.components.ScreenHeader
import pl.szczytomania.ui.components.framed
import pl.szczytomania.ui.screens.conquest.ConquestEntry
import pl.szczytomania.ui.screens.conquest.groupByMonth
import pl.szczytomania.ui.theme.NumberStyles
import pl.szczytomania.ui.theme.SzczytomaniaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Zdobycze użytkownika ze szczytami, od najnowszych, pogrupowane po miesiącach. null = wczytywanie. */
class MyConquestsViewModel(repository: PeakRepository) : ViewModel() {
    val months: StateFlow<List<Pair<Date, List<ConquestEntry>>>?> =
        combine(flow { emit(repository.getAll().associateBy { it.id }) }, ConqueredPeaks.all) { peaks, conquests ->
            groupByMonth(conquests.mapNotNull { c -> peaks[c.peakId]?.let { ConquestEntry(it, c) } })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        val Factory = viewModelFactory {
            initializer { MyConquestsViewModel(PeakRepository.get(checkNotNull(this[APPLICATION_KEY]).assets)) }
        }
    }
}

private val pl = Locale.forLanguageTag("pl")

/** "Moje zdobycze" (mockupy 06 i 07): książeczka zdobywcy z pieczątkami, dotknięcie pokazuje szczegóły. */
@Composable
fun MyConquestsScreen(onMenuClick: () -> Unit, viewModel: MyConquestsViewModel = viewModel(factory = MyConquestsViewModel.Factory)) {
    val months by viewModel.months.collectAsStateWithLifecycle()
    var open by remember { mutableStateOf<ConquestEntry?>(null) }
    val count = months?.sumOf { it.second.size } ?: 0
    // Pieczątki na zmianę: zieleń, czerwień szlaku, niebieski – jak w mockupie.
    val stampColors = listOf(SzczytomaniaTheme.colors.conquered, SzczytomaniaTheme.colors.trail, MaterialTheme.colorScheme.secondary)

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding().padding(horizontal = 20.dp)) {
        ScreenHeader(
            title = stringResource(R.string.nav_my_conquests),
            subtitle = pluralStringResource(R.plurals.my_conquests_subtitle, count, count),
            navIcon = R.drawable.ic_menu,
            navDescription = stringResource(R.string.menu_open),
            onNavClick = onMenuClick,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        val groups = months
        if (groups != null && groups.isEmpty()) {
            CenteredMessage(stringResource(R.string.my_conquests_empty))
        } else {
            StampBook(groups.orEmpty(), stampColors, onOpen = { open = it })
        }
    }

    open?.let { entry -> ConquestDialog(entry, onDismiss = { open = null }) }
}

@Composable
private fun StampBook(groups: List<Pair<Date, List<ConquestEntry>>>, stampColors: List<Color>, onOpen: (ConquestEntry) -> Unit) {
    Column {
        Text(stringResource(R.string.my_conquests_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        BoxWithConstraints(Modifier.padding(vertical = 14.dp).fillMaxSize().framed().padding(horizontal = 16.dp)) {
            val stampSize = (maxWidth - 24.dp) / 4
            LazyColumn {
                items(groups, key = { it.first.time }) { (month, entries) ->
                    Text(
                        text = SimpleDateFormat("LLLL yyyy", pl).format(month).uppercase(pl),
                        style = NumberStyles.tag,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
                    )
                    DashedDivider()
                    entries.chunked(4).forEach { row ->
                        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { entry ->
                                PeakStamp(
                                    name = entry.peak.name,
                                    date = SimpleDateFormat("dd.MM", pl).format(entry.conquest.conqueredAt),
                                    color = stampColors[(entry.peak.id.hashCode() and 0x7fffffff) % stampColors.size],
                                    size = stampSize,
                                    modifier = Modifier.clip(CircleShape).clickable { onOpen(entry) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Szczegóły zdobyczy (mockup 07): miejsce na zdjęcie, nazwa, pasmo, data, status zdjęcia i dokładność GPS. */
@Composable
private fun ConquestDialog(entry: ConquestEntry, onDismiss: () -> Unit) {
    val peak = entry.peak
    val conquest = entry.conquest
    // Zdjęcie ze szczytu: z telefonu albo pobrane ze Storage. Do tego czasu – samo tło.
    val context = LocalContext.current
    val uploaded = conquest.photoStatus == ConquestRepository.PHOTO_UPLOADED
    val photo by produceState<Bitmap?>(null, conquest.peakId, uploaded) {
        value = ConquestPhotos(context).load(conquest.peakId, uploaded)
    }
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().framed(borderWidth = 1.5.dp).padding(14.dp)) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(6.dp)).background(Color(0xFF5E7A8C)),
                contentAlignment = Alignment.TopEnd,
            ) {
                photo?.let {
                    Image(
                        bitmap = remember(it) { it.asImageBitmap() },
                        contentDescription = peak.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                }
                Box(
                    Modifier.padding(10.dp).size(44.dp).clip(CircleShape).background(Color(0xCC2C3A42)).clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.close), tint = Color.White)
                }
            }
            Text(peak.name, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 14.dp))
            Text(
                text = stringResource(R.string.conquest_subtitle, peak.range, peak.elevation),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(SimpleDateFormat("dd.MM.yyyy, HH:mm", pl).format(conquest.conqueredAt), style = NumberStyles.elevation, color = MaterialTheme.colorScheme.onSurface)
            DashedDivider(Modifier.padding(vertical = 12.dp))
            val gps = conquest.accuracyMeters?.let { stringResource(R.string.my_conquests_gps, kotlin.math.ceil(it).toInt()) }
            val photoPending = conquest.photoStatus != ConquestRepository.PHOTO_UPLOADED
            Text(
                text = listOfNotNull(stringResource(R.string.my_conquests_photo_pending).takeIf { photoPending }, gps).joinToString(" · "),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
            )
        }
    }
}
