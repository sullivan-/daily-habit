package com.habit.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "choice",
    foreignKeys = [ForeignKey(
        entity = Tally::class,
        parentColumns = ["id"],
        childColumns = ["tallyId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("tallyId"), Index("timestamp")]
)
data class Choice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tallyId: Long,
    val timestamp: Instant,
    val abstained: Boolean
)
