package pl.szczytomania.ui.screens.conquest

import pl.szczytomania.data.firestore.Conquest
import pl.szczytomania.data.model.Peak
import pl.szczytomania.ui.screens.sets.SetProgress
import pl.szczytomania.ui.screens.sets.SetsData
import java.util.Calendar
import java.util.Date

/** Dane ekranu "Szczyt zdobyty!" (mockup 06_Szczyt_zdobyty). */
data class ConquestSummary(
    val peak: Peak,
    val date: Date,
    /** Który to szczyt użytkownika (13. = "To Twój 13. szczyt"). */
    val number: Int,
    /** Wyższy niż wszystkie wcześniej zdobyte. */
    val isRecord: Boolean,
    /** Zestaw, do którego należy szczyt (Korona Gór Polski ma pierwszeństwo – jest pierwsza w pliku). */
    val set: SetProgress?,
    /** Miejsce nowego szczytu w zestawie – ten kwadracik jest wyróżniony. */
    val indexInSet: Int,
) {
    companion object {
        fun of(peak: Peak, data: SetsData, previousIds: Set<String>, date: Date): ConquestSummary {
            val previous = previousIds.mapNotNull { data.peaksById[it] }.filter { it.id != peak.id }
            val set = data.sets.firstOrNull { peak.id in it.peakIds }
                ?.let { SetProgress.of(it, data.peaksById, previousIds + peak.id) }
            return ConquestSummary(
                peak = peak,
                date = date,
                number = previous.size + 1,
                isRecord = previous.isNotEmpty() && previous.all { it.elevation < peak.elevation },
                set = set,
                indexInSet = set?.peaks?.indexOfFirst { it.peak.id == peak.id } ?: -1,
            )
        }
    }
}

/** Zdobycz razem ze szczytem – pozycja w "Moich zdobyczach". */
data class ConquestEntry(val peak: Peak, val conquest: Conquest)

/** Zdobycze od najnowszych, pogrupowane po miesiącach (klucz: pierwszy dzień miesiąca). */
fun groupByMonth(entries: List<ConquestEntry>): List<Pair<Date, List<ConquestEntry>>> =
    entries.sortedByDescending { it.conquest.conqueredAt }
        .groupBy { monthStart(it.conquest.conqueredAt) }
        .toList()

private fun monthStart(date: Date): Date = Calendar.getInstance().apply {
    time = date
    set(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.time
