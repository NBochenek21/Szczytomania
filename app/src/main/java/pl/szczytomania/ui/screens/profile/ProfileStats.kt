package pl.szczytomania.ui.screens.profile

import pl.szczytomania.data.model.Peak
import pl.szczytomania.data.model.PeakSet
import pl.szczytomania.ui.screens.sets.SetProgress

/** Statystyki na profilu (mockup 11_Profil): liczba szczytów, najwyższy szczyt, zestawy. */
data class ProfileStats(
    val conqueredCount: Int,
    /** Najwyższy zdobyty szczyt albo null, gdy użytkownik nie zdobył jeszcze żadnego. */
    val highest: Peak?,
    /** Zestawy, w których zdobyto co najmniej jeden szczyt. */
    val startedSets: Int,
    /** Postęp pokazywany na profilu: wyróżnione zestawy (Korona Gór Polski) i te rozpoczęte. */
    val shownSets: List<SetProgress>,
) {
    companion object {
        fun of(peaksById: Map<String, Peak>, sets: List<PeakSet>, conqueredIds: Set<String>): ProfileStats {
            // Szczyty spoza pliku (np. testowy szczyt z buildu debug) nie wliczają się do statystyk.
            val conquered = conqueredIds.mapNotNull { peaksById[it] }
            val progress = sets.map { SetProgress.of(it, peaksById, conqueredIds) }
            return ProfileStats(
                conqueredCount = conquered.size,
                highest = conquered.maxByOrNull { it.elevation },
                startedSets = progress.count { it.done > 0 },
                shownSets = progress.filter { it.set.badge != null || it.done > 0 },
            )
        }
    }
}

/** Pierwsza litera nicku do awatara, np. "Norbert" -> "N". */
fun avatarInitial(nick: String): String = nick.trim().firstOrNull()?.uppercase() ?: "?"
