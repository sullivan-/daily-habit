package com.habit.viewmodel

import com.habit.data.Activity
import com.habit.data.Habit
import java.time.DayOfWeek
import java.time.LocalDate

fun sortAgenda(
    habits: List<Habit>,
    activities: List<Activity>,
    today: LocalDate
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
            items.add(AgendaItem(
                habit = habit,
                activityNumber = done + 1,
                totalTarget = habit.dailyTarget
            ))
        }
    }

    return items.sortedWith(
        compareBy<AgendaItem> { it.activityNumber }
            .thenBy { if (it.activityNumber == 1) it.habit.timeOfDay else it.habit.priority.ordinal }
            .thenBy { it.habit.sortOrder }
    )
}
