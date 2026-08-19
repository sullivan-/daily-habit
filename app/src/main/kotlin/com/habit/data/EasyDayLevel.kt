package com.habit.data

enum class EasyDayLevel(val skipsAtOrBelow: Priority?) {
    OFF(null),
    LOW(Priority.LOW),
    MEDIUM_LOW(Priority.MEDIUM_LOW),
    MEDIUM(Priority.MEDIUM),
    MEDIUM_HIGH(Priority.MEDIUM_HIGH),
    HIGH(Priority.HIGH);

    fun includes(priority: Priority): Boolean {
        val threshold = skipsAtOrBelow ?: return true
        return priorityToScore(priority) > priorityToScore(threshold)
    }
}
