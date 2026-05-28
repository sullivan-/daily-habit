package com.habit.viewmodel

import com.habit.data.Activity
import com.habit.data.EasyDayLevel
import com.habit.data.Habit
import com.habit.data.Priority
import java.time.LocalDate
import java.time.LocalDateTime

fun graceMinutes(@Suppress("UNUSED_PARAMETER") priority: Priority): Int = 270

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

    // assign completions to past slots first (the ones already done),
    // preserving upcoming slots for the next activity
    val nonPast = expandedSlots
        .filter { !isSlotPastTime(it, habit.priority, now) }
        .toMutableList()
    val past = expandedSlots
        .filter { isSlotPastTime(it, habit.priority, now) }
        .toMutableList()

    var remaining = completedCount
    while (remaining > 0 && past.isNotEmpty()) {
        past.removeAt(0)
        remaining--
    }
    while (remaining > 0 && nonPast.isNotEmpty()) {
        nonPast.removeAt(0)
        remaining--
    }

    // pick the best unclaimed: earliest non-past-time, or earliest past
    return nonPast.minOrNull() ?: past.minOrNull()
}

fun sortAgenda(
    habits: List<Habit>,
    activities: List<Activity>,
    today: LocalDate,
    now: LocalDateTime = LocalDateTime.now(),
    easyDayLevel: EasyDayLevel = EasyDayLevel.OFF
): List<AgendaItem> {
    val dayOfWeek = today.dayOfWeek
    val activeHabits = habits
        .filter { dayOfWeek in it.daysActive }
        .filter { easyDayLevel.effectiveTarget(it.priority, it.dailyTarget) > 0 }

    val completedCounts = activities
        .filter { it.completedAt != null }
        .groupBy { it.habitId }
        .mapValues { it.value.size }

    // habits with in-progress activities today get on the agenda
    // even if not normally active today
    val inProgressHabitIds = activities
        .filter { it.completedAt == null }
        .map { it.habitId }
        .toSet()
    val habitsById = habits.associateBy { it.id }
    val extraHabits = inProgressHabitIds
        .filter { id -> activeHabits.none { it.id == id } }
        .mapNotNull { habitsById[it] }
        .filter { easyDayLevel.effectiveTarget(it.priority, it.dailyTarget) > 0 }

    val items = mutableListOf<AgendaItem>()
    for (habit in activeHabits) {
        val done = completedCounts[habit.id] ?: 0
        val target = easyDayLevel.effectiveTarget(habit.priority, habit.dailyTarget)
        if (done < target) {
            val slot = bestSlot(habit, done, now)
            if (slot != null) {
                items.add(AgendaItem(
                    habit = habit,
                    activityNumber = done + 1,
                    totalTarget = target,
                    assignedTimeOfDay = slot
                ))
            }
        }
    }
    for (habit in extraHabits) {
        val done = completedCounts[habit.id] ?: 0
        val target = easyDayLevel.effectiveTarget(habit.priority, habit.dailyTarget)
        items.add(AgendaItem(
            habit = habit,
            activityNumber = done + 1,
            totalTarget = maxOf(target, done + 1),
            assignedTimeOfDay = now.hour
        ))
    }

    return items.sortedWith(
        compareBy<AgendaItem> { it.isPastTime(now) }
            .thenBy { it.timeOfDay }
            .thenBy { it.habit.priority.ordinal }
            .thenByDescending { it.habit.tieBreaker }
    )
}
