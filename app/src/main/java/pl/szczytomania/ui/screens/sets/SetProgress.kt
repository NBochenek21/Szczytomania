package pl.szczytomania.ui.screens.sets

import androidx.annotation.StringRes
import pl.szczytomania.R
import pl.szczytomania.data.model.Peak
import pl.szczytomania.data.model.PeakSet
import pl.szczytomania.data.repository.PeakRepository

/** Szczyty (po id) i gotowe zestawy – wspólne dane ekranu zestawów i profilu. */
class SetsData(val peaksById: Map<String, Peak>, val sets: List<PeakSet>)

suspend fun PeakRepository.loadSetsData() = SetsData(getAll().associateBy { it.id }, getSets())

/** Szczyt w zestawie razem z informacją, czy użytkownik go zdobył. */
data class SetPeak(val peak: Peak, val conquered: Boolean)

/** Zestaw z postępem użytkownika. */
data class SetProgress(val set: PeakSet, val peaks: List<SetPeak>) {
    val total = peaks.size
    val done = peaks.count { it.conquered }
    val fraction = if (total == 0) 0f else done.toFloat() / total
    /** Procent zaokrąglony w dół, żeby 27/28 nie pokazywało 100%. */
    val percent = if (total == 0) 0 else done * 100 / total

    companion object {
        /** Łączy zestaw ze szczytami; szczyty, których nie ma w pliku szczytów, są pomijane. */
        fun of(set: PeakSet, peaksById: Map<String, Peak>, conqueredIds: Set<String>) = SetProgress(
            set = set,
            peaks = set.peakIds.mapNotNull { id -> peaksById[id]?.let { SetPeak(it, id in conqueredIds) } },
        )
    }
}

/** Filtr listy w szczegółach zestawu. */
enum class SetFilter(@param:StringRes val label: Int) {
    ALL(R.string.set_filter_all),
    CONQUERED(R.string.set_filter_conquered),
    TO_CONQUER(R.string.set_filter_to_conquer),
    ;

    fun matches(peak: SetPeak) = when (this) {
        ALL -> true
        CONQUERED -> peak.conquered
        TO_CONQUER -> !peak.conquered
    }
}
