package pl.szczytomania.location

import pl.szczytomania.data.model.Peak
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Warunki zaliczenia szczytu z karty projektu. */
object SummitRules {
    const val MAX_DISTANCE_METERS = 150
    const val MAX_ACCURACY_METERS = 50
}

/** Odczyt GPS – te dane trafiają też do zapisu zdobyczy (współrzędne i dokładność). */
data class GpsReading(
    val lat: Double,
    val lon: Double,
    val accuracyMeters: Float,
    /** Pozycja pochodzi z aplikacji podmieniającej GPS albo z emulatora. */
    val isMock: Boolean,
)

/** Wynik porównania odczytu GPS ze szczytem. Szczyt można zdobyć tylko przy [passed]. */
data class SummitCheck(val reading: GpsReading, val distanceMeters: Double) {
    val inRange get() = distanceMeters <= SummitRules.MAX_DISTANCE_METERS
    val accurate get() = reading.accuracyMeters <= SummitRules.MAX_ACCURACY_METERS
    val genuine get() = !reading.isMock
    val passed get() = inRange && accurate && genuine

    companion object {
        fun of(peak: Peak, reading: GpsReading) = SummitCheck(
            reading = reading,
            distanceMeters = distanceMeters(reading.lat, reading.lon, peak.lat, peak.lon),
        )
    }
}

private const val EARTH_RADIUS_METERS = 6_371_008.8

/** Odległość po powierzchni Ziemi (wzór haversine). */
fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val h = sin(dLat / 2).pow(2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return 2 * EARTH_RADIUS_METERS * asin(sqrt(h))
}
