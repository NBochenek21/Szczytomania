package pl.szczytomania.ui.screens.sets

import pl.szczytomania.data.model.Peak
import pl.szczytomania.data.model.PeakSet

/** Szczyt w zestawie razem z informacją, czy użytkownik go zdobył. */
data class SetPeak(val peak: Peak, val conquered: Boolean)

/** Zestaw z postępem użytkownika. */
data class SetProgress(val set: PeakSet, val peaks: List<SetPeak>) {
    val total: Int get() = peaks.size
    val done: Int get() = peaks.count { it.conquered }
    val fraction: Float get() = if (total == 0) 0f else done.toFloat() / total
    /** Procent zaokrąglony w dół, żeby 27/28 nie pokazywało 100%. */
    val percent: Int get() = if (total == 0) 0 else done * 100 / total

    companion object {
        /** Łączy zestaw ze szczytami; szczyty, których nie ma w pliku szczytów, są pomijane. */
        fun of(set: PeakSet, peaksById: Map<String, Peak>, conqueredIds: Set<String>) = SetProgress(
            set = set,
            peaks = set.peakIds.mapNotNull { id -> peaksById[id]?.let { SetPeak(it, id in conqueredIds) } },
        )
    }
}

/** Filtr listy w szczegółach zestawu. */
enum class SetFilter {
    ALL,
    CONQUERED,
    TO_CONQUER,
    ;

    fun select(peaks: List<SetPeak>): List<SetPeak> = when (this) {
        ALL -> peaks
        CONQUERED -> peaks.filter { it.conquered }
        TO_CONQUER -> peaks.filterNot { it.conquered }
    }
}
