package pl.szczytomania.ui.screens.challenges

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.szczytomania.data.firestore.Challenge
import pl.szczytomania.data.firestore.ChallengeMode
import pl.szczytomania.data.firestore.ChallengeScore
import pl.szczytomania.data.firestore.Conquest
import java.util.Date

class ChallengeLogicTest {

    private val start = Date(1_000_000)

    private fun challenge(mode: ChallengeMode, endsAt: Date? = null, scores: Map<String, ChallengeScore> = emptyMap()) = Challenge(
        id = "c1",
        ownerId = "me",
        setName = "Gorce",
        peakIds = listOf("node/1", "node/2", "node/3"),
        mode = mode,
        participants = listOf("me", "ola", "jakub"),
        createdAt = start,
        endsAt = endsAt,
        scores = scores,
    )

    private fun conquest(peakId: String, at: Long) = Conquest(peakId, Date(at), 5.0, null)

    @Test
    fun onlyPeaksOfTheSetConqueredDuringTheChallengeCount() {
        val c = challenge(ChallengeMode.MOST, endsAt = Date(5_000_000))
        val score = scoreFor(
            c,
            listOf(
                conquest("node/1", 500_000), // przed startem
                conquest("node/2", 2_000_000),
                conquest("node/9", 2_000_000), // spoza zestawu
                conquest("node/3", 6_000_000), // po końcu
            ),
        )
        assertEquals(ChallengeScore(1, null), score)
    }

    @Test
    fun wholeSetIsCompletedAtTheLastMissingPeak() {
        val score = scoreFor(
            challenge(ChallengeMode.FIRST),
            listOf(conquest("node/1", 2_000_000), conquest("node/3", 4_000_000), conquest("node/2", 3_000_000)),
        )
        assertEquals(3, score.done)
        assertEquals(Date(4_000_000), score.completedAt)
    }

    @Test
    fun firstToFinishWinsAndTiesShareAPlace() {
        val c = challenge(
            ChallengeMode.FIRST,
            scores = mapOf(
                "me" to ChallengeScore(2, null),
                "ola" to ChallengeScore(3, Date(3_000_000)),
                "jakub" to ChallengeScore(2, null),
            ),
        )
        val rows = ranking(c, "me", mapOf("ola" to "Ola", "jakub" to "Jakub"))
        assertEquals(listOf("ola", "jakub", "me"), rows.map { it.uid })
        assertEquals(listOf(1, 2, 2), rows.map { it.place })
        assertTrue(rows.last().me)
        assertNull(rows.last().nick)
        assertTrue(isFinished(c))
    }

    @Test
    fun mostPeaksRaceEndsOnItsDate() {
        val c = challenge(ChallengeMode.MOST, endsAt = Date(5_000_000), scores = mapOf("jakub" to ChallengeScore(1, null)))
        assertEquals("jakub", ranking(c, "me", emptyMap()).first().uid)
        assertFalse(isFinished(c, now = Date(4_999_999)))
        assertTrue(isFinished(c, now = Date(5_000_000)))
    }
}
