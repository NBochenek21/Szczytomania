package pl.szczytomania.data.peaks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PeakSetsTest {

    // Testy jednostkowe uruchamiają się w katalogu modułu app/.
    private val sets = PeakSetFileParser.parse(File("src/main/assets/sets/sets.json").readText())
    private val peaks = PeakFileParser.parse(File("src/main/assets/peaks/peaks.json").readText())
    private val peaksById = peaks.associateBy { it.id }

    @Test
    fun parsesSetWithOptionalBadge() {
        val parsed = PeakSetFileParser.parse(
            """
            { "version": 1, "sets": [
              { "id": "a", "name": "A", "description": "opis", "peakIds": ["node/1"], "badge": "Odznaka" },
              { "id": "b", "name": "B", "description": "opis", "peakIds": [] }
            ] }
            """,
        )
        assertEquals(listOf("Odznaka", null), parsed.map { it.badge })
    }

    @Test
    fun rejectsUnsupportedVersion() {
        assertThrows(IllegalArgumentException::class.java) {
            PeakSetFileParser.parse("""{ "version": 2, "sets": [] }""")
        }
    }

    @Test
    fun setIdsAreUnique() {
        assertEquals(sets.size, sets.map { it.id }.toSet().size)
    }

    @Test
    fun everyPeakInSetsExistsInPeaksFile() {
        sets.forEach { set ->
            set.peakIds.forEach { id -> assertTrue("${set.name}: brak szczytu $id w peaks.json", id in peaksById) }
        }
    }

    @Test
    fun setsHaveNoDuplicatesAndAreSortedFromHighest() {
        sets.forEach { set ->
            assertEquals("${set.name}: powtórzony szczyt", set.peakIds.size, set.peakIds.toSet().size)
            val elevations = set.peakIds.map { peaksById.getValue(it).elevation }
            assertEquals("${set.name}: kolejność", elevations.sortedDescending(), elevations)
        }
    }

    @Test
    fun koronaGorPolskiMatchesKgpFlag() {
        val kgp = sets.single { it.id == "kgp" }
        assertEquals(peaks.filter { it.kgp }.map { it.id }.toSet(), kgp.peakIds.toSet())
        assertEquals(28, kgp.peakIds.size)
    }
}
