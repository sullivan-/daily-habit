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

    fun effectiveTarget(priority: Priority, dailyTarget: Int): Int {
        val threshold = skipsAtOrBelow ?: return dailyTarget
        if (includes(priority)) return dailyTarget
        val stepsPastThreshold = priority.ordinal - threshold.ordinal + 1
        return maxOf(0, dailyTarget - stepsPastThreshold)
    }
}
