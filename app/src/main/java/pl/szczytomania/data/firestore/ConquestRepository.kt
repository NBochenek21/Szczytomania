package pl.szczytomania.data.firestore

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import pl.szczytomania.data.auth.AuthRepository
import pl.szczytomania.data.model.Peak
import pl.szczytomania.location.GpsReading

/**
 * Zdobycze w Firestore: podkolekcja `users/{uid}/conquests`. Jeden dokument na szczyt – liczy się
 * pierwsze wejście, tak jak jedna pieczątka na szczyt w "Moich zdobyczach".
 */
class ConquestRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {

    /**
     * Id szczytów zdobytych przez zalogowanego użytkownika, na bieżąco – także bez zasięgu
     * (z pamięci podręcznej Firestore). Idzie za logowaniem: po wylogowaniu daje pusty zbiór,
     * a po zalogowaniu innej osoby jej szczyty.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val conqueredPeakIds: Flow<Set<String>> = AuthRepository(auth).currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptySet()) else peakIdsOf(user.uid)
    }

    /**
     * Zapisuje zdobycz zalogowanego użytkownika. Nie czekamy na odpowiedź serwera: bez zasięgu zapis
     * trafia do pamięci podręcznej telefonu i od razu widać go w aplikacji, a Firestore sam wyśle go
     * po odzyskaniu zasięgu – wtedy serwer dopisze swój czas.
     */
    fun save(peak: Peak, reading: GpsReading) {
        val uid = auth.currentUser?.uid ?: return
        conquests(uid).document(documentId(peak.id))
            .set(
                mapOf(
                    "peakId" to peak.id,
                    // Czas z telefonu: kiedy użytkownik był na szczycie, nawet jeśli zapis dotrze później.
                    "conqueredAt" to Timestamp.now(),
                    "serverTime" to FieldValue.serverTimestamp(),
                    "lat" to reading.lat,
                    "lon" to reading.lon,
                    "accuracy" to reading.accuracyMeters,
                    "photoUrl" to null,
                    "photoStatus" to PHOTO_PENDING,
                ),
            )
            .addOnFailureListener { Log.e(TAG, "Nie udało się zapisać zdobyczy ${peak.id}", it) }
    }

    private fun peakIdsOf(uid: String): Flow<Set<String>> = callbackFlow {
        val registration = conquests(uid).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Nie udało się odczytać zdobyczy $uid", error)
                return@addSnapshotListener
            }
            trySend(snapshot?.documents.orEmpty().mapNotNull { it.getString("peakId") }.toSet())
        }
        awaitClose { registration.remove() }
    }

    private fun conquests(uid: String) = db.collection(UserRepository.USERS).document(uid).collection(CONQUESTS)

    companion object {
        private const val TAG = "ConquestRepository"
        const val CONQUESTS = "conquests"

        /** Zdjęcie czeka na wysłanie. Wysyłanie w tle i status "wysłane" dochodzą w tygodniu 4. */
        const val PHOTO_PENDING = "pending"

        /**
         * Id szczytu z OpenStreetMap ma ukośnik ("node/4366169405"), a ten w ścieżce Firestore
         * oddziela kolekcje od dokumentów. Reguły bezpieczeństwa sprawdzają tę samą zamianę.
         */
        fun documentId(peakId: String) = peakId.replace('/', '_')
    }
}
