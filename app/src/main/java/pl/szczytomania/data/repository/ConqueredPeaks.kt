package pl.szczytomania.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Id szczytów zdobytych przez użytkownika. TYMCZASOWO pusty zbiór – po dodaniu zapisu zdobyczy
 * w Firestore (Jakub) wystarczy podmienić tu źródło; korzystają z niego wszystkie ekrany.
 */
object ConqueredPeaks {
    val ids: Flow<Set<String>> = flowOf(emptySet())
}
