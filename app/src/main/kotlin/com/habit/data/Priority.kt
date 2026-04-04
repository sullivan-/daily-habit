package com.habit.data

enum class Priority { HIGH, MEDIUM_HIGH, MEDIUM, MEDIUM_LOW, LOW }

fun priorityToScore(priority: Priority): Float = when (priority) {
    Priority.LOW -> 0.2f
    Priority.MEDIUM_LOW -> 0.4f
    Priority.MEDIUM -> 0.6f
    Priority.MEDIUM_HIGH -> 0.8f
    Priority.HIGH -> 1.0f
}
