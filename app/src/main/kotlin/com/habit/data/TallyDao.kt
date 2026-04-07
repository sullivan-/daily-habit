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
    suspend fun getById(id: String): Tally?

    @Insert(onConflict = androidx.room.OnConflictStrategy.IGNORE)
    suspend fun insertAll(tallies: List<Tally>)

    @Insert
    suspend fun insert(tally: Tally)

    @Update
    suspend fun update(tally: Tally)

    @Query("DELETE FROM tally WHERE id = :id")
    suspend fun deleteById(id: String)
}
