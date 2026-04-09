package com.habit.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek

@Entity(tableName = "habit")
data class Habit(
    @PrimaryKey val id: String,
    val name: String,
    val timesOfDay: List<Int>,
    val sortOrder: Int,
    val daysActive: Set<DayOfWeek>,
    val dailyTarget: Int,
    val dailyTargetMode: TargetMode,
    val timed: Boolean,
    val goalMinutes: Int?,
    val stopMinutes: Int?,
    val priority: Priority
)
