package pl.szczytomania.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.Text
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import pl.szczytomania.ui.theme.IbmPlexMono
import pl.szczytomania.ui.theme.Rokkitt

/**
 * Pieczątka zdobytego szczytu (mockupy "Szczyt zdobyty!" i "Moje zdobycze"): podwójny okrąg,
 * trójkąt, nazwa i data. Z [ringText] – duża pieczątka z napisem dookoła i wysokością.
 */
@Composable
fun PeakStamp(
    name: String,
    date: String,
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    elevation: String? = null,
    ringText: String? = null,
) {
    val big = ringText != null
    val sp = with(LocalDensity.current) { { fraction: Float -> (size * fraction).toSp() } }
    Box(modifier.size(size).rotate(if (big) -6f else -3f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2
            val stroke = r * (if (big) 0.025f else 0.035f)
            drawCircle(color, radius = r - stroke, style = Stroke(stroke))
            drawCircle(color, radius = r * (if (big) 0.9f else 0.86f), style = Stroke(stroke * 0.6f))
            if (ringText != null) {
                drawCircle(
                    color,
                    radius = r * 0.62f,
                    style = Stroke(stroke * 0.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(r * 0.03f, r * 0.03f))),
                )
                // Napis dookoła rozciągnięty na cały obwód.
                drawIntoCanvas { canvas ->
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        this.color = color.toArgb()
                        textSize = r * 0.11f
                        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    }
                    val textRadius = r * 0.72f
                    val circumference = (2 * Math.PI * textRadius).toFloat()
                    paint.letterSpacing = (circumference - paint.measureText(ringText)) / (ringText.length * paint.textSize)
                    val path = android.graphics.Path().apply {
                        addCircle(center.x, center.y, textRadius, android.graphics.Path.Direction.CW)
                    }
                    // Napis zaczyna się w 60% obwodu (z lewej u góry). Obracamy płótno zamiast przesuwać
                    // początek napisu – drawTextOnPath nie zawija tekstu za koniec okręgu, więc litery
                    // za końcem ścieżki lądowały w jednym miejscu, jedna na drugiej.
                    val native = canvas.nativeCanvas
                    native.save()
                    native.rotate(360f * 0.6f, center.x, center.y)
                    native.drawTextOnPath(ringText, path, 0f, 0f, paint)
                    native.restore()
                }
            }
            // Trójkąt szczytu nad nazwą.
            val t = r * (if (big) 0.16f else 0.15f)
            val top = center.y - r * (if (big) 0.42f else 0.45f)
            drawPath(
                Path().apply {
                    moveTo(center.x, top)
                    lineTo(center.x + t, top + t * 1.1f)
                    lineTo(center.x - t, top + t * 1.1f)
                    close()
                },
                color,
            )
        }
        Column(Modifier.padding(top = size * 0.12f), horizontalAlignment = Alignment.CenterHorizontally) {
            // Długie nazwy ("Mięguszowiecki Szczyt Wielki") mniejszą czcionką, w trzech linijkach.
            val nameSize = (if (big) 0.1f else 0.11f) * (if (name.length > 14) 0.75f else 1f)
            Text(
                text = name.uppercase(),
                style = TextStyle(fontFamily = Rokkitt, fontWeight = FontWeight.ExtraBold, fontSize = sp(nameSize), lineHeight = sp(nameSize * 1.05f)),
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 3,
                modifier = Modifier.padding(horizontal = size * 0.12f),
            )
            val mono = TextStyle(fontFamily = IbmPlexMono, fontWeight = FontWeight.Medium, fontSize = sp(if (big) 0.065f else 0.09f))
            if (elevation != null) Text(elevation, style = mono, color = color)
            Text(date, style = mono, color = color)
        }
    }
}
