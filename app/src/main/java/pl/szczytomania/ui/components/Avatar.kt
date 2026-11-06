package pl.szczytomania.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pl.szczytomania.ui.theme.SzczytomaniaTheme

/**
 * Awatar z pierwszą literą nicku na zielonym kółku (mapa, menu, profil).
 * Zdjęcie profilowe dojdzie z edycją konta.
 *
 * @param dashedRing przerywany pierścień wokół – na ekranie profilu, jak na mockupie.
 */
@Composable
fun Avatar(
    initial: String,
    size: Dp,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    dashedRing: Boolean = false,
    /** Odwrócone kolory (białe kółko, zielona litera) – na zielonym nagłówku menu. */
    inverted: Boolean = false,
) {
    val green = SzczytomaniaTheme.colors.conquered
    val light = MaterialTheme.colorScheme.onPrimary
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        if (dashedRing) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    color = green,
                    radius = this.size.minDimension / 2 - 1.dp.toPx(),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
                    ),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (dashedRing) size * 0.08f else 0.dp)
                .clip(CircleShape)
                .background(if (inverted) light else green),
            contentAlignment = Alignment.Center,
        ) {
            Text(initial, style = textStyle, color = if (inverted) green else light)
        }
    }
}
