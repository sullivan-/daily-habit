package com.habit.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TallyDao {
    @Query("SELECT * FROM tally")
    fun allTallies(): Flow<List<Tally>>

    @Query("SELECT * FROM tally WHERE id = :id")
    suspend fun getById(id: Long): Tally?

    @Insert
    suspend fun insert(tally: Tally): Long

    @Update
    suspend fun update(tally: Tally)

    @Query("DELETE FROM tally WHERE id = :id")
    suspend fun deleteById(id: Long)
}
