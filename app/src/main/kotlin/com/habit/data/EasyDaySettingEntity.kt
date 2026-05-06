package com.habit.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "easy_day_setting")
data class EasyDaySettingEntity(
    @PrimaryKey val date: LocalDate,
    val level: EasyDayLevel
)
