package pl.szczytomania.ui.screens.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pl.szczytomania.data.model.Peak
import pl.szczytomania.data.model.PeakSet

class ProfileStatsTest {

    private val peaks = listOf(
        Peak("node/1", "Rysy", 2499, 49.18, 20.09, "Tatry", kgp = true),
        Peak("node/2", "Śnieżka", 1603, 50.74, 15.74, "Karkonosze", kgp = true),
        Peak("node/3", "Giewont", 1895, 49.25, 19.93, "Tatry"),
    ).associateBy { it.id }

    private val sets = listOf(
        PeakSet("kgp", "Korona Gór Polski", "opis", listOf("node/1", "node/2"), badge = "Odznaka klasyczna"),
        PeakSet("tatry", "Tatry Polskie", "opis", listOf("node/1", "node/3")),
        PeakSet("sudety", "Sudety", "opis", listOf("node/2")),
    )

    @Test
    fun newUserHasEmptyStatsButSeesKorona() {
        val stats = ProfileStats.of(peaks, sets, emptySet())
        assertEquals(0, stats.conqueredCount)
        assertNull(stats.highest)
        assertEquals(0, stats.startedSets)
        assertEquals(listOf("kgp"), stats.shownSets.map { it.set.id })
    }

    @Test
    fun countsConquestsHighestPeakAndStartedSets() {
        val stats = ProfileStats.of(peaks, sets, setOf("node/2", "node/3"))
        assertEquals(2, stats.conqueredCount)
        assertEquals("Giewont", stats.highest?.name)
        assertEquals(3, stats.startedSets)
        assertEquals(listOf("kgp", "tatry", "sudety"), stats.shownSets.map { it.set.id })
    }

    @Test
    fun ignoresPeaksMissingFromPeaksFile() {
        val stats = ProfileStats.of(peaks, sets, setOf("test/agh"))
        assertEquals(0, stats.conqueredCount)
        assertNull(stats.highest)
    }

    @Test
    fun polishPluralForms() {
        assertEquals(PolishPlural.MANY, polishPlural(0))
        assertEquals(PolishPlural.ONE, polishPlural(1))
        assertEquals(PolishPlural.FEW, polishPlural(2))
        assertEquals(PolishPlural.FEW, polishPlural(4))
        assertEquals(PolishPlural.MANY, polishPlural(5))
        assertEquals(PolishPlural.MANY, polishPlural(12))
        assertEquals(PolishPlural.MANY, polishPlural(14))
        assertEquals(PolishPlural.FEW, polishPlural(22))
        assertEquals(PolishPlural.MANY, polishPlural(28))
        assertEquals(PolishPlural.MANY, polishPlural(111))
    }

    @Test
    fun avatarInitialIsFirstLetterUppercase() {
        assertEquals("N", avatarInitial("norbert"))
        assertEquals("Ł", avatarInitial("  łukasz"))
        assertEquals("?", avatarInitial(""))
    }
}
