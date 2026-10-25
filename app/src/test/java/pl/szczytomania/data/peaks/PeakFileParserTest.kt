package pl.szczytomania.data.peaks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import pl.szczytomania.data.model.Peak

class PeakFileParserTest {

    @Test
    fun parsesPeaks() {
        val peaks = PeakFileParser.parse(
            """
            {
              "version": 1,
              "source": "© OpenStreetMap contributors (ODbL)",
              "peaks": [
                { "id": "node/4366169405", "name": "Rysy", "elevation": 2499, "lat": 49.1795756, "lon": 20.0881081, "range": "Tatry" }
              ]
            }
            """,
        )

        assertEquals(
            listOf(Peak("node/4366169405", "Rysy", 2499, 49.1795756, 20.0881081, "Tatry")),
            peaks,
        )
    }

    @Test
    fun ignoresUnknownFields() {
        val peaks = PeakFileParser.parse(
            """
            {
              "version": 1,
              "source": "OSM",
              "generatedAt": "2026-10-25",
              "peaks": [
                { "id": "node/1", "name": "Test", "elevation": 1000, "lat": 49.5, "lon": 20.0, "range": "Gorce", "wikidata": "Q1" }
              ]
            }
            """,
        )

        assertEquals("Test", peaks.single().name)
    }

    @Test
    fun rejectsUnsupportedVersion() {
        assertThrows(IllegalArgumentException::class.java) {
            PeakFileParser.parse("""{ "version": 2, "source": "OSM", "peaks": [] }""")
        }
    }
}
