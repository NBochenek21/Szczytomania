package pl.szczytomania.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pl.szczytomania.ui.theme.NumberStyles
import pl.szczytomania.ui.theme.SzczytomaniaTheme

/** Kwadratowy przycisk z ikoną w ramce (hamburger, strzałka wstecz) – lewy górny róg ekranów. */
@Composable
fun SquareIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.5.dp, MaterialTheme.colorScheme.outline, shape)
            .clickable(onClick = onClick, role = Role.Button),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = contentDescription, tint = MaterialTheme.colorScheme.onSurface)
    }
}

/** Nagłówek ekranu: przycisk w ramce, opcjonalny nadtytuł, tytuł i podpis. */
@Composable
fun ScreenHeader(
    title: String,
    @DrawableRes navIcon: Int,
    navDescription: String,
    onNavClick: () -> Unit,
    modifier: Modifier = Modifier,
    overline: String? = null,
    subtitle: String? = null,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SquareIconButton(navIcon, navDescription, onNavClick)
        Column(modifier = Modifier.padding(start = 14.dp)) {
            if (overline != null) {
                Text(overline.uppercase(), style = NumberStyles.tag, color = SzczytomaniaTheme.colors.trail)
            }
            Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Cienka przerywana linia między wierszami list. */
@Composable
fun DashedDivider(modifier: Modifier = Modifier, color: Color = SzczytomaniaTheme.colors.lines) {
    Canvas(modifier = modifier.fillMaxWidth().height(1.dp)) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
        )
    }
}

/** Trójkąt szczytu: wypełniony = zdobyty, kontur = do zdobycia. */
@Composable
fun PeakTriangle(conquered: Boolean, modifier: Modifier = Modifier, iconSize: Dp = 24.dp) {
    val fill = SzczytomaniaTheme.colors.conquered
    val outline = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = modifier.size(iconSize)) {
        val stroke = 2.dp.toPx()
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w / 2, stroke)
            lineTo(w - stroke / 2, h * 0.88f)
            lineTo(stroke / 2, h * 0.88f)
            close()
        }
        if (conquered) drawPath(path, fill) else drawPath(path, outline, style = Stroke(width = stroke))
    }
}

/** Mała plakietka w ramce, np. "KGP", "KORONA GÓR POLSKI", "DO ZDOBYCIA". */
@Composable
fun Tag(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    dashed: Boolean = false,
) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = modifier.then(
            if (dashed) {
                Modifier.dashedBorder(color, 4.dp)
            } else {
                Modifier.border(1.dp, color, shape)
            },
        ),
    ) {
        Text(
            text = text.uppercase(),
            style = NumberStyles.tag,
            color = color,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
        )
    }
}

private fun Modifier.dashedBorder(color: Color, radius: Dp) = drawBehind {
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(
            width = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx())),
        ),
    )
}

/** Zaokrąglony przycisk-pigułka: sortowanie, filtry. Zaznaczony jest wypełniony. */
@Composable
fun PillButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
) {
    val colors = SzczytomaniaTheme.colors
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .background(if (selected) colors.selected else Color.Transparent)
            .border(1.5.dp, if (selected) colors.selected else MaterialTheme.colorScheme.outline, shape)
            .clickable(onClick = onClick, role = Role.Tab)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val content = if (selected) colors.onSelected else MaterialTheme.colorScheme.onBackground
        Text(text, style = MaterialTheme.typography.labelLarge, color = content)
        if (count != null) {
            Spacer(Modifier.width(8.dp))
            Text(count.toString(), style = NumberStyles.tag, color = content)
        }
    }
}

/** Dwie lub więcej zakładek w jednej ramce (np. "Gotowe | Własne"). */
@Composable
fun SegmentedTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SzczytomaniaTheme.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.5.dp, MaterialTheme.colorScheme.outline, shape),
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (selected) colors.selected else Color.Transparent)
                    .clickable(onClick = { onSelect(index) }, role = Role.Tab)
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) colors.onSelected else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Ciągły pasek postępu (karty zestawów). */
@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    val colors = SzczytomaniaTheme.colors
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(shape)
            .background(colors.lines.copy(alpha = 0.5f)),
    ) {
        if (fraction > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(10.dp)
                    .clip(shape)
                    .background(colors.conquered),
            )
        }
    }
}

/** Postęp z osobnym kwadracikiem na każdy szczyt (Korona Gór Polski, szczegóły zestawu). */
@Composable
fun SegmentedProgress(conquered: List<Boolean>, modifier: Modifier = Modifier) {
    val colors = SzczytomaniaTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().height(28.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        conquered.forEach { done ->
            val shape = RoundedCornerShape(4.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .clip(shape)
                    .background(if (done) colors.conquered else Color.Transparent)
                    .border(1.dp, if (done) colors.conquered else colors.lines, shape),
            )
        }
    }
}
