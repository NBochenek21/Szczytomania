package pl.szczytomania.ui.screens.conquest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.szczytomania.data.firestore.Conquest
import pl.szczytomania.data.model.Peak
import pl.szczytomania.data.model.PeakSet
import pl.szczytomania.ui.screens.sets.SetsData
import java.util.Calendar
import java.util.Date

class ConquestSummaryTest {

    private val rysy = Peak("node/1", "Rysy", 2499, 49.18, 20.09, "Tatry", kgp = true)
    private val babia = Peak("node/2", "Babia Góra", 1723, 49.57, 19.53, "Beskid Żywiecki", kgp = true)
    private val giewont = Peak("node/3", "Giewont", 1895, 49.25, 19.93, "Tatry")
    private val data = SetsData(
        peaksById = listOf(rysy, babia, giewont).associateBy { it.id },
        sets = listOf(PeakSet("kgp", "Korona Gór Polski", "opis", listOf("node/1", "node/2"), "Odznaka")),
    )
    private val now = Date()

    @Test
    fun firstConquestIsNumberOneButNotRecord() {
        val summary = ConquestSummary.of(babia, data, emptySet(), now)
        assertEquals(1, summary.number)
        assertFalse(summary.isRecord)
    }

    @Test
    fun higherThanAllPreviousIsRecord() {
        val summary = ConquestSummary.of(rysy, data, setOf("node/2", "node/3"), now)
        assertEquals(3, summary.number)
        assertTrue(summary.isRecord)
        assertFalse(ConquestSummary.of(babia, data, setOf("node/3"), now).isRecord)
    }

    @Test
    fun countsNewPeakInSetAndMarksItsPlace() {
        val summary = ConquestSummary.of(babia, data, setOf("node/1"), now)
        assertEquals(2, summary.set?.done)
        assertEquals(1, summary.indexInSet)
    }

    @Test
    fun peakOutsideSetsHasNoSet() {
        val summary = ConquestSummary.of(giewont, data, emptySet(), now)
        assertNull(summary.set)
        assertEquals(-1, summary.indexInSet)
    }

    @Test
    fun groupsByMonthFromNewest() {
        fun entry(peak: Peak, month: Int, day: Int) = ConquestEntry(
            peak,
            Conquest(peak.id, Calendar.getInstance().apply { set(2026, month, day, 12, 0) }.time, 9.0, "pending"),
        )
        val groups = groupByMonth(
            listOf(entry(babia, Calendar.SEPTEMBER, 3), entry(rysy, Calendar.OCTOBER, 18), entry(giewont, Calendar.SEPTEMBER, 27)),
        )
        assertEquals(2, groups.size)
        assertEquals(listOf("Rysy"), groups[0].second.map { it.peak.name })
        assertEquals(listOf("Giewont", "Babia Góra"), groups[1].second.map { it.peak.name })
    }
}
