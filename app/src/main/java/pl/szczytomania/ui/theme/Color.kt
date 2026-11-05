package pl.szczytomania.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Paleta z mockupów (docs/mockupy). Motyw jasny:
internal val LightBackground = Color(0xFFF2EAD7)
internal val LightCard = Color(0xFFFBF7EC)
internal val LightText = Color(0xFF28261E)
internal val LightTextSecondary = Color(0xFF5C5644)
internal val LightLines = Color(0xFFCBBC98)
internal val LightGreen = Color(0xFF2F5D3A)
internal val LightTrailRed = Color(0xFFA8372B)
internal val LightLocation = Color(0xFF2F5E8E)

// Motyw ciemny:
internal val DarkBackground = Color(0xFF1A1F1A)
internal val DarkCard = Color(0xFF232A23)
internal val DarkText = Color(0xFFEEE6D1)
// Mockupy nie podają tekstu drugorzędnego w ciemnym motywie: DarkText w 70% na tle DarkBackground.
internal val DarkTextSecondary = Color(0xFFAEAA9A)
internal val DarkBorders = Color(0xFF56634F)
internal val DarkGreen = Color(0xFF8FC096)
internal val DarkTrailRed = Color(0xFFE8907A)
internal val DarkLocation = Color(0xFF7FB0E0)

/** Kolory spoza schematu Material 3, używane w mockupach. */
@Immutable
data class SzczytomaniaColors(
    /** Zdobyty szczyt, postęp, główne przyciski. */
    val conquered: Color,
    /** Czerwony szlak: plakietki KGP, nadtytuły. */
    val trail: Color,
    /** Lokalizacja użytkownika i odległości. */
    val location: Color,
    /** Cienkie, przerywane linie między wierszami i nieaktywny postęp. */
    val lines: Color,
    /** Wypełnienie zaznaczonej zakładki lub przycisku sortowania i tekst na nim. */
    val selected: Color,
    val onSelected: Color,
)

internal val LightExtraColors = SzczytomaniaColors(
    conquered = LightGreen,
    trail = LightTrailRed,
    location = LightLocation,
    lines = LightLines,
    selected = LightText,
    onSelected = LightCard,
)

internal val DarkExtraColors = SzczytomaniaColors(
    conquered = DarkGreen,
    trail = DarkTrailRed,
    location = DarkLocation,
    lines = DarkBorders,
    selected = DarkText,
    onSelected = DarkBackground,
)
