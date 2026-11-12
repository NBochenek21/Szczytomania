package pl.szczytomania.data.repository

import kotlinx.coroutines.flow.Flow
import pl.szczytomania.data.firestore.Conquest
import pl.szczytomania.data.firestore.ConquestRepository

/**
 * Zdobycze zalogowanego użytkownika – z Firestore, na bieżąco i także bez zasięgu.
 * Korzystają z nich wszystkie ekrany: mapa, lista szczytów, zestawy, profil i "Moje zdobycze".
 */
object ConqueredPeaks {
    val repository = ConquestRepository()
    val all: Flow<List<Conquest>> = repository.conquests
    val ids: Flow<Set<String>> = repository.conqueredPeakIds
}
