package com.habit.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activity WHERE attributedDate = :date")
    fun activitiesForDate(date: LocalDate): Flow<List<Activity>>

    @Query(
        "SELECT * FROM activity WHERE habitId = :habitId AND attributedDate = :date"
    )
    fun activitiesForHabitOnDate(
        habitId: String,
        date: LocalDate
    ): Flow<List<Activity>>

    @Query(
        "SELECT * FROM activity " +
        "WHERE habitId = :habitId AND attributedDate = :date AND completedAt IS NULL " +
        "LIMIT 1"
    )
    suspend fun inProgressActivity(habitId: String, date: LocalDate): Activity?

    @Query(
        "SELECT * FROM activity " +
        "WHERE habitId = :habitId AND completedAt IS NOT NULL " +
        "ORDER BY completedAt ASC"
    )
    suspend fun completedHistoryForHabit(habitId: String): List<Activity>

    @Insert
    suspend fun insert(activity: Activity): Long

    @Update
    suspend fun update(activity: Activity)

    @Query("DELETE FROM activity WHERE id = :id")
    suspend fun deleteById(id: Long)
}
