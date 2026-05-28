package com.habit.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "easy_day_carry_over")
data class EasyDayCarryOverEntity(
    @PrimaryKey val id: Int = 1,
    val enabled: Boolean,
    val level: EasyDayLevel
)
