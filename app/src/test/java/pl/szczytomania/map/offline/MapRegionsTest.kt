package pl.szczytomania.map.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.szczytomania.data.peaks.PeakFileParser
import java.io.File

class MapRegionsTest {

    // Testy jednostkowe uruchamiają się w katalogu modułu app/.
    private val peaks = PeakFileParser.parse(File("src/main/assets/peaks/peaks.json").readText())

    @Test
    fun everyPeakCanBeDownloadedForOfflineUse() {
        val missing = peaks.filter { peak -> MapRegions.all.none { it.contains(peak.lat, peak.lon) } }
        assertTrue("Szczyty poza regionami: ${missing.map { "${it.name} (${it.range})" }}", missing.isEmpty())
    }

    @Test
    fun idsAreUnique() {
        assertEquals(MapRegions.all.size, MapRegions.all.map { it.id }.toSet().size)
    }

    @Test
    fun boundsAreValid() {
        MapRegions.all.forEach {
            assertTrue("${it.id}: south < north", it.south < it.north)
            assertTrue("${it.id}: west < east", it.west < it.east)
        }
    }

    @Test
    fun regionsStayReasonablySmall() {
        // Kilka tysięcy kafelków to kilkadziesiąt–sto kilkadziesiąt MB – więcej nie ma sensu pobierać na raz.
        MapRegions.all.forEach { assertTrue("${it.id}: ${it.tileCount()} kafelków", it.tileCount() in 10..4_000) }
    }

    @Test
    fun tileCountMatchesHandCalculation() {
        // Na zoomie 14 kafelek ma 360/16384 ≈ 0,022° długości, więc 0,01° mieści się w jednym kafelku
        // na każdym zoomie 6..14 – region wewnątrz jednego kafelka to 9 kafelków (po jednym na zoom).
        val tiny = MapRegion("tiny", "Mały", 49.2301, 20.0001, 49.2302, 20.0002)
        assertEquals(9L, tiny.tileCount())
    }
}
