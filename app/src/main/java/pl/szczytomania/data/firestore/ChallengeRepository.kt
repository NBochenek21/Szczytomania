package pl.szczytomania.data.firestore

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import pl.szczytomania.data.auth.AuthRepository
import java.util.Date

/** Rodzaj wyścigu z karty projektu: kto pierwszy zdobędzie cały zestaw / kto zdobędzie więcej do daty. */
enum class ChallengeMode(val key: String) {
    FIRST("first"),
    MOST("most"),
    ;

    companion object {
        fun of(key: String?) = entries.firstOrNull { it.key == key }
    }
}

/** Wynik jednej osoby: ile szczytów zestawu zdobyła od startu wyzwania i kiedy skończyła cały zestaw. */
data class ChallengeScore(val done: Int, val completedAt: Date?)

/** Wyzwanie (wyścig) ze znajomymi na wybrany zestaw. */
data class Challenge(
    val id: String,
    val ownerId: String,
    val setName: String,
    /** Szczyty zestawu zapisane w wyzwaniu – uczestnicy nie muszą mieć dostępu do własnego zestawu właściciela. */
    val peakIds: List<String>,
    val mode: ChallengeMode,
    val participants: List<String>,
    val createdAt: Date,
    /** Koniec wyścigu "kto więcej do daty"; w "kto pierwszy" null. */
    val endsAt: Date?,
    /** Wyniki po uid; każdy uczestnik zapisuje tylko swój. */
    val scores: Map<String, ChallengeScore>,
)

/**
 * Wyzwania w Firestore: kolekcja `challenges`. Widzą je tylko uczestnicy. Wynik każdej osoby
 * zapisuje jej telefon (na podstawie jej zdobyczy), bo zdobycze innych uczestników nie zawsze są
 * dla nas widoczne – nie każdy uczestnik musi być naszym znajomym.
 */
class ChallengeRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {

    /** Wyzwania zalogowanego użytkownika (założone przez niego i te, do których go zaproszono), od najnowszych. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val challenges: Flow<List<Challenge>> = AuthRepository(auth).currentUser.flatMapLatest { user ->
        if (user == null) {
            flowOf(emptyList())
        } else {
            callbackFlow {
                val registration = db.collection(CHALLENGES).whereArrayContains("participants", user.uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            // Np. reguły jeszcze nieopublikowane – pusta lista zamiast wiecznego ładowania.
                            Log.w(TAG, "Nie udało się odczytać wyzwań", error)
                            trySend(emptyList())
                            return@addSnapshotListener
                        }
                        trySend(snapshot?.documents.orEmpty().mapNotNull(::toChallenge).sortedByDescending { it.createdAt })
                    }
                awaitClose { registration.remove() }
            }
        }
    }

    /** Zakłada wyzwanie. Właściciel jest zawsze uczestnikiem. */
    fun create(setName: String, peakIds: List<String>, mode: ChallengeMode, endsAt: Date?, friends: List<String>) {
        val uid = auth.currentUser?.uid ?: return
        db.collection(CHALLENGES).add(
            mapOf(
                "ownerId" to uid,
                "setName" to setName,
                "peakIds" to peakIds,
                "mode" to mode.key,
                "participants" to (listOf(uid) + friends).distinct(),
                "createdAt" to FieldValue.serverTimestamp(),
                "endsAt" to endsAt?.let(::Timestamp),
                "scores" to emptyMap<String, Any>(),
            ),
        ).addOnFailureListener { Log.e(TAG, "Nie udało się założyć wyzwania", it) }
    }

    /** Zapisuje wynik zalogowanego użytkownika (reguły pozwalają zmienić tylko własny). */
    fun saveMyScore(challengeId: String, score: ChallengeScore) {
        val uid = auth.currentUser?.uid ?: return
        db.collection(CHALLENGES).document(challengeId)
            .update("scores.$uid", mapOf("done" to score.done, "completedAt" to score.completedAt?.let(::Timestamp)))
            .addOnFailureListener { Log.e(TAG, "Nie udało się zapisać wyniku w wyzwaniu $challengeId", it) }
    }

    /** Usuwa wyzwanie (tylko właściciel). */
    fun delete(challengeId: String) {
        db.collection(CHALLENGES).document(challengeId).delete()
            .addOnFailureListener { Log.e(TAG, "Nie udało się usunąć wyzwania $challengeId", it) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun toChallenge(doc: DocumentSnapshot): Challenge? {
        val scores = (doc.get("scores") as? Map<String, Map<String, Any?>>).orEmpty().mapValues { (_, v) ->
            ChallengeScore(
                done = (v["done"] as? Number)?.toInt() ?: 0,
                completedAt = (v["completedAt"] as? Timestamp)?.toDate(),
            )
        }
        return Challenge(
            id = doc.id,
            ownerId = doc.getString("ownerId") ?: return null,
            setName = doc.getString("setName").orEmpty(),
            peakIds = doc.get("peakIds") as? List<String> ?: return null,
            mode = ChallengeMode.of(doc.getString("mode")) ?: return null,
            participants = doc.get("participants") as? List<String> ?: return null,
            // Tuż po założeniu (przed odpowiedzią serwera) – czas szacowany w telefonie.
            createdAt = doc.getTimestamp("createdAt", ServerTimestampBehavior.ESTIMATE)?.toDate() ?: Date(),
            endsAt = doc.getTimestamp("endsAt")?.toDate(),
            scores = scores,
        )
    }

    companion object {
        private const val TAG = "ChallengeRepository"
        const val CHALLENGES = "challenges"

        /** Najwięcej osób w jednym wyzwaniu (razem z właścicielem) – tyle samo sprawdzają reguły. */
        const val MAX_PARTICIPANTS = 10
    }
}
