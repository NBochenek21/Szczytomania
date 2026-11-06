package pl.szczytomania.ui.screens.peaks

import org.junit.Assert.assertEquals
import org.junit.Test
import pl.szczytomania.data.model.Peak

class PeakSearchTest {

    private val peaks = listOf(
        peak("Śnieżnik", 1423, "Masyw Śnieżnika"),
        peak("Łysica", 612, "Góry Świętokrzyskie"),
        peak("Babia Góra", 1723, "Beskid Żywiecki"),
        peak("Pilsko", 1557, "Beskid Żywiecki"),
        peak("Rysy", 2499, "Tatry"),
    )

    @Test
    fun normalizeRemovesPolishCharactersAndCase() {
        assertEquals("snieznik", PeakSearch.normalize("Śnieżnik"))
        assertEquals("lysica", PeakSearch.normalize("ŁYSICA"))
        assertEquals("zazolc gesla jazn", PeakSearch.normalize("  Zażółć gęślą jaźń "))
    }

    @Test
    fun findsByNameWithoutPolishCharacters() {
        assertEquals(listOf("Śnieżnik"), names(PeakSearch.filter(peaks, "snieznik")))
        assertEquals(listOf("Łysica"), names(PeakSearch.filter(peaks, "lysica")))
    }

    @Test
    fun ignoresCase() {
        assertEquals(listOf("Rysy"), names(PeakSearch.filter(peaks, "RYSY")))
        assertEquals(listOf("Łysica"), names(PeakSearch.filter(peaks, "ŁySiCa")))
    }

    @Test
    fun polishCharactersInQueryAlsoMatch() {
        assertEquals(listOf("Śnieżnik"), names(PeakSearch.filter(peaks, "Śnież")))
    }

    @Test
    fun findsByRange() {
        assertEquals(listOf("Babia Góra", "Pilsko"), names(PeakSearch.filter(peaks, "beskid zyw")))
        // "Śnieżnik" pasuje i nazwą, i pasmem – ma być na liście tylko raz.
        assertEquals(listOf("Śnieżnik"), names(PeakSearch.filter(peaks, "masyw sniez")))
    }

    @Test
    fun matchesPartOfName() {
        assertEquals(listOf("Babia Góra"), names(PeakSearch.filter(peaks, "gora")))
    }

    @Test
    fun emptyOrBlankQueryReturnsAllFromHighest() {
        val expected = listOf("Rysy", "Babia Góra", "Pilsko", "Śnieżnik", "Łysica")
        assertEquals(expected, names(PeakSearch.filter(peaks, "")))
        assertEquals(expected, names(PeakSearch.filter(peaks, "   ")))
    }

    @Test
    fun noMatchReturnsEmptyList() {
        assertEquals(emptyList<String>(), names(PeakSearch.filter(peaks, "Mont Blanc")))
    }

    @Test
    fun sortsByNameInPolishOrder() {
        // Zwykłe sortowanie napisów dałoby "Babia Góra, Pilsko, Rysy, Łysica, Śnieżnik".
        assertEquals(
            listOf("Babia Góra", "Łysica", "Pilsko", "Rysy", "Śnieżnik"),
            names(PeakSearch.filter(peaks, "", PeakSort.NAME)),
        )
    }

    @Test
    fun polishLettersComeRightAfterTheirBaseLetter() {
        val list = listOf(peak("Śnieżka", 1603, "Karkonosze"), peak("Szrenica", 1361, "Karkonosze"),
            peak("Łysica", 612, "Góry Świętokrzyskie"), peak("Lubomir", 904, "Beskid Makowski"),
            peak("Mogielica", 1171, "Beskid Wyspowy"), peak("Tarnica", 1346, "Bieszczady"))
        assertEquals(
            listOf("Lubomir", "Łysica", "Mogielica", "Szrenica", "Śnieżka", "Tarnica"),
            names(PeakSearch.filter(list, "", PeakSort.NAME)),
        )
    }

    @Test
    fun sortsByRangeThenFromHighest() {
        assertEquals(
            listOf("Babia Góra", "Pilsko", "Łysica", "Śnieżnik", "Rysy"),
            names(PeakSearch.filter(peaks, "", PeakSort.RANGE)),
        )
    }

    @Test
    fun sortingKeepsSearchFilter() {
        assertEquals(listOf("Babia Góra", "Pilsko"), names(PeakSearch.filter(peaks, "beskid", PeakSort.NAME)))
    }

    private fun peak(name: String, elevation: Int, range: String) =
        Peak("node/${name.hashCode()}", name, elevation, 50.0, 20.0, range)

    private fun names(peaks: List<Peak>) = peaks.map { it.name }
}
