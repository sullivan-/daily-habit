package com.habit.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tally")
data class Tally(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val priority: Priority
)
