package pl.szczytomania.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.szczytomania.data.model.Peak

class SummitCheckTest {

    private val peak = Peak("test/agh", "Szczyt testowy AGH", 210, 50.064547, 19.9232975, "Testowe")

    // Na tej szerokości stopień południka to ok. 111,2 km, czyli 0,001° ≈ 111,2 m.
    private fun readingNorthOfPeak(degrees: Double, accuracy: Float = 10f, isMock: Boolean = false) =
        GpsReading(peak.lat + degrees, peak.lon, accuracy, isMock)

    @Test
    fun distanceMatchesKnownValue() {
        // Stopień południka to 1/360 obwodu Ziemi: 2π · 6371008,8 m / 360 ≈ 111195 m.
        assertEquals(111_195.0, distanceMeters(50.0, 20.0, 51.0, 20.0), 1.0)
        // Równoleżnik jest krótszy: na 50°N stopień to 111195 m · cos(50°) ≈ 71475 m.
        assertEquals(71_475.0, distanceMeters(50.0, 20.0, 50.0, 21.0), 5.0)
    }

    @Test
    fun distanceIsZeroForTheSamePoint() {
        assertEquals(0.0, distanceMeters(peak.lat, peak.lon, peak.lat, peak.lon), 0.001)
    }

    @Test
    fun passesWithinRadiusAndAccuracy() {
        assertTrue(SummitCheck.of(peak, readingNorthOfPeak(0.001)).passed)
    }

    @Test
    fun radiusLimitIsInclusive() {
        val reading = readingNorthOfPeak(0.0)
        assertTrue(SummitCheck(reading, 150.0).inRange)
        assertFalse(SummitCheck(reading, 150.01).inRange)
    }

    @Test
    fun failsWhenTooFar() {
        val check = SummitCheck.of(peak, readingNorthOfPeak(0.002))
        assertFalse(check.inRange)
        assertFalse(check.passed)
    }

    @Test
    fun accuracyLimitIsInclusive() {
        assertTrue(SummitCheck.of(peak, readingNorthOfPeak(0.0, accuracy = 50f)).accurate)
        val tooInaccurate = SummitCheck.of(peak, readingNorthOfPeak(0.0, accuracy = 50.5f))
        assertFalse(tooInaccurate.accurate)
        assertFalse(tooInaccurate.passed)
    }

    @Test
    fun failsWhenLocationIsMocked() {
        val check = SummitCheck.of(peak, readingNorthOfPeak(0.0, isMock = true))
        assertTrue(check.inRange && check.accurate)
        assertFalse(check.genuine)
        assertFalse(check.passed)
    }
}
