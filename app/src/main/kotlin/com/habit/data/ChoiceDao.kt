package com.habit.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ChoiceDao {
    @Insert
    suspend fun insert(choice: Choice): Long

    @Query(
        "SELECT * FROM choice WHERE tallyId = :tallyId " +
        "ORDER BY timestamp DESC LIMIT :limit"
    )
    suspend fun recentChoices(tallyId: String, limit: Int): List<Choice>

    @Query(
        "SELECT * FROM choice WHERE tallyId = :tallyId " +
        "AND timestamp >= :since ORDER BY timestamp DESC"
    )
    suspend fun choicesSince(tallyId: String, since: Long): List<Choice>

    @Query(
        "SELECT tallyId, COUNT(*) as count FROM choice " +
        "WHERE timestamp >= :since GROUP BY tallyId"
    )
    suspend fun choiceCountsSince(since: Long): List<TallyChoiceCount>

    @Query(
        "SELECT COUNT(*) FROM choice WHERE timestamp >= :since"
    )
    suspend fun totalCountSince(since: Long): Int

    @Query(
        "SELECT COUNT(*) FROM choice WHERE timestamp >= :since AND abstained = 1"
    )
    suspend fun abstainCountSince(since: Long): Int

    @Query(
        "SELECT * FROM choice WHERE tallyId = :tallyId " +
        "AND timestamp >= :since AND timestamp < :until " +
        "ORDER BY timestamp DESC"
    )
    suspend fun choicesInRange(tallyId: String, since: Long, until: Long): List<Choice>
}
