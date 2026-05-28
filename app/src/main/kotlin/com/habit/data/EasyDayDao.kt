package com.habit.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface EasyDayDao {
    @Query("SELECT * FROM easy_day_setting WHERE date = :date")
    fun flowForDate(date: LocalDate): Flow<EasyDaySettingEntity?>

    @Query("SELECT * FROM easy_day_setting WHERE date = :date")
    suspend fun getForDate(date: LocalDate): EasyDaySettingEntity?

    @Query("SELECT * FROM easy_day_setting ORDER BY date DESC")
    suspend fun getAll(): List<EasyDaySettingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(setting: EasyDaySettingEntity)

    @Query("DELETE FROM easy_day_setting WHERE date = :date")
    suspend fun deleteForDate(date: LocalDate)

    @Query("SELECT * FROM easy_day_carry_over WHERE id = 1")
    fun flowCarryOver(): Flow<EasyDayCarryOverEntity?>

    @Query("SELECT * FROM easy_day_carry_over WHERE id = 1")
    suspend fun getCarryOver(): EasyDayCarryOverEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCarryOver(setting: EasyDayCarryOverEntity)
}
