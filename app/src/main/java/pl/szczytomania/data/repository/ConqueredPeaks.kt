package pl.szczytomania.data.repository

import kotlinx.coroutines.flow.Flow
import pl.szczytomania.data.firestore.ConquestRepository

/**
 * Id szczytów zdobytych przez zalogowanego użytkownika – z Firestore, na bieżąco i także bez zasięgu.
 * Korzystają z niego wszystkie ekrany: mapa, lista szczytów, zestawy i profil.
 */
object ConqueredPeaks {
    val ids: Flow<Set<String>> = ConquestRepository().conqueredPeakIds
}
