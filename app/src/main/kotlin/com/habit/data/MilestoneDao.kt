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

    @Query("SELECT COUNT(*) FROM activity WHERE milestoneId = :milestoneId")
    suspend fun activityCount(milestoneId: Long): Int

    @Query("SELECT MAX(sortOrder) FROM milestone WHERE trackId = :trackId")
    suspend fun maxSortOrder(trackId: String): Int?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(milestones: List<Milestone>)
}
