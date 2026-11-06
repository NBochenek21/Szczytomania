package pl.szczytomania.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LightScheme = lightColorScheme(
    primary = LightGreen,
    onPrimary = LightCard,
    primaryContainer = LightGreen.copy(alpha = 0.15f),
    onPrimaryContainer = LightGreen,
    secondary = LightLocation,
    onSecondary = LightCard,
    // Zaznaczone przyciski segmentowe (np. Logowanie / Rejestracja) – zieleń zamiast domyślnego fioletu.
    secondaryContainer = LightGreen.copy(alpha = 0.15f),
    onSecondaryContainer = LightGreen,
    error = LightTrailRed,
    onError = LightCard,
    background = LightBackground,
    onBackground = LightText,
    surface = LightCard,
    onSurface = LightText,
    surfaceVariant = LightBackground,
    onSurfaceVariant = LightTextSecondary,
    surfaceContainerLow = LightCard,
    surfaceContainer = LightCard,
    surfaceContainerHigh = LightCard,
    // Mocne obramowania (pola, przyciski, karty) są w kolorze tekstu, cienkie linie – w kolorze linii.
    outline = LightText,
    outlineVariant = LightLines,
)

private val DarkScheme = darkColorScheme(
    primary = DarkGreen,
    onPrimary = DarkBackground,
    primaryContainer = DarkGreen.copy(alpha = 0.18f),
    onPrimaryContainer = DarkGreen,
    secondary = DarkLocation,
    onSecondary = DarkBackground,
    secondaryContainer = DarkGreen.copy(alpha = 0.18f),
    onSecondaryContainer = DarkGreen,
    error = DarkTrailRed,
    onError = DarkBackground,
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkCard,
    onSurface = DarkText,
    surfaceVariant = DarkBackground,
    onSurfaceVariant = DarkTextSecondary,
    surfaceContainerLow = DarkCard,
    surfaceContainer = DarkCard,
    surfaceContainerHigh = DarkCard,
    outline = DarkBorders,
    outlineVariant = DarkBorders,
)

private val LocalSzczytomaniaColors = staticCompositionLocalOf { LightExtraColors }

/** Motyw aplikacji: kolory i czcionki z mockupów, jasny lub ciemny zgodnie z ustawieniem telefonu. */
@Composable
fun SzczytomaniaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalSzczytomaniaColors provides if (darkTheme) DarkExtraColors else LightExtraColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = SzczytomaniaTypography,
            content = content,
        )
    }
}

/** Dostęp do kolorów spoza Material 3: `SzczytomaniaTheme.colors.conquered`. */
object SzczytomaniaTheme {
    val colors: SzczytomaniaColors
        @Composable @ReadOnlyComposable get() = LocalSzczytomaniaColors.current
}
