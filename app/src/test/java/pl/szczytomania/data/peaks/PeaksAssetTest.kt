package pl.szczytomania.data.peaks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Sprawdza plik szczytów, który trafia do aplikacji – pilnuje poprawności każdego importu z OpenStreetMap. */
class PeaksAssetTest {

    // Testy jednostkowe uruchamiają się w katalogu modułu app/.
    private val peaks = PeakFileParser.parse(File("src/main/assets/peaks/peaks.json").readText())

    @Test
    fun fileIsNotEmpty() {
        assertTrue(peaks.isNotEmpty())
    }

    @Test
    fun idsAreUniqueOsmIds() {
        assertEquals("Powtórzone id", peaks.size, peaks.map { it.id }.toSet().size)
        peaks.forEach { assertTrue("Złe id: ${it.id}", OSM_ID.matches(it.id)) }
    }

    @Test
    fun peaksAreInPoland() {
        peaks.forEach {
            assertTrue("${it.name}: szerokość ${it.lat} poza Polską", it.lat in 49.0..54.9)
            assertTrue("${it.name}: długość ${it.lon} poza Polską", it.lon in 14.1..24.2)
        }
    }

    @Test
    fun fieldsAreFilledIn() {
        peaks.forEach {
            assertTrue("Pusta nazwa: ${it.id}", it.name.isNotBlank())
            assertTrue("${it.name}: brak pasma", it.range.isNotBlank())
            // Najwyższy punkt Polski to Rysy, 2499 m.
            assertTrue("${it.name}: wysokość ${it.elevation} m", it.elevation in 1..2499)
        }
    }

    @Test
    fun containsWholeKoronaGorPolski() {
        assertEquals("Szczyty KGP w pliku", 28, peaks.count { it.kgp })
    }

    private companion object {
        val OSM_ID = Regex("""^(node|way|relation)/\d+$""")
    }
}
