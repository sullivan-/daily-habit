package com.habit.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tally")
data class Tally(
    @PrimaryKey val id: String,
    val name: String,
    val priority: Priority
)
