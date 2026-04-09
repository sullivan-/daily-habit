package com.habit.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.DayOfWeek

@Entity(
    tableName = "track",
    foreignKeys = [ForeignKey(
        entity = Habit::class,
        parentColumns = ["id"],
        childColumns = ["habitId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("habitId")]
)
data class Track(
    @PrimaryKey val id: String,
    val habitId: String,
    val name: String,
    val priority: Priority,
    val dayOfWeek: DayOfWeek? = null,
    val archived: Boolean = false
)
