package pl.szczytomania.data.firestore

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import pl.szczytomania.data.auth.AuthRepository
import pl.szczytomania.data.model.PeakSet

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
    val ownSets: Flow<List<PeakSet>> = AuthRepository(auth).currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList()) else setsOf(user.uid)
    }

    /** Zapisuje nowy zestaw. Szczyty w podanej kolejności (od najwyższego). */
    fun create(name: String, peakIds: List<String>) {
        val uid = auth.currentUser?.uid ?: return
        db.collection(SETS).add(
            mapOf(
                "name" to name,
                "ownerId" to uid,
                "peakIds" to peakIds,
                "sharedWith" to emptyList<String>(),
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).addOnFailureListener { Log.e(TAG, "Nie udało się zapisać zestawu $name", it) }
    }

    fun delete(setId: String) {
        db.collection(SETS).document(setId).delete()
            .addOnFailureListener { Log.e(TAG, "Nie udało się usunąć zestawu $setId", it) }
    }

    private fun setsOf(uid: String): Flow<List<PeakSet>> = callbackFlow {
        val registration = db.collection(SETS)
            .whereEqualTo("ownerId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Nie udało się odczytać zestawów $uid", error)
                    return@addSnapshotListener
                }
                trySend(
                    // Sortujemy w aplikacji – zapytanie z sortowaniem wymagałoby osobnego indeksu w konsoli.
                    snapshot?.documents.orEmpty()
                        .sortedByDescending { it.getTimestamp("createdAt", ServerTimestampBehavior.ESTIMATE)?.seconds ?: Long.MAX_VALUE }
                        .mapNotNull { doc ->
                        @Suppress("UNCHECKED_CAST")
                        val peakIds = doc.get("peakIds") as? List<String> ?: return@mapNotNull null
                        PeakSet(id = OWN_PREFIX + doc.id, name = doc.getString("name").orEmpty(), description = "", peakIds = peakIds)
                    },
                )
            }
        awaitClose { registration.remove() }
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
