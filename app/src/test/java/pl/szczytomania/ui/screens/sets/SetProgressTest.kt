package pl.szczytomania.ui.screens.sets

import org.junit.Assert.assertEquals
import org.junit.Test
import pl.szczytomania.data.model.Peak
import pl.szczytomania.data.model.PeakSet

class SetProgressTest {

    private val peaks = listOf(
        Peak("node/1", "Rysy", 2499, 49.18, 20.09, "Tatry", kgp = true),
        Peak("node/2", "Babia Góra", 1723, 49.57, 19.53, "Beskid Żywiecki", kgp = true),
        Peak("node/3", "Śnieżka", 1603, 50.74, 15.74, "Karkonosze", kgp = true),
    ).associateBy { it.id }

    private val set = PeakSet("kgp", "Korona", "opis", listOf("node/1", "node/2", "node/3"))

    @Test
    fun countsConqueredPeaks() {
        val progress = SetProgress.of(set, peaks, setOf("node/3"))
        assertEquals(3, progress.total)
        assertEquals(1, progress.done)
        assertEquals(33, progress.percent)
        assertEquals(listOf(false, false, true), progress.peaks.map { it.conquered })
    }

    @Test
    fun emptyProgressWithoutConquests() {
        val progress = SetProgress.of(set, peaks, emptySet())
        assertEquals(0, progress.done)
        assertEquals(0f, progress.fraction)
        assertEquals(0, progress.percent)
    }

    @Test
    fun percentRoundsDown() {
        val many = (1..28).map { Peak("node/$it", "P$it", 1000, 50.0, 20.0, "X") }
        val bigSet = PeakSet("s", "S", "o", many.map { it.id })
        val progress = SetProgress.of(bigSet, many.associateBy { it.id }, many.take(27).map { it.id }.toSet())
        assertEquals(96, progress.percent)
    }

    @Test
    fun skipsPeaksMissingFromPeaksFile() {
        val withMissing = set.copy(peakIds = set.peakIds + "node/999")
        assertEquals(3, SetProgress.of(withMissing, peaks, emptySet()).total)
    }

    @Test
    fun filtersKeepSetOrder() {
        val progress = SetProgress.of(set, peaks, setOf("node/1", "node/3"))
        assertEquals(listOf("Rysy", "Babia Góra", "Śnieżka"), progress.peaks.filter(SetFilter.ALL::matches).map { it.peak.name })
        assertEquals(listOf("Rysy", "Śnieżka"), progress.peaks.filter(SetFilter.CONQUERED::matches).map { it.peak.name })
        assertEquals(listOf("Babia Góra"), progress.peaks.filter(SetFilter.TO_CONQUER::matches).map { it.peak.name })
    }
}
