package com.habit.viewmodel

import com.habit.data.ChoiceRepository
import java.time.Instant

class StreakCalculator(private val choiceRepo: ChoiceRepository) {

    suspend fun currentStreakStart(tallyId: String): Instant? {
        val mostRecent = choiceRepo.mostRecentChoice(tallyId)
            ?: return null
        if (!mostRecent.abstained) return null

        val lastYes = choiceRepo.mostRecentIndulgence(tallyId)
        val streakChoice = if (lastYes != null) {
            choiceRepo.firstAbstentionAfter(tallyId, lastYes.timestamp)
        } else {
            choiceRepo.firstAbstention(tallyId)
        }
        return streakChoice?.timestamp
    }

    suspend fun currentLapseStart(tallyId: String): Instant? {
        val mostRecent = choiceRepo.mostRecentChoice(tallyId)
            ?: return null
        if (mostRecent.abstained) return null

        val lastNo = choiceRepo.mostRecentAbstention(tallyId)
        val lapseChoice = if (lastNo != null) {
            choiceRepo.firstIndulgenceAfter(tallyId, lastNo.timestamp)
        } else {
            choiceRepo.firstIndulgence(tallyId)
        }
        return lapseChoice?.timestamp
    }
}
