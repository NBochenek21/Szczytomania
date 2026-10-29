package pl.szczytomania.data.firestore

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

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

    companion object {
        private const val TAG = "UserRepository"
        const val USERS = "users"
    }
}
