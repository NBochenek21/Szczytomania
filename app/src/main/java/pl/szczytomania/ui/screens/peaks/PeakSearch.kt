package pl.szczytomania.ui.screens.peaks

import pl.szczytomania.data.model.Peak
import java.text.Normalizer

/** Wyszukiwanie szczytów po nazwie lub paśmie – bez rozróżniania wielkości liter i polskich znaków. */
object PeakSearch {

    private val COMBINING_MARKS = Regex("\\p{Mn}+")

    /** "Śnieżnik" -> "snieznik", "ŁYSICA" -> "lysica". */
    fun normalize(text: String): String =
        Normalizer.normalize(text.trim().lowercase(), Normalizer.Form.NFD)
            .replace(COMBINING_MARKS, "")
            // "ł" nie rozkłada się na literę i znak diakrytyczny, więc zamieniamy ją ręcznie.
            .replace('ł', 'l')

    /** Szczyty pasujące do zapytania, od najwyższego. Puste zapytanie zwraca wszystkie. */
    fun filter(peaks: List<Peak>, query: String): List<Peak> {
        val needle = normalize(query)
        val matching = if (needle.isEmpty()) {
            peaks
        } else {
            peaks.filter { normalize(it.name).contains(needle) || normalize(it.range).contains(needle) }
        }
        return matching.sortedWith(compareByDescending<Peak> { it.elevation }.thenBy { it.name })
    }
}
