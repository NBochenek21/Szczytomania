package pl.szczytomania.ui.screens.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.szczytomania.R
import pl.szczytomania.map.MapStyle
import pl.szczytomania.map.offline.MapRegion
import pl.szczytomania.map.offline.PolandOutline
import pl.szczytomania.map.offline.RegionDownload
import pl.szczytomania.ui.components.DashedDivider
import pl.szczytomania.ui.components.ScreenHeader
import pl.szczytomania.ui.components.framed
import pl.szczytomania.ui.theme.NumberStyles
import pl.szczytomania.ui.theme.SzczytomaniaTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos

/** Ekran "Mapy offline" (mockup 12_Mapy_offline): pobieranie map regionów przed wyjściem w góry. */
@Composable
fun OfflineMapsScreen(
    onMenuClick: () -> Unit,
    viewModel: OfflineMapsViewModel = viewModel(factory = OfflineMapsViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Region czekający na zgodę na pobieranie przez dane komórkowe.
    var confirmMetered by remember { mutableStateOf<MapRegion?>(null) }

    val download: (MapRegion) -> Unit = { region ->
        if (viewModel.isOnMeteredNetwork) confirmMetered = region else viewModel.download(region)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ScreenHeader(
            title = stringResource(R.string.offline_title),
            subtitle = stringResource(R.string.offline_subtitle),
            navIcon = R.drawable.ic_menu,
            navDescription = stringResource(R.string.menu_open),
            onNavClick = onMenuClick,
        )

        RegionsOverview(state.regions)
        StorageUsage(usedBytes = state.usedBytes, freeBytes = state.freeBytes)

        Column(Modifier.fillMaxWidth().framed()) {
            state.regions.forEachIndexed { index, item ->
                if (index > 0) DashedDivider()
                RegionRow(item, onDownload = { download(item.region) }, onDelete = { viewModel.delete(item.region) })
            }
        }

        Text(
            text = stringResource(R.string.offline_footer, MapStyle.provider),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    confirmMetered?.let { region ->
        AlertDialog(
            onDismissRequest = { confirmMetered = null },
            title = { Text(stringResource(R.string.offline_metered_title)) },
            text = { Text(stringResource(R.string.offline_metered_text, region.name, megabytes(region.estimatedBytes))) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.download(region)
                    confirmMetered = null
                }) { Text(stringResource(R.string.offline_download)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmMetered = null }) { Text(stringResource(R.string.offline_cancel)) }
            },
        )
    }
}

/** Kontur Polski z kropkami regionów: pobrane, pobierane, dostępne. */
@Composable
private fun RegionsOverview(regions: List<RegionItem>) {
    val outline = MaterialTheme.colorScheme.outline
    val land = SzczytomaniaTheme.colors.lines.copy(alpha = 0.35f)
    val colors = Markers(
        done = SzczytomaniaTheme.colors.conquered,
        inProgress = MaterialTheme.colorScheme.secondary,
        available = MaterialTheme.colorScheme.surface,
        outline = outline,
    )
    Column(Modifier.fillMaxWidth().framed(borderWidth = 2.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(OVERVIEW_ASPECT)) {
            val path = Path()
            PolandOutline.points.forEachIndexed { index, (lon, lat) ->
                val point = project(lon, lat)
                if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
            }
            path.close()
            drawPath(path, land)
            drawPath(path, outline, style = Stroke(width = 1.5.dp.toPx()))
            regions.forEach { item -> drawMarker(project(item.region.centerLon, item.region.centerLat), item.download, colors) }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LegendItem(RegionDownload.Done(0, 0), colors, stringResource(R.string.offline_legend_done))
            LegendItem(RegionDownload.InProgress(0, 0f, active = true), colors, stringResource(R.string.offline_legend_in_progress))
            LegendItem(RegionDownload.None, colors, stringResource(R.string.offline_legend_available))
        }
    }
}

private class Markers(val done: Color, val inProgress: Color, val available: Color, val outline: Color)

@Composable
private fun LegendItem(download: RegionDownload, colors: Markers, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(Modifier.size(16.dp)) { drawMarker(center, download, colors) }
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun DrawScope.drawMarker(at: Offset, download: RegionDownload, colors: Markers) {
    val radius = 7.dp.toPx()
    val stroke = 1.5.dp.toPx()
    when (download) {
        is RegionDownload.Done -> {
            drawCircle(colors.done, radius, at)
            drawCircle(colors.outline, radius, at, style = Stroke(stroke))
        }
        is RegionDownload.InProgress -> drawCircle(
            colors.inProgress, radius, at,
            style = Stroke(stroke * 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx()))),
        )
        RegionDownload.None -> {
            drawCircle(colors.available, radius, at)
            drawCircle(colors.outline, radius, at, style = Stroke(stroke))
        }
    }
}

// Rzut równoodległościowy ze skróceniem długości o cos(52°) – Polska nie jest "rozciągnięta" w poziomie.
private const val MIN_LON = 14.0
private const val MAX_LON = 24.2
private const val MIN_LAT = 48.9
private const val MAX_LAT = 54.9
private val LON_SCALE = cos(Math.toRadians(52.0))
private val OVERVIEW_ASPECT = ((MAX_LON - MIN_LON) * LON_SCALE / (MAX_LAT - MIN_LAT)).toFloat()

private fun DrawScope.project(lon: Double, lat: Double) = Offset(
    x = ((lon - MIN_LON) / (MAX_LON - MIN_LON) * size.width).toFloat(),
    y = ((MAX_LAT - lat) / (MAX_LAT - MIN_LAT) * size.height).toFloat(),
)

@Composable
private fun StorageUsage(usedBytes: Long, freeBytes: Long) {
    val colors = SzczytomaniaTheme.colors
    val fraction = if (usedBytes > 0) (usedBytes.toFloat() / (usedBytes + freeBytes)).coerceAtLeast(0.02f) else 0f
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.offline_storage),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(stringResource(R.string.offline_megabytes, megabytes(usedBytes)), style = NumberStyles.elevation, color = MaterialTheme.colorScheme.onBackground)
        }
        Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(colors.lines.copy(alpha = 0.5f))) {
            Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(RoundedCornerShape(50)).background(colors.selected))
        }
    }
}

@Composable
private fun RegionRow(item: RegionItem, onDownload: () -> Unit, onDelete: () -> Unit) {
    val secondaryText = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (val download = item.download) {
            is RegionDownload.Done -> {
                Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = SzczytomaniaTheme.colors.conquered)
                RegionText(
                    name = item.region.name,
                    detail = stringResource(
                        R.string.offline_downloaded,
                        DATE_FORMAT.format(Date(download.downloadedAt)),
                        megabytes(download.bytes),
                    ),
                    color = secondaryText,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDelete) {
                    Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.offline_delete, item.region.name))
                }
            }
            is RegionDownload.InProgress -> Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RegionText(
                        name = item.region.name,
                        detail = when {
                            !download.active -> stringResource(R.string.offline_interrupted, (download.fraction * 100).toInt())
                            download.waitingForNetwork -> stringResource(R.string.offline_waiting_for_network, megabytes(download.completedBytes))
                            else -> stringResource(R.string.offline_progress, (download.fraction * 100).toInt(), megabytes(download.completedBytes))
                        },
                        color = if (download.active) MaterialTheme.colorScheme.secondary else secondaryText,
                        modifier = Modifier.weight(1f),
                    )
                    if (!download.active) DownloadButton(stringResource(R.string.offline_resume), onDownload)
                    IconButton(onClick = onDelete) {
                        Icon(
                            painter = painterResource(if (download.active) R.drawable.ic_close else R.drawable.ic_delete),
                            contentDescription = stringResource(R.string.offline_cancel_download, item.region.name),
                        )
                    }
                }
                DownloadProgress(download.fraction)
            }
            RegionDownload.None -> {
                RegionText(
                    name = item.region.name,
                    detail = stringResource(R.string.offline_estimate, megabytes(item.region.estimatedBytes)),
                    color = secondaryText,
                    modifier = Modifier.weight(1f),
                )
                DownloadButton(stringResource(R.string.offline_download), onDownload)
            }
        }
    }
}

@Composable
private fun RegionText(name: String, detail: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(detail, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

@Composable
private fun DownloadButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SzczytomaniaTheme.colors.conquered),
        modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
    ) {
        Icon(painterResource(R.drawable.ic_download), contentDescription = null, modifier = Modifier.size(20.dp))
        Text(text, modifier = Modifier.padding(start = 8.dp))
    }
}

/** Niebieski pasek pobierania – kolor lokalizacji, jak na mockupie. */
@Composable
private fun DownloadProgress(fraction: Float) {
    val colors = SzczytomaniaTheme.colors
    Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(colors.lines.copy(alpha = 0.5f))) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary),
        )
    }
}

private val DATE_FORMAT = SimpleDateFormat("d.MM", Locale.forLanguageTag("pl"))
