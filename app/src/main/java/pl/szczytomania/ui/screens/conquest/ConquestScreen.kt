package pl.szczytomania.ui.screens.conquest

import androidx.camera.core.ImageCapture
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.szczytomania.R
import pl.szczytomania.camera.CameraViewfinder
import pl.szczytomania.camera.rememberCameraPermissionState
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

    // Aparat z podglądem na żywo w ramce (CameraX) – migawka robi zdjęcie przez ten sam ImageCapture.
    val cameraPermission = rememberCameraPermissionState()
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    var cameraError by remember { mutableStateOf(false) }

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
            Steps(locationOk = if (done == null) null else state.passed, photoOk = state.photo != null)

            Box(Modifier.fillMaxWidth().framed().padding(20.dp)) { SummitCheckStatus(state.check) }

            if (state.passed) {
                // Zdjęcie ze szczytu (mockupy 04 i 05): podgląd z aparatu w ramce, migawka,
                // potem zrobione zdjęcie z pytaniem "Czy zdjęcie jest w porządku?".
                val photo = state.photo
                if (photo == null) {
                    // Przy wejściu w krok "Zdjęcie" od razu pytamy o aparat.
                    LaunchedEffect(Unit) {
                        if (!cameraPermission.granted && !cameraPermission.permanentlyDenied) cameraPermission.request()
                    }
                    PhotoFrame {
                        when {
                            !cameraPermission.granted -> FrameText(stringResource(R.string.conquest_camera_permission))
                            cameraError -> FrameText(stringResource(R.string.conquest_camera_error))
                            else -> {
                                CameraViewfinder(imageCapture, onError = { cameraError = true }, modifier = Modifier.matchParentSize())
                                ViewfinderCorners(Modifier.matchParentSize().padding(16.dp))
                                if (state.processingPhoto) {
                                    CircularProgressIndicator(color = Color.White)
                                } else {
                                    FrameText(stringResource(R.string.conquest_photo_hint), Modifier.align(Alignment.BottomCenter))
                                }
                            }
                        }
                    }
                    if (state.photoFailed) {
                        Text(stringResource(R.string.conquest_photo_failed), style = MaterialTheme.typography.bodyMedium, color = SzczytomaniaTheme.colors.trail)
                    }
                    if (cameraPermission.granted) {
                        ShutterButton(
                            enabled = !state.processingPhoto && !cameraError,
                            onClick = { viewModel.takePhoto(imageCapture) },
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                    } else {
                        GreenButton(
                            stringResource(if (cameraPermission.permanentlyDenied) R.string.conquest_camera_settings else R.string.conquest_camera_allow),
                            R.drawable.ic_camera,
                            cameraPermission::request,
                        )
                    }
                } else {
                    PhotoFrame(ratio = photo.width.toFloat() / photo.height) {
                        Image(
                            bitmap = remember(photo) { photo.asImageBitmap() },
                            contentDescription = stringResource(R.string.conquest_step_photo),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize(),
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.conquest_photo_question), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
                        Text(stringResource(R.string.conquest_photo_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = viewModel::retakePhoto,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).height(56.dp),
                        ) {
                            Icon(painterResource(R.drawable.ic_camera), contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.conquest_photo_retake), style = MaterialTheme.typography.titleMedium)
                        }
                        Box(Modifier.weight(1f)) {
                            GreenButton(stringResource(R.string.conquest_save), R.drawable.ic_flag, viewModel::save)
                        }
                    }
                }
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
private fun Steps(locationOk: Boolean?, photoOk: Boolean) {
    val colors = SzczytomaniaTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(R.string.conquest_step_location, R.string.conquest_step_photo, R.string.conquest_step_save).forEachIndexed { i, label ->
            val fill = when {
                i == 0 && locationOk == true -> colors.conquered
                i == 0 && locationOk == false -> colors.trail
                i == 1 && photoOk -> colors.conquered
                else -> null
            }
            val active = fill != null || (i == 1 && locationOk == true) || (i == 2 && photoOk)
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

/** Ramka zdjęcia (mockup 04): ciemne tło i zaokrąglone rogi – podgląd z aparatu albo zrobione zdjęcie. */
@Composable
private fun PhotoFrame(ratio: Float = 4f / 3f, content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier.fillMaxWidth().aspectRatio(ratio).clip(RoundedCornerShape(18.dp)).background(Color(0xFF2C3A42)),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** Biały napis na ramce – z cieniem, żeby był czytelny także na jasnym obrazie z aparatu. */
@Composable
private fun FrameText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge.copy(shadow = Shadow(Color.Black.copy(alpha = 0.6f), blurRadius = 8f)),
        color = Color.White,
        textAlign = TextAlign.Center,
        modifier = modifier.padding(24.dp),
    )
}

/** Białe narożniki celownika z mockupu 04. */
@Composable
private fun ViewfinderCorners(modifier: Modifier) {
    Canvas(modifier) {
        val length = 28.dp.toPx()
        val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Square)
        fun corner(x: Float, y: Float, dx: Float, dy: Float) = drawPath(
            Path().apply {
                moveTo(x + dx * length, y)
                lineTo(x, y)
                lineTo(x, y + dy * length)
            },
            Color.White,
            style = stroke,
        )
        corner(0f, 0f, 1f, 1f)
        corner(size.width, 0f, -1f, 1f)
        corner(0f, size.height, 1f, -1f)
        corner(size.width, size.height, -1f, -1f)
    }
}

/** Okrągła czerwona migawka z mockupu 04. */
@Composable
private fun ShutterButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(84.dp)
            .border(3.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .padding(7.dp)
            .clip(CircleShape)
            .background(SzczytomaniaTheme.colors.trail.copy(alpha = if (enabled) 1f else 0.4f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(R.drawable.ic_camera),
            contentDescription = stringResource(R.string.conquest_take_photo),
            tint = Color.White,
            modifier = Modifier.size(30.dp),
        )
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
