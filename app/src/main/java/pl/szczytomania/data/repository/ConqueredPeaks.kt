package pl.szczytomania.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Identyfikatory szczytów zdobytych przez zalogowanego użytkownika.
 *
 * TYMCZASOWO zawsze pusty zbiór: zapis zdobyczy w Firestore powstaje w tym tygodniu (Jakub).
 * Gdy będzie gotowy, wystarczy podmienić tu źródło na repozytorium zdobyczy – postęp zestawów,
 * trójkąty na listach i status na karcie szczytu korzystają tylko z tego miejsca.
 */
object ConqueredPeaks {
    val ids: Flow<Set<String>> = flowOf(emptySet())
}
