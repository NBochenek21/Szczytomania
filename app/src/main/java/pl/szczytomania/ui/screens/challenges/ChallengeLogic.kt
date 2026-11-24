package pl.szczytomania.ui.screens.challenges

import pl.szczytomania.data.firestore.Challenge
import pl.szczytomania.data.firestore.ChallengeMode
import pl.szczytomania.data.firestore.ChallengeScore
import pl.szczytomania.data.firestore.Conquest
import java.util.Date

/**
 * Wynik użytkownika w wyzwaniu z jego zdobyczy: liczą się szczyty zestawu zdobyte od startu wyzwania
 * (a w "kto więcej do daty" – najpóźniej w dniu końca). Cały zestaw = czas ostatniego brakującego szczytu.
 */
fun scoreFor(challenge: Challenge, conquests: List<Conquest>): ChallengeScore {
    val inSet = challenge.peakIds.toSet()
    val counted = conquests.filter { c ->
        c.peakId in inSet && !c.conqueredAt.before(challenge.createdAt) && (challenge.endsAt == null || !c.conqueredAt.after(challenge.endsAt))
    }
    val done = counted.map { it.peakId }.distinct().size
    val completedAt = if (done == inSet.size) counted.maxOf { it.conqueredAt } else null
    return ChallengeScore(done, completedAt)
}

/** Wiersz rankingu. Osoby z takim samym wynikiem mają to samo miejsce. */
data class RankRow(val uid: String, val nick: String?, val done: Int, val completedAt: Date?, val me: Boolean, val place: Int)

/**
 * Ranking: w "kto pierwszy" najpierw ci, którzy skończyli zestaw (kto wcześniej, ten wyżej), potem reszta
 * po liczbie szczytów; w "kto więcej do daty" – po liczbie szczytów.
 */
fun ranking(challenge: Challenge, myUid: String?, nicks: Map<String, String>): List<RankRow> {
    val rows = challenge.participants.map { uid ->
        val score = challenge.scores[uid] ?: ChallengeScore(0, null)
        RankRow(uid, nicks[uid], score.done, score.completedAt, uid == myUid, place = 0)
    }
    val order = when (challenge.mode) {
        ChallengeMode.FIRST -> compareBy<RankRow>({ it.completedAt == null }, { it.completedAt }, { -it.done })
        ChallengeMode.MOST -> compareBy { -it.done }
    }
    val sorted = rows.sortedWith(order.then(compareBy({ it.nick == null }, { it.nick?.lowercase() })))
    var place = 0
    return sorted.mapIndexed { index, row ->
        if (index == 0 || order.compare(sorted[index - 1], row) != 0) place = index + 1
        row.copy(place = place)
    }
}

/** Wyścig się skończył: ktoś zdobył cały zestaw ("kto pierwszy") albo minęła data końca ("kto więcej"). */
fun isFinished(challenge: Challenge, now: Date = Date()): Boolean = when (challenge.mode) {
    ChallengeMode.FIRST -> challenge.scores.values.any { it.completedAt != null }
    ChallengeMode.MOST -> challenge.endsAt?.let { !now.before(it) } ?: false
}
