package pl.szczytomania.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import pl.szczytomania.R

// Czcionki z Google Fonts (licencja OFL, teksty licencji w docs/licencje-czcionek).
// Rokkitt i Work Sans to czcionki zmienne: jeden plik, grubość wybierana przez FontVariation.

@OptIn(ExperimentalTextApi::class)
private fun variable(resId: Int, weight: FontWeight) = Font(
    resId = resId,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/** Nagłówki: nazwy szczytów, tytuły ekranów. */
val Rokkitt = FontFamily(
    variable(R.font.rokkitt, FontWeight.Bold),
    variable(R.font.rokkitt, FontWeight.ExtraBold),
)

/** Treść: opisy, etykiety, przyciski. */
val WorkSans = FontFamily(
    variable(R.font.work_sans, FontWeight.Normal),
    variable(R.font.work_sans, FontWeight.Medium),
    variable(R.font.work_sans, FontWeight.SemiBold),
)

/** Liczby i wysokości ("1725 m", "7/28"). */
val IbmPlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_mono_semibold, FontWeight.SemiBold),
)

internal val SzczytomaniaTypography = Typography(
    displaySmall = TextStyle(fontFamily = Rokkitt, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp, lineHeight = 40.sp),
    headlineLarge = TextStyle(fontFamily = Rokkitt, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = Rokkitt, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = Rokkitt, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = Rokkitt, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Rokkitt, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 23.sp),
    titleSmall = TextStyle(fontFamily = Rokkitt, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = WorkSans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = WorkSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = WorkSans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = WorkSans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = WorkSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp),
)

/** Style liczb – nie ma ich w schemacie Material 3. */
object NumberStyles {
    /** Wysokość w wierszu listy: "2499 m". */
    val elevation = TextStyle(fontFamily = IbmPlexMono, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp)
    /** Licznik zestawu na karcie: "7/28". */
    val counter = TextStyle(fontFamily = IbmPlexMono, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 24.sp)
    /** Duża liczba zdobytych w szczegółach zestawu. */
    val big = TextStyle(fontFamily = IbmPlexMono, fontWeight = FontWeight.SemiBold, fontSize = 48.sp, lineHeight = 52.sp)
    /** Plakietki i drobne dopiski: "KGP", "DO ZDOBYCIA". */
    val tag = TextStyle(fontFamily = IbmPlexMono, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.5.sp)
}
