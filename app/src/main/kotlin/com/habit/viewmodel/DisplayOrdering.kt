package com.habit.viewmodel

import com.habit.data.Activity
import com.habit.data.Habit
import com.habit.data.Priority
import java.time.LocalDate
import java.time.LocalDateTime

fun graceMinutes(priority: Priority): Int = when (priority) {
    Priority.HIGH -> 270
    Priority.MEDIUM_HIGH -> 210
    Priority.MEDIUM -> 150
    Priority.MEDIUM_LOW -> 90
    Priority.LOW -> 30
}

fun isSlotPastTime(hour: Int, priority: Priority, now: LocalDateTime): Boolean {
    val deadlineMinutes = hour * 60 + graceMinutes(priority)
    val nowMinutes = now.hour * 60 + now.minute
    return nowMinutes > deadlineMinutes
}

fun AgendaItem.isPastTime(now: LocalDateTime): Boolean =
    isSlotPastTime(timeOfDay, habit.priority, now)

fun bestSlot(
    habit: Habit,
    completedCount: Int,
    now: LocalDateTime
): Int? {
    val slots = habit.timesOfDay.sorted()

    // if more targets than slots, expand by repeating the last slot
    val expandedSlots = if (habit.dailyTarget > slots.size) {
        slots + List(habit.dailyTarget - slots.size) { slots.last() }
    } else {
        slots
    }

    // assign completions to the best available slots:
    // prefer non-past-time slots first, then past-time slots
    val nonPast = expandedSlots
        .filter { !isSlotPastTime(it, habit.priority, now) }
        .toMutableList()
    val past = expandedSlots
        .filter { isSlotPastTime(it, habit.priority, now) }
        .toMutableList()

    var remaining = completedCount
    while (remaining > 0 && nonPast.isNotEmpty()) {
        nonPast.removeAt(0)
        remaining--
    }
    while (remaining > 0 && past.isNotEmpty()) {
        past.removeAt(0)
        remaining--
    }

    // pick the best unclaimed: earliest non-past-time, or earliest past
    return nonPast.minOrNull() ?: past.minOrNull()
}

fun sortAgenda(
    habits: List<Habit>,
    activities: List<Activity>,
    today: LocalDate,
    now: LocalDateTime = LocalDateTime.now()
): List<AgendaItem> {
    val dayOfWeek = today.dayOfWeek
    val activeHabits = habits.filter { dayOfWeek in it.daysActive }

    val completedCounts = activities
        .filter { it.completedAt != null }
        .groupBy { it.habitId }
        .mapValues { it.value.size }

    val items = mutableListOf<AgendaItem>()
    for (habit in activeHabits) {
        val done = completedCounts[habit.id] ?: 0
        if (done < habit.dailyTarget) {
            val slot = bestSlot(habit, done, now)
            if (slot != null) {
                items.add(AgendaItem(
                    habit = habit,
                    activityNumber = done + 1,
                    totalTarget = habit.dailyTarget,
                    assignedTimeOfDay = slot
                ))
            }
        }
    }

    return items.sortedWith(
        compareBy<AgendaItem> { it.isPastTime(now) }
            .thenBy { it.timeOfDay }
            .thenBy { it.habit.priority.ordinal }
            .thenBy { it.habit.sortOrder }
    )
}
