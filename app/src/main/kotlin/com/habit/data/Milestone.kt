package com.habit.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "milestone",
    foreignKeys = [ForeignKey(
        entity = Track::class,
        parentColumns = ["id"],
        childColumns = ["trackId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("trackId")]
)
data class Milestone(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: String,
    val name: String,
    val sortOrder: Int,
    val completed: Boolean = false
)
