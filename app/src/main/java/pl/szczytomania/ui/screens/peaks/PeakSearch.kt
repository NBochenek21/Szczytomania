package pl.szczytomania.ui.screens.peaks

import pl.szczytomania.data.model.Peak
import java.text.Collator
import java.text.Normalizer
import java.util.Locale

/** Kolejność listy szczytów – przyciski "Sortuj" z mockupu. */
enum class PeakSort {
    /** Od najwyższego (domyślnie). */
    ELEVATION,
    /** Alfabetycznie, po polsku: Łysica po Lubomirze, Śnieżka po Szrenicy. */
    NAME,
    /** Alfabetycznie po paśmie, w paśmie od najwyższego. */
    RANGE,
}

/** Wyszukiwanie szczytów po nazwie lub paśmie – bez rozróżniania wielkości liter i polskich znaków. */
object PeakSearch {

    private val COMBINING_MARKS = Regex("\\p{Mn}+")

    // Collator nie jest bezpieczny dla wielu wątków naraz – każdy wątek dostaje własny.
    private val polishCollator = ThreadLocal.withInitial { Collator.getInstance(Locale.forLanguageTag("pl")) }

    /** "Śnieżnik" -> "snieznik", "ŁYSICA" -> "lysica". */
    fun normalize(text: String): String =
        Normalizer.normalize(text.trim().lowercase(), Normalizer.Form.NFD)
            .replace(COMBINING_MARKS, "")
            // "ł" nie rozkłada się na literę i znak diakrytyczny, więc zamieniamy ją ręcznie.
            .replace('ł', 'l')

    /** Szczyty pasujące do zapytania w wybranej kolejności. Puste zapytanie zwraca wszystkie. */
    fun filter(peaks: List<Peak>, query: String, sort: PeakSort = PeakSort.ELEVATION): List<Peak> {
        val needle = normalize(query)
        val matching = if (needle.isEmpty()) {
            peaks
        } else {
            peaks.filter { normalize(it.name).contains(needle) || normalize(it.range).contains(needle) }
        }
        return matching.sortedWith(comparator(sort))
    }

    private fun comparator(sort: PeakSort): Comparator<Peak> {
        val collator = polishCollator.get()
        val byName = Comparator<Peak> { a, b -> collator.compare(a.name, b.name) }
        val byElevation = compareByDescending<Peak> { it.elevation }
        return when (sort) {
            PeakSort.ELEVATION -> byElevation.then(byName)
            PeakSort.NAME -> byName.then(byElevation)
            PeakSort.RANGE -> Comparator<Peak> { a, b -> collator.compare(a.range, b.range) }
                .then(byElevation)
                .then(byName)
        }
    }
}
