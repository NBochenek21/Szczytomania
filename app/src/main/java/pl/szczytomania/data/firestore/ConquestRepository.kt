package pl.szczytomania.data.firestore

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import pl.szczytomania.data.auth.AuthRepository
import pl.szczytomania.data.model.Peak
import pl.szczytomania.location.GpsReading
import java.util.Date

/** Zdobycz z Firestore – tyle, ile pokazują ekrany "Moje zdobycze" i "Szczyt zdobyty". */
data class Conquest(
    val peakId: String,
    /** Czas z telefonu – kiedy użytkownik był na szczycie. */
    val conqueredAt: Date,
    val accuracyMeters: Double?,
    /** [ConquestRepository.PHOTO_PENDING] albo [ConquestRepository.PHOTO_UPLOADED]. */
    val photoStatus: String?,
    /** Link do zdjęcia ze szczytu; null, dopóki zdjęcie nie zostanie wysłane. */
    val photoUrl: String? = null,
    /**
     * false, dopóki zapis czeka w telefonie na wysłanie (np. w górach bez zasięgu).
     * Firestore wyśle go sam po odzyskaniu sieci – wtedy zmieni się na true.
     */
    val synced: Boolean = true,
)

/**
 * Zdobycze w Firestore: podkolekcja `users/{uid}/conquests`. Jeden dokument na szczyt – liczy się
 * pierwsze wejście, tak jak jedna pieczątka na szczyt w "Moich zdobyczach".
 *
 * Bez zasięgu wszystko działa na pamięci podręcznej Firestore w telefonie (włączonej domyślnie):
 * zapisy czekają tam na sieć, a odczyty pokazują dane z telefonu razem z niewysłanymi zapisami.
 */
class ConquestRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {

    /**
     * Zdobycze zalogowanego użytkownika, na bieżąco – także bez zasięgu (z pamięci podręcznej
     * Firestore). Idzie za logowaniem: po wylogowaniu pusta lista, po zalogowaniu innej osoby jej zdobycze.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val conquests: Flow<List<Conquest>> = AuthRepository(auth).currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList()) else conquestsOf(user.uid)
    }

    /** Id zdobytych szczytów – dla ekranów, którym wystarczy wiedzieć, co zdobyto. */
    val conqueredPeakIds: Flow<Set<String>> =
        conquests.map { list -> list.mapTo(HashSet()) { it.peakId } }.distinctUntilChanged()

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

    /**
     * Po wysłaniu zdjęcia (WorkManager) zapisuje jego link i status "wysłane". Tak jak [save] nie czeka
     * na serwer – zmiana czeka w telefonie, a Firestore wyśle ją sam, nawet po restarcie aplikacji.
     */
    fun markPhotoUploaded(peakId: String, photoUrl: String) {
        val uid = auth.currentUser?.uid ?: return
        conquests(uid).document(documentId(peakId))
            .update(mapOf("photoUrl" to photoUrl, "photoStatus" to PHOTO_UPLOADED))
            .addOnFailureListener { Log.e(TAG, "Nie udało się zapisać zdjęcia zdobyczy $peakId", it) }
    }

    private fun conquestsOf(uid: String): Flow<List<Conquest>> = callbackFlow {
        // MetadataChanges.INCLUDE: nasłuch odzywa się także wtedy, gdy serwer potwierdzi zapis
        // (synced: false → true), choć same dane zdobyczy się nie zmieniły.
        val registration = conquests(uid).addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Nie udało się odczytać zdobyczy $uid", error)
                return@addSnapshotListener
            }
            trySend(
                snapshot?.documents.orEmpty().mapNotNull { doc ->
                    Conquest(
                        peakId = doc.getString("peakId") ?: return@mapNotNull null,
                        conqueredAt = doc.getTimestamp("conqueredAt")?.toDate() ?: Date(0),
                        accuracyMeters = doc.getDouble("accuracy"),
                        photoStatus = doc.getString("photoStatus"),
                        photoUrl = doc.getString("photoUrl"),
                        // Zapis zrobiony w telefonie, którego serwer jeszcze nie potwierdził.
                        synced = !doc.metadata.hasPendingWrites(),
                    )
                },
            )
        }
        awaitClose { registration.remove() }
    }.distinctUntilChanged()

    private fun conquests(uid: String) = db.collection(UserRepository.USERS).document(uid).collection(CONQUESTS)

    companion object {
        private const val TAG = "ConquestRepository"
        const val CONQUESTS = "conquests"

        /** Zdjęcie czeka w telefonie na wysłanie. */
        const val PHOTO_PENDING = "pending"

        /** Zdjęcie jest na serwerze, a jego link w `photoUrl`. */
        const val PHOTO_UPLOADED = "uploaded"

        /**
         * Id szczytu z OpenStreetMap ma ukośnik ("node/4366169405"), a ten w ścieżce Firestore
         * oddziela kolekcje od dokumentów. Reguły bezpieczeństwa sprawdzają tę samą zamianę.
         */
        fun documentId(peakId: String) = peakId.replace('/', '_')
    }
}
