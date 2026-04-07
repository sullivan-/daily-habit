package com.habit.data

import java.time.Instant

class ChoiceRepository(private val choiceDao: ChoiceDao) {
    suspend fun record(choice: Choice): Long = choiceDao.insert(choice)

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
}
