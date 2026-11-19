package pl.szczytomania.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import pl.szczytomania.data.firestore.FriendRepository
import pl.szczytomania.data.firestore.Friendship

/** Znajomości zalogowanego użytkownika – wspólne dla menu (licznik zaproszeń), ekranu Znajomi i profilu znajomego. */
object Friends {
    val repository = FriendRepository()
    val friendships: Flow<List<Friendship>> = repository.friendships

    /** Liczba zaproszeń czekających na odpowiedź – czerwone kółko przy "Znajomi" w menu. */
    val incomingCount: Flow<Int> = friendships.map { list -> list.count { it.incoming && !it.accepted } }.distinctUntilChanged()
}
