package com.habit.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM track WHERE habitId = :habitId ORDER BY archived, name")
    fun tracksForHabit(habitId: String): Flow<List<Track>>

    @Query(
        "SELECT * FROM track WHERE habitId = :habitId AND archived = 0 " +
        "ORDER BY name"
    )
    suspend fun activeTracksForHabit(habitId: String): List<Track>

    @Query("SELECT * FROM track WHERE id = :id")
    suspend fun getById(id: String): Track?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(tracks: List<Track>)

    @Insert
    suspend fun insert(track: Track)

    @Update
    suspend fun update(track: Track)

    @Query("DELETE FROM track WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM activity WHERE trackId = :trackId")
    suspend fun activityCount(trackId: String): Int
}
