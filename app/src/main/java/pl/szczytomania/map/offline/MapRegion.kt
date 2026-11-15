package pl.szczytomania.map.offline

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

/** Region mapy do pobrania na telefon: prostokąt obejmujący szczyty jednego lub kilku pasm. */
data class MapRegion(
    val id: String,
    val name: String,
    val south: Double,
    val west: Double,
    val north: Double,
    val east: Double,
) {
    val centerLat get() = (south + north) / 2
    val centerLon get() = (west + east) / 2

    fun contains(lat: Double, lon: Double) = lat in south..north && lon in west..east

    /** Liczba kafelków mapy w regionie dla przybliżeń [MIN_ZOOM]..[MAX_ZOOM]. */
    fun tileCount(): Long = (MIN_ZOOM..MAX_ZOOM).sumOf { zoom ->
        val columns = tileX(east, zoom) - tileX(west, zoom) + 1
        val rows = tileY(south, zoom) - tileY(north, zoom) + 1
        columns.toLong() * rows
    }

    /** Przybliżony rozmiar pobrania – do napisu "ok. 95 MB" przed pobraniem. */
    val estimatedBytes: Long get() = STYLE_BYTES + tileCount() * AVERAGE_TILE_BYTES

    companion object {
        /** Od widoku kilku województw – po wyłączeniu internetu mapa nie znika przy oddalaniu. */
        const val MIN_ZOOM = 6

        /** Kafelki wektorowe mają najwięcej szczegółów (szlaki, ścieżki) na 14 – bliżej MapLibre je powiększa. */
        const val MAX_ZOOM = 14

        // Zmierzone na emulatorze dla stylu OpenFreeMap (Tatry: 477 kafelków = 16 MB, Beskid Żywiecki:
        // 1097 kafelków = 23 MB): każdy region ma też czcionki i ikony stylu, reszta to kafelki.
        // Styl MapTiler Outdoor ma dodatkowo poziomice i cieniowanie, więc tam pobrania są większe.
        private const val STYLE_BYTES = 10_500_000L
        private const val AVERAGE_TILE_BYTES = 11_500L

        // Numer kafelka w układzie "slippy map" (OpenStreetMap, MapLibre).
        private fun tileX(lon: Double, zoom: Int) = floor((lon + 180) / 360 * (1 shl zoom)).toInt()

        private fun tileY(lat: Double, zoom: Int): Int {
            val rad = Math.toRadians(lat)
            return floor((1 - ln(tan(rad) + 1 / cos(rad)) / PI) / 2 * (1 shl zoom)).toInt()
        }
    }
}

/**
 * Regiony do pobrania. Razem obejmują wszystkie szczyty z pliku – pilnuje tego test MapRegionsTest.
 * Granice to skrajne szczyty pasm z zapasem kilku kilometrów.
 */
object MapRegions {
    val all = listOf(
        MapRegion("tatry", "Tatry", 49.15, 19.70, 49.32, 20.25),
        MapRegion("beskid-zywiecki", "Beskid Żywiecki", 49.38, 18.93, 49.69, 19.66),
        MapRegion("beskid-slaski", "Beskid Śląski i Mały", 49.58, 18.77, 49.82, 19.48),
        MapRegion("beskid-wyspowy", "Beskid Wyspowy", 49.59, 19.95, 49.80, 20.41),
        MapRegion("gorce-pieniny", "Gorce i Pieniny", 49.36, 20.03, 49.60, 20.59),
        MapRegion("beskid-sadecki", "Beskid Sądecki i Niski", 49.37, 20.54, 49.50, 21.13),
        MapRegion("bieszczady", "Bieszczady", 48.98, 22.15, 49.33, 22.88),
        MapRegion("karkonosze", "Karkonosze i Góry Izerskie", 50.70, 15.24, 50.97, 15.93),
        MapRegion("sudety-srodkowe", "Góry Sowie, Wałbrzyskie i Ślęża", 50.62, 16.18, 50.89, 16.74),
        MapRegion("ziemia-klodzka", "Ziemia Kłodzka i Śnieżnik", 50.17, 16.30, 50.50, 17.05),
        MapRegion("gory-opawskie", "Góry Opawskie", 50.22, 17.36, 50.30, 17.50),
        MapRegion("gory-swietokrzyskie", "Góry Świętokrzyskie", 50.83, 20.86, 50.92, 21.08),
    )

    fun byId(id: String) = all.find { it.id == id }
}
