package com.habit.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habit")
    fun allHabits(): Flow<List<Habit>>

    @Query("SELECT COUNT(*) FROM habit")
    suspend fun count(): Int

    @Query("SELECT * FROM habit WHERE id = :id")
    suspend fun getById(id: String): Habit?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(habits: List<Habit>)

    @Query("SELECT id FROM habit")
    suspend fun allIds(): List<String>

    @Insert
    suspend fun insert(habit: Habit)

    @Update
    suspend fun update(habit: Habit)

    @Query("DELETE FROM habit WHERE id = :id")
    suspend fun deleteById(id: String)
}
