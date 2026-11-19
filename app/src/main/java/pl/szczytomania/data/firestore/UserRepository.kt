package pl.szczytomania.data.firestore

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

/** Profil użytkownika widoczny dla innych (dokument `users/{uid}`). */
data class UserProfile(
    val nick: String,
    /** Data założenia konta (czas serwera); null, dopóki zapis nie dotrze na serwer. */
    val createdAt: Date?,
)

/** Użytkownik znaleziony w wyszukiwarce znajomych. */
data class UserHit(val uid: String, val nick: String)

/** Nick jest już zajęty przez inną osobę. */
class NickTakenException(nick: String) : Exception("Nick $nick jest zajęty")

/**
 * Profile użytkowników w Firestore: kolekcja `users`, dokument o id = uid z Authentication.
 *
 * Nicki są unikalne: kolekcja `nicks` ma dokument dla każdego zajętego nicku (id = nick małymi literami,
 * pole `uid`). Reguły pozwalają założyć go tylko raz i tylko razem z profilem z tym nickiem, więc
 * rezerwacja i profil zapisują się zawsze razem – jednym zapisem (batch).
 */
class UserRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    /**
     * Sprawdza na serwerze, czy nick jest wolny (albo należy już do [uid]). Wymaga internetu –
     * bez zasięgu rzuca wyjątek, bo z pamięci telefonu nie da się tego rozstrzygnąć.
     */
    suspend fun isNickFree(nick: String, uid: String? = null): Boolean {
        val owner = nicks().document(nickKey(nick)).get(Source.SERVER).await().getString("uid")
        return owner == null || owner == uid
    }

    /**
     * Tworzy profil nowego użytkownika razem z rezerwacją nicku. E-mail i hasło zostają tylko
     * w Authentication – w Firestore trzymamy dane, które mogą zobaczyć znajomi.
     *
     * Nie czekamy na odpowiedź serwera: Firestore od razu zapisuje dane w pamięci podręcznej
     * telefonu i sam wyśle je na serwer, nawet jeśli zasięg zniknie zaraz po rejestracji.
     * Wcześniej rejestracja sprawdza [isNickFree].
     */
    fun createProfile(uid: String, nick: String) {
        db.batch()
            .set(
                users().document(uid),
                mapOf(
                    "nick" to nick,
                    "nickLower" to nickKey(nick),
                    "photoUrl" to null,
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            )
            .set(nicks().document(nickKey(nick)), mapOf("uid" to uid))
            .commit()
            .addOnFailureListener { Log.e(TAG, "Nie udało się zapisać profilu $uid", it) }
    }

    /**
     * Zmiana nicku: rezerwuje nowy, zwalnia stary i zmienia profil – jednym zapisem.
     * Wymaga internetu (trzeba sprawdzić, czy nowy nick jest wolny). Rzuca [NickTakenException].
     */
    suspend fun changeNick(uid: String, nick: String) {
        val newKey = nickKey(nick)
        val oldKey = users().document(uid).get(Source.SERVER).await().getString("nick")?.let(::nickKey)
        val batch = db.batch().update(users().document(uid), mapOf("nick" to nick, "nickLower" to newKey))
        if (newKey != oldKey) {
            if (!isNickFree(nick, uid)) throw NickTakenException(nick)
            batch.set(nicks().document(newKey), mapOf("uid" to uid))
            // Stary nick zwalniamy tylko, jeśli jest nasz (konta sprzed unikalnych nicków mogą go nie mieć).
            if (oldKey != null && nicks().document(oldKey).get(Source.SERVER).await().getString("uid") == uid) {
                batch.delete(nicks().document(oldKey))
            }
        }
        batch.commit().await()
    }

    /**
     * Konta założone przed wyszukiwaniem znajomych nie mają `nickLower` ani rezerwacji nicku –
     * dopisujemy je po zalogowaniu, żeby dało się je znaleźć. Gdy nick zajęła już inna osoba,
     * konto zostaje bez zmian (znajdzie się po zmianie nicku w edycji konta).
     */
    suspend fun ensureSearchable(uid: String) {
        try {
            val profile = users().document(uid).get(Source.SERVER).await()
            val nick = profile.getString("nick") ?: return
            if (profile.getString("nickLower") == nickKey(nick)) return
            if (!isValidNickFormat(nick) || !isNickFree(nick, uid)) {
                Log.w(TAG, "Nie da się zarezerwować nicku $nick – zajęty albo niepoprawny")
                return
            }
            db.batch()
                .update(users().document(uid), "nickLower", nickKey(nick))
                .set(nicks().document(nickKey(nick)), mapOf("uid" to uid))
                .commit()
                .await()
        } catch (e: Exception) {
            // Bez zasięgu spróbujemy przy następnym uruchomieniu.
            Log.w(TAG, "Nie udało się uzupełnić profilu $uid", e)
        }
    }

    /**
     * Wyszukiwanie znajomych: użytkownicy, których nick zaczyna się od [query] (bez względu na wielkość
     * liter), alfabetycznie. Bez zasięgu Firestore odpowie z pamięci telefonu – znajdzie tylko tych,
     * których profile już tam są.
     */
    suspend fun searchByNick(query: String, limit: Long = 10): List<UserHit> {
        val key = nickKey(query)
        if (key.isEmpty()) return emptyList()
        return users()
            .orderBy("nickLower")
            .startAt(key)
            .endAt(key + "")
            .limit(limit)
            .get()
            .await()
            .documents
            .mapNotNull { doc -> doc.getString("nick")?.let { UserHit(doc.id, it) } }
    }

    /**
     * Profil użytkownika na bieżąco (także bez zasięgu – z pamięci podręcznej Firestore).
     * null, gdy profilu nie ma albo nie udało się go odczytać.
     */
    fun profile(uid: String): Flow<UserProfile?> = callbackFlow {
        val registration = users().document(uid).addSnapshotListener { snapshot, error ->
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

    private fun users() = db.collection(USERS)
    private fun nicks() = db.collection(NICKS)

    companion object {
        private const val TAG = "UserRepository"
        const val USERS = "users"
        const val NICKS = "nicks"

        /** Klucz nicku: małe litery – "Kacper" i "kacper" to ten sam nick. Reguły liczą go tak samo. */
        fun nickKey(nick: String) = nick.trim().lowercase()

        private val NICK_FORMAT = Regex("[A-Za-z0-9._-]{3,20}")

        /**
         * Znaki nicku jak w regułach: litery bez polskich znaków, cyfry, kropka, podkreślenie, myślnik
         * (reguły nie umieją zmienić wielkości polskich liter, a bez tego nie sprawdzą unikalności).
         */
        fun isValidNickFormat(nick: String) = NICK_FORMAT.matches(nick.trim())
    }
}
