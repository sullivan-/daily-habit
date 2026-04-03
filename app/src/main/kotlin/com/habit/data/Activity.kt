package com.habit.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "activity",
    foreignKeys = [ForeignKey(
        entity = Habit::class,
        parentColumns = ["id"],
        childColumns = ["habitId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("habitId"), Index("attributedDate")]
)
data class Activity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: String,
    val attributedDate: LocalDate,
    val startTime: Instant?,
    val elapsedMs: Long,
    val note: String,
    val completedAt: Instant?
)
