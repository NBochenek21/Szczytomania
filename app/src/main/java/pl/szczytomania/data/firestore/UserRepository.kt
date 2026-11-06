package pl.szczytomania.data.firestore

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Date

/** Profil użytkownika widoczny dla innych (dokument `users/{uid}`). */
data class UserProfile(
    val nick: String,
    /** Data założenia konta (czas serwera); null, dopóki zapis nie dotrze na serwer. */
    val createdAt: Date?,
)

/** Profile użytkowników w Firestore: kolekcja `users`, dokument o id = uid z Authentication. */
class UserRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    /**
     * Tworzy profil nowego użytkownika. E-mail i hasło zostają tylko w Authentication –
     * w Firestore trzymamy dane, które mogą zobaczyć znajomi.
     *
     * Nie czekamy na odpowiedź serwera: Firestore od razu zapisuje dane w pamięci podręcznej
     * telefonu i sam wyśle je na serwer, nawet jeśli zasięg zniknie zaraz po rejestracji.
     */
    fun createProfile(uid: String, nick: String) {
        db.collection(USERS).document(uid)
            .set(
                mapOf(
                    "nick" to nick,
                    "photoUrl" to null,
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            )
            .addOnFailureListener { Log.e(TAG, "Nie udało się zapisać profilu $uid", it) }
    }

    /**
     * Profil użytkownika na bieżąco (także bez zasięgu – z pamięci podręcznej Firestore).
     * null, gdy profilu nie ma albo nie udało się go odczytać.
     */
    fun profile(uid: String): Flow<UserProfile?> = callbackFlow {
        val registration = db.collection(USERS).document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Nie udało się odczytać profilu $uid", error)
                trySend(null)
                return@addSnapshotListener
            }
            trySend(
                snapshot?.takeIf { it.exists() }?.let {
                    UserProfile(
                        nick = it.getString("nick").orEmpty(),
                        // Przed potwierdzeniem przez serwer bierzemy szacowany czas zapisu.
                        createdAt = it.getTimestamp("createdAt", ServerTimestampBehavior.ESTIMATE)?.toDate(),
                    )
                },
            )
        }
        awaitClose { registration.remove() }
    }

    companion object {
        private const val TAG = "UserRepository"
        const val USERS = "users"
    }
}
