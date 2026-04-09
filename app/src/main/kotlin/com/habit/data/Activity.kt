package com.habit.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "activity",
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Track::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Milestone::class,
            parentColumns = ["id"],
            childColumns = ["milestoneId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("habitId"),
        Index("attributedDate"),
        Index("trackId"),
        Index("milestoneId")
    ]
)
data class Activity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: String,
    val attributedDate: LocalDate,
    val startTime: Instant?,
    val note: String,
    val completedAt: Instant?,
    val trackId: String? = null,
    val milestoneId: Long? = null
) {
    val elapsedMs: Long
        get() = when {
            startTime == null -> 0
            completedAt != null -> completedAt.toEpochMilli() - startTime.toEpochMilli()
            else -> System.currentTimeMillis() - startTime.toEpochMilli()
        }
}
