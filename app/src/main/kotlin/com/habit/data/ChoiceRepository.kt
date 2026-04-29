package com.habit.data

import java.time.Instant
import kotlinx.coroutines.flow.Flow

class ChoiceRepository(private val choiceDao: ChoiceDao) {
    suspend fun record(choice: Choice): Long = choiceDao.insert(choice)

    fun choiceChanges(): Flow<Int> = choiceDao.choiceCountFlow()

    suspend fun recentChoices(tallyId: String, limit: Int = 10): List<Choice> =
        choiceDao.recentChoices(tallyId, limit)

    suspend fun choiceCountsSince(since: Instant): List<TallyChoiceCount> =
        choiceDao.choiceCountsSince(since.toEpochMilli())

    suspend fun totalCountSince(since: Instant): Int =
        choiceDao.totalCountSince(since.toEpochMilli())

    suspend fun abstainCountSince(since: Instant): Int =
        choiceDao.abstainCountSince(since.toEpochMilli())

    suspend fun choicesToday(
        tallyId: String,
        dayStart: Instant,
        dayEnd: Instant
    ): List<Choice> =
        choiceDao.choicesInRange(
            tallyId,
            dayStart.toEpochMilli(),
            dayEnd.toEpochMilli()
        )

    suspend fun mostRecentChoice(tallyId: String): Choice? =
        choiceDao.mostRecentChoice(tallyId)

    suspend fun mostRecentIndulgence(tallyId: String): Choice? =
        choiceDao.mostRecentIndulgence(tallyId)

    suspend fun firstAbstentionAfter(tallyId: String, after: Instant): Choice? =
        choiceDao.firstAbstentionAfter(tallyId, after.toEpochMilli())

    suspend fun firstAbstention(tallyId: String): Choice? =
        choiceDao.firstAbstention(tallyId)

    suspend fun earliestChoice(tallyId: String): Choice? =
        choiceDao.earliestChoice(tallyId)
}
