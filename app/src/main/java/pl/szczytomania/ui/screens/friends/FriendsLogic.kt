package pl.szczytomania.ui.screens.friends

import pl.szczytomania.data.firestore.Conquest
import pl.szczytomania.data.firestore.Friendship
import pl.szczytomania.data.model.Peak
import pl.szczytomania.data.model.PeakSet
import pl.szczytomania.ui.screens.sets.SetProgress
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date

/** Id zestawu Korona Gór Polski w `sets.json` – jego postęp pokazujemy przy znajomych. */
const val KGP_SET_ID = "kgp"

/** Znajomość razem z nickiem i zdobyczami drugiej osoby. */
data class FriendEntry(
    val friendship: Friendship,
    /** null, dopóki profil się nie wczyta. */
    val nick: String?,
    /** null, gdy nie widzimy zdobyczy (wysłane przez nas zaproszenie jeszcze nie przyjęte). */
    val conquests: List<Conquest>?,
)

/** Liczba szczytów i postęp w Koronie Gór Polski – w wierszu znajomego i na zaproszeniu. */
data class FriendSummary(val count: Int, val kgp: SetProgress?) {
    companion object {
        fun of(conquests: List<Conquest>, peaksById: Map<String, Peak>, sets: List<PeakSet>): FriendSummary {
            // Jak w statystykach profilu: liczą się tylko szczyty z pliku szczytów.
            val ids = conquests.mapTo(HashSet()) { it.peakId }.filterTo(HashSet()) { it in peaksById }
            return FriendSummary(
                count = ids.size,
                kgp = sets.find { it.id == KGP_SET_ID }?.let { SetProgress.of(it, peaksById, ids) },
            )
        }
    }
}

/** Wpis "Ostatnio na szczytach": kto, jaki szczyt i kiedy. */
data class RecentConquest(val friendUid: String, val nick: String, val peak: Peak, val date: Date)

/** Najnowsze zdobycze znajomych (tylko zaakceptowanych), od najnowszej. */
fun recentConquests(entries: List<FriendEntry>, peaksById: Map<String, Peak>, limit: Int = 5): List<RecentConquest> =
    entries
        .filter { it.friendship.accepted && it.nick != null }
        .flatMap { entry ->
            entry.conquests.orEmpty().mapNotNull { c ->
                peaksById[c.peakId]?.let { RecentConquest(entry.friendship.friendUid, entry.nick!!, it, c.conqueredAt) }
            }
        }
        .sortedByDescending { it.date }
        .take(limit)

/** Ile dni kalendarzowych temu: 0 – dziś, 1 – wczoraj, 3 – trzy dni temu. */
fun daysAgo(date: Date, now: Date, zone: ZoneId = ZoneId.systemDefault()): Long =
    ChronoUnit.DAYS.between(date.toInstant().atZone(zone).toLocalDate(), now.toInstant().atZone(zone).toLocalDate())

/** Kim jest dla nas osoba z wyszukiwarki – od tego zależy przycisk przy jej nicku. */
enum class Relation { NONE, FRIEND, INVITED, INVITES_YOU }

fun relationTo(uid: String, friendships: List<Friendship>): Pair<Relation, Friendship?> {
    val friendship = friendships.find { it.friendUid == uid } ?: return Relation.NONE to null
    val relation = when {
        friendship.accepted -> Relation.FRIEND
        friendship.incoming -> Relation.INVITES_YOU
        else -> Relation.INVITED
    }
    return relation to friendship
}

/** Zdobycz znajomego na jego profilu, z oznaczeniem "Ty też", gdy też zdobyłeś ten szczyt. */
data class FriendConquestRow(val peak: Peak, val date: Date, val youToo: Boolean)

/** Profil znajomego (mockup 09) – jego statystyki w porównaniu z Twoimi. */
data class FriendComparison(
    val count: Int,
    val highest: Peak?,
    /** Szczyty zdobyte przez was oboje. */
    val common: Int,
    val friendKgp: SetProgress?,
    val myKgp: SetProgress?,
    /** Od najnowszej. */
    val conquests: List<FriendConquestRow>,
) {
    /** O ile szczytów Korony znajomy prowadzi (ujemne – prowadzisz Ty). */
    val kgpLead: Int get() = (friendKgp?.done ?: 0) - (myKgp?.done ?: 0)

    companion object {
        fun of(
            friendConquests: List<Conquest>,
            myPeakIds: Set<String>,
            peaksById: Map<String, Peak>,
            sets: List<PeakSet>,
        ): FriendComparison {
            val rows = friendConquests
                .mapNotNull { c -> peaksById[c.peakId]?.let { FriendConquestRow(it, c.conqueredAt, c.peakId in myPeakIds) } }
                .sortedByDescending { it.date }
            val friendIds = rows.mapTo(HashSet()) { it.peak.id }
            val kgp = sets.find { it.id == KGP_SET_ID }
            return FriendComparison(
                count = rows.size,
                highest = rows.maxByOrNull { it.peak.elevation }?.peak,
                common = rows.count { it.youToo },
                friendKgp = kgp?.let { SetProgress.of(it, peaksById, friendIds) },
                myKgp = kgp?.let { SetProgress.of(it, peaksById, myPeakIds) },
                conquests = rows,
            )
        }
    }
}
