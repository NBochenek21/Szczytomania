package pl.szczytomania.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import pl.szczytomania.data.firestore.Challenge
import pl.szczytomania.data.firestore.ChallengeRepository
import pl.szczytomania.ui.screens.challenges.scoreFor

/** Wyzwania zalogowanego użytkownika – wspólne dla listy, szczegółów i zapisu wyników. */
object Challenges {
    val repository = ChallengeRepository()
    val all: Flow<List<Challenge>> = repository.challenges

    /**
     * Na bieżąco zapisuje wynik użytkownika w jego wyzwaniach: po każdej nowej zdobyczy (także tej
     * zapisanej bez zasięgu) i po dołączeniu do nowego wyzwania. Zapisuje tylko, gdy wynik się zmienił.
     */
    suspend fun keepMyScoresUpToDate(myUid: String) {
        combine(all, ConqueredPeaks.all) { challenges, conquests -> challenges to conquests }.collect { (challenges, conquests) ->
            challenges.forEach { challenge ->
                val score = scoreFor(challenge, conquests)
                if (challenge.scores[myUid] != score) repository.saveMyScore(challenge.id, score)
            }
        }
    }
}
