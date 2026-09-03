package com.habit.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface MilestoneDao {
    @Query("SELECT * FROM milestone WHERE trackId = :trackId ORDER BY sortOrder")
    suspend fun milestonesForTrack(trackId: String): List<Milestone>

    @Query(
        "SELECT * FROM milestone WHERE trackId = :trackId AND completed = 0 " +
        "ORDER BY sortOrder LIMIT 1"
    )
    suspend fun defaultMilestone(trackId: String): Milestone?

    @Query(
        "SELECT * FROM milestone WHERE trackId = :trackId AND completed = 0 " +
        "ORDER BY sortOrder"
    )
    suspend fun incompleteMilestones(trackId: String): List<Milestone>

    @Query("SELECT * FROM milestone WHERE id = :id")
    suspend fun getById(id: Long): Milestone?

    @Insert
    suspend fun insert(milestone: Milestone): Long

    @Update
    suspend fun update(milestone: Milestone)

    @Query("DELETE FROM milestone WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        "SELECT COUNT(*) FROM activity " +
        "WHERE milestoneId = :milestoneId " +
        "AND completedAt IS NOT NULL AND skipped = 0"
    )
    suspend fun activityCount(milestoneId: Long): Int

    @Query(
        "SELECT DISTINCT milestoneId FROM activity " +
        "WHERE milestoneId IN (:milestoneIds) " +
        "AND completedAt IS NOT NULL AND skipped = 0 " +
        "UNION SELECT id FROM milestone " +
        "WHERE id IN (:milestoneIds) AND activityId IS NOT NULL AND completed = 1"
    )
    suspend fun milestoneIdsWithHistory(milestoneIds: List<Long>): List<Long>

    @Query("SELECT * FROM milestone WHERE activityId = :activityId ORDER BY sortOrder")
    suspend fun claimedBy(activityId: Long): List<Milestone>

    @Query("SELECT * FROM milestone WHERE activityId IN (:activityIds) ORDER BY sortOrder")
    suspend fun claimedByAny(activityIds: List<Long>): List<Milestone>

    @Query(
        "UPDATE milestone SET activityId = :activityId, " +
        "completed = CASE WHEN :completed THEN 1 ELSE completed END WHERE id = :id"
    )
    suspend fun claim(id: Long, activityId: Long, completed: Boolean)

    @Query("UPDATE milestone SET activityId = NULL, completed = 0 WHERE id = :id")
    suspend fun release(id: Long)

    @Query("UPDATE milestone SET completed = 1 WHERE activityId = :activityId")
    suspend fun completeClaimedBy(activityId: Long)

    @Query("UPDATE milestone SET activityId = NULL WHERE activityId = :activityId")
    suspend fun releaseClaimedBy(activityId: Long)

    @Query(
        "UPDATE activity SET milestoneId = NULL " +
        "WHERE milestoneId = :milestoneId AND completedAt IS NULL"
    )
    suspend fun detachPendingActivities(milestoneId: Long)

    @Query("SELECT MAX(sortOrder) FROM milestone WHERE trackId = :trackId")
    suspend fun maxSortOrder(trackId: String): Int?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(milestones: List<Milestone>)
}
