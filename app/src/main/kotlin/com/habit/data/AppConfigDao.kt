package com.habit.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface AppConfigDao {
    @Query("SELECT * FROM app_config WHERE id = 1")
    suspend fun get(): AppConfigEntity?

    @Insert
    suspend fun insert(config: AppConfigEntity)
}
