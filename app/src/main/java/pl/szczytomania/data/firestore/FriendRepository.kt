package pl.szczytomania.data.firestore

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import pl.szczytomania.data.auth.AuthRepository
import java.util.Date

/** Znajomość zalogowanego użytkownika z inną osobą – zaproszenie albo zaakceptowani znajomi. */
data class Friendship(
    /** Id dokumentu – [FriendRepository.pairId] obu osób. */
    val id: String,
    /** Druga osoba. */
    val friendUid: String,
    val accepted: Boolean,
    /** true – zaprosiła nas druga osoba, false – my ją. */
    val incoming: Boolean,
    /** Od kiedy jesteście znajomymi; null dla zaproszeń (i zanim serwer potwierdzi akceptację). */
    val since: Date?,
)

/**
 * Znajomi w Firestore: kolekcja `friendships`, jeden dokument na parę osób (karta projektu:
 * "dwie osoby, status – zaproszenie / zaakceptowane"). Pola: `users` (oba uid), `from`, `to`,
 * `status` (`pending` / `accepted`), `createdAt`, `acceptedAt`.
 *
 * Tak jak zdobycze – zapisy nie czekają na serwer, więc działają też bez zasięgu.
 */
class FriendRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {

    /** Znajomości zalogowanego użytkownika na bieżąco: zaproszenia (w obie strony) i znajomi. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val friendships: Flow<List<Friendship>> = AuthRepository(auth).currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList()) else friendshipsOf(user.uid)
    }

    /** Wysyła zaproszenie. Reguły odrzucą je, jeśli ta para ma już dokument (zaproszenie albo znajomość). */
    fun invite(friendUid: String) {
        val uid = auth.currentUser?.uid ?: return
        friendships().document(pairId(uid, friendUid))
            .set(
                mapOf(
                    "users" to listOf(uid, friendUid),
                    "from" to uid,
                    "to" to friendUid,
                    "status" to PENDING,
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            )
            .addOnFailureListener { Log.e(TAG, "Nie udało się zaprosić $friendUid", it) }
    }

    /** Akceptuje zaproszenie – może tylko zaproszona osoba. */
    fun accept(friendship: Friendship) {
        friendships().document(friendship.id)
            .update(mapOf("status" to ACCEPTED, "acceptedAt" to FieldValue.serverTimestamp()))
            .addOnFailureListener { Log.e(TAG, "Nie udało się zaakceptować ${friendship.id}", it) }
    }

    /** Odrzuca lub anuluje zaproszenie albo usuwa ze znajomych – usunięcie dokumentu. */
    fun remove(friendship: Friendship) {
        friendships().document(friendship.id).delete()
            .addOnFailureListener { Log.e(TAG, "Nie udało się usunąć ${friendship.id}", it) }
    }

    private fun friendshipsOf(uid: String): Flow<List<Friendship>> = callbackFlow {
        // Reguły pozwalają czytać tylko własne znajomości – zapytanie musi to zawierać ("users" zawiera uid).
        val registration = friendships().whereArrayContains("users", uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Nie udało się odczytać znajomych $uid", error)
                return@addSnapshotListener
            }
            trySend(
                snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val from = doc.getString("from") ?: return@mapNotNull null
                    val to = doc.getString("to") ?: return@mapNotNull null
                    val accepted = doc.getString("status") == ACCEPTED
                    Friendship(
                        id = doc.id,
                        friendUid = if (from == uid) to else from,
                        accepted = accepted,
                        incoming = to == uid,
                        since = if (accepted) doc.getTimestamp("acceptedAt", ServerTimestampBehavior.ESTIMATE)?.toDate() else null,
                    )
                },
            )
        }
        awaitClose { registration.remove() }
    }.distinctUntilChanged()

    private fun friendships() = db.collection(FRIENDSHIPS)

    companion object {
        private const val TAG = "FriendRepository"
        const val FRIENDSHIPS = "friendships"
        const val PENDING = "pending"
        const val ACCEPTED = "accepted"

        /** Id dokumentu pary: uid posortowane i połączone "_" – tak samo liczą je reguły. */
        fun pairId(a: String, b: String) = if (a < b) "${a}_$b" else "${b}_$a"
    }
}
