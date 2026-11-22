package pl.szczytomania.data.firestore

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import pl.szczytomania.data.auth.AuthRepository
import pl.szczytomania.data.model.PeakSet

/** Własny zestaw z Firestore: lista szczytów, właściciel i znajomi, którym go udostępniono. */
data class OwnSet(val set: PeakSet, val ownerId: String, val sharedWith: List<String>)

/**
 * Własne zestawy gór w Firestore: kolekcja `sets` (nazwa, właściciel, lista szczytów, komu udostępniony),
 * zgodnie z modelem danych z karty projektu. Działa też bez zasięgu – zapis czeka w pamięci telefonu.
 */
class OwnSetRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {

    /** Zestawy zalogowanego użytkownika, od najnowszych. Po wylogowaniu – pusta lista. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val ownSets: Flow<List<OwnSet>> = AuthRepository(auth).currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList()) else query(db.collection(SETS).whereEqualTo("ownerId", user.uid))
    }

    /** Zestawy, które znajomi udostępnili zalogowanemu użytkownikowi. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val sharedWithMe: Flow<List<OwnSet>> = AuthRepository(auth).currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList()) else query(db.collection(SETS).whereArrayContains("sharedWith", user.uid))
    }

    /** Zapisuje nowy zestaw. Szczyty w podanej kolejności (od najwyższego). */
    fun create(name: String, peakIds: List<String>, sharedWith: List<String> = emptyList()) {
        val uid = auth.currentUser?.uid ?: return
        db.collection(SETS).add(
            mapOf(
                "name" to name,
                "ownerId" to uid,
                "peakIds" to peakIds,
                "sharedWith" to sharedWith,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).addOnFailureListener { Log.e(TAG, "Nie udało się zapisać zestawu $name", it) }
    }

    /** Zmienia listę znajomych, którym udostępniono zestaw (reguły pozwalają tylko właścicielowi). */
    fun setSharedWith(setId: String, sharedWith: List<String>) {
        db.collection(SETS).document(documentId(setId)).update("sharedWith", sharedWith)
            .addOnFailureListener { Log.e(TAG, "Nie udało się udostępnić zestawu $setId", it) }
    }

    fun delete(setId: String) {
        db.collection(SETS).document(documentId(setId)).delete()
            .addOnFailureListener { Log.e(TAG, "Nie udało się usunąć zestawu $setId", it) }
    }

    private fun query(query: Query): Flow<List<OwnSet>> = callbackFlow {
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Nie udało się odczytać zestawów", error)
                return@addSnapshotListener
            }
            // Sortujemy w aplikacji – zapytanie z sortowaniem wymagałoby osobnego indeksu w konsoli.
            trySend(
                snapshot?.documents.orEmpty()
                    .sortedByDescending { it.getTimestamp("createdAt", ServerTimestampBehavior.ESTIMATE)?.seconds ?: Long.MAX_VALUE }
                    .mapNotNull(::toOwnSet),
            )
        }
        awaitClose { registration.remove() }
    }

    @Suppress("UNCHECKED_CAST")
    private fun toOwnSet(doc: DocumentSnapshot): OwnSet? {
        val peakIds = doc.get("peakIds") as? List<String> ?: return null
        return OwnSet(
            set = PeakSet(id = OWN_PREFIX + doc.id, name = doc.getString("name").orEmpty(), description = "", peakIds = peakIds),
            ownerId = doc.getString("ownerId").orEmpty(),
            sharedWith = doc.get("sharedWith") as? List<String> ?: emptyList(),
        )
    }

    companion object {
        private const val TAG = "OwnSetRepository"
        const val SETS = "sets"

        /** Przedrostek id własnego zestawu w aplikacji – odróżnia je od gotowych ("kgp", "tatry"…). */
        const val OWN_PREFIX = "own:"

        fun isOwn(setId: String) = setId.startsWith(OWN_PREFIX)
        fun documentId(setId: String) = setId.removePrefix(OWN_PREFIX)
    }
}
