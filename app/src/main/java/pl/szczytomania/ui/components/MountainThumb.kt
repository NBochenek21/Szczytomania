package pl.szczytomania.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Kolory ilustracji z mockupów (niebo i dwa pasma gór) – takie same w obu motywach, jak zdjęcie.
private val skies = listOf(Color(0xFF5B7387), Color(0xFF3F566B), Color(0xFF2B3A45))
private val farMountains = Color(0xFF55684E)
private val nearMountains = Color(0xFF7C8B62)

/**
 * Miniatura górskiego widoku przy zdobyczy znajomego (mockupy "Znajomi" i "Profil znajomego").
 * Kolor nieba zależy od [seed] (np. id szczytu), więc sąsiednie wiersze się różnią.
 */
@Composable
fun MountainThumb(seed: String, modifier: Modifier = Modifier, size: Dp = 52.dp) {
    val sky = skies[Math.floorMod(seed.hashCode(), skies.size)]
    Canvas(modifier.size(size).clip(RoundedCornerShape(10.dp))) {
        val w = this.size.width
        val h = this.size.height
        drawRect(sky)
        drawPath(
            Path().apply {
                moveTo(0f, h * 0.62f)
                lineTo(w * 0.28f, h * 0.42f)
                lineTo(w * 0.45f, h * 0.55f)
                lineTo(w * 0.68f, h * 0.30f)
                lineTo(w, h * 0.55f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            },
            farMountains,
        )
        drawPath(
            Path().apply {
                moveTo(0f, h * 0.80f)
                lineTo(w * 0.40f, h * 0.66f)
                lineTo(w, h * 0.72f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            },
            nearMountains,
        )
    }
}
