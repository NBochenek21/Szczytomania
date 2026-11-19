package pl.szczytomania.ui.screens.friends

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.szczytomania.data.firestore.Conquest
import pl.szczytomania.data.firestore.FriendRepository
import pl.szczytomania.data.firestore.Friendship
import pl.szczytomania.data.firestore.UserRepository
import pl.szczytomania.data.model.Peak
import pl.szczytomania.data.model.PeakSet
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

class FriendsLogicTest {

    private val zone = ZoneId.of("Europe/Warsaw")
    private fun date(day: Int, hour: Int = 12) = Date.from(LocalDateTime.of(2026, 11, day, hour, 0).atZone(zone).toInstant())

    private val peaks = listOf(
        Peak("node/1", "Turbacz", 1310, 49.54, 20.11, "Gorce", kgp = true),
        Peak("node/2", "Babia Góra", 1725, 49.57, 19.53, "Beskid Żywiecki", kgp = true),
        Peak("node/3", "Mogielica", 1171, 49.65, 20.27, "Beskid Wyspowy", kgp = true),
        Peak("node/4", "Lubań", 1211, 49.48, 20.33, "Gorce"),
    ).associateBy { it.id }
    private val sets = listOf(PeakSet(KGP_SET_ID, "Korona Gór Polski", "", listOf("node/2", "node/1", "node/3")))

    private fun conquest(peakId: String, at: Date) = Conquest(peakId, at, 5.0, "uploaded")
    private fun friendship(uid: String, accepted: Boolean = true, incoming: Boolean = false) =
        Friendship(FriendRepository.pairId("me", uid), uid, accepted, incoming, null)

    @Test
    fun pairIdIsTheSameForBothPeople() {
        assertEquals("abc_xyz", FriendRepository.pairId("xyz", "abc"))
        assertEquals(FriendRepository.pairId("abc", "xyz"), FriendRepository.pairId("xyz", "abc"))
    }

    @Test
    fun nickFormatMatchesFirestoreRules() {
        assertTrue(UserRepository.isValidNickFormat("marta_w_gorach"))
        assertTrue(UserRepository.isValidNickFormat("kasia.szlaki"))
        assertFalse(UserRepository.isValidNickFormat("Łukasz"))
        assertFalse(UserRepository.isValidNickFormat("ala ma"))
        assertFalse(UserRepository.isValidNickFormat("a/b/c"))
        assertFalse(UserRepository.isValidNickFormat("ab"))
        assertEquals("kacper", UserRepository.nickKey(" Kacper "))
    }

    @Test
    fun daysAgoCountsCalendarDays() {
        val now = date(19, hour = 9)
        assertEquals(0, daysAgo(date(19, hour = 1), now, zone))
        // Wczoraj wieczorem to "wczoraj", choć minęło mniej niż 24 godziny.
        assertEquals(1, daysAgo(date(18, hour = 22), now, zone))
        assertEquals(3, daysAgo(date(16), now, zone))
    }

    @Test
    fun recentShowsNewestConquestsOfAcceptedFriendsOnly() {
        val entries = listOf(
            FriendEntry(friendship("ola"), "Ola", listOf(conquest("node/1", date(10)), conquest("node/2", date(15)))),
            FriendEntry(friendship("kuba"), "Kuba", listOf(conquest("node/3", date(18)), conquest("test/x", date(19)))),
            // Zaproszenie od Marty – jej zdobyczy nie ma w "Ostatnio na szczytach".
            FriendEntry(friendship("marta", accepted = false, incoming = true), "marta", listOf(conquest("node/4", date(19)))),
        )
        val recent = recentConquests(entries, peaks, limit = 2)
        assertEquals(listOf("Mogielica" to "Kuba", "Babia Góra" to "Ola"), recent.map { it.peak.name to it.nick })
    }

    @Test
    fun relationDecidesTheSearchButton() {
        val list = listOf(friendship("ola"), friendship("kuba", accepted = false), friendship("marta", accepted = false, incoming = true))
        assertEquals(Relation.FRIEND, relationTo("ola", list).first)
        assertEquals(Relation.INVITED, relationTo("kuba", list).first)
        assertEquals(Relation.INVITES_YOU, relationTo("marta", list).first)
        assertEquals(Relation.NONE, relationTo("obcy", list).first)
        assertNull(relationTo("obcy", list).second)
    }

    @Test
    fun summaryCountsKnownPeaksAndCrown() {
        val summary = FriendSummary.of(listOf(conquest("node/1", date(1)), conquest("node/4", date(2)), conquest("test/x", date(3))), peaks, sets)
        assertEquals(2, summary.count)
        assertEquals(1, summary.kgp?.done)
        assertEquals(3, summary.kgp?.total)
    }

    @Test
    fun comparisonShowsCommonPeaksAndWhoLeads() {
        val friend = listOf(conquest("node/1", date(10)), conquest("node/2", date(12)), conquest("node/4", date(14)))
        val c = FriendComparison.of(friend, myPeakIds = setOf("node/1", "node/4"), peaksById = peaks, sets = sets)
        assertEquals(3, c.count)
        assertEquals("Babia Góra", c.highest?.name)
        assertEquals(2, c.common)
        // Znajomy: Turbacz i Babia Góra z Korony, Ty: tylko Turbacz – prowadzi o 1.
        assertEquals(1, c.kgpLead)
        assertEquals(listOf("Lubań", "Babia Góra", "Turbacz"), c.conquests.map { it.peak.name })
        assertEquals(listOf(true, false, true), c.conquests.map { it.youToo })
    }
}
