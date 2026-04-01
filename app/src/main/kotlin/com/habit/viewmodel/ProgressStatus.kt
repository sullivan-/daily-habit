package com.habit.viewmodel

import com.habit.data.Activity
import com.habit.data.Habit
import com.habit.data.Priority
import java.time.LocalDateTime

fun priorityWeight(priority: Priority): Int = when (priority) {
    Priority.HIGH -> 5
    Priority.MEDIUM_HIGH -> 4
    Priority.MEDIUM -> 3
    Priority.MEDIUM_LOW -> 2
    Priority.LOW -> 1
}

data class ProgressRatios(
    val completedOverTotal: Float,
    val expectedOverTotal: Float
)

fun progressRatios(
    habits: List<Habit>,
    activities: List<Activity>,
    now: LocalDateTime
): ProgressRatios {
    val dayOfWeek = now.dayOfWeek
    val activeHabits = habits.filter { dayOfWeek in it.daysActive }

    var totalWeight = 0
    var expectedWeight = 0
    for (habit in activeHabits) {
        val w = priorityWeight(habit.priority)
        totalWeight += habit.dailyTarget * w
        expectedWeight += habit.timesOfDay.count { it <= now.hour } * w
    }

    if (totalWeight == 0) return ProgressRatios(0f, 0f)

    val completedByHabit = activities
        .filter { it.completedAt != null }
        .groupBy { it.habitId }
        .mapValues { it.value.size }

    var completedWeight = 0
    for (habit in activeHabits) {
        val w = priorityWeight(habit.priority)
        val done = completedByHabit[habit.id] ?: 0
        completedWeight += minOf(done, habit.dailyTarget) * w
    }

    return ProgressRatios(
        completedOverTotal = completedWeight.toFloat() / totalWeight,
        expectedOverTotal = minOf(expectedWeight.toFloat() / totalWeight, 1f)
    )
}
