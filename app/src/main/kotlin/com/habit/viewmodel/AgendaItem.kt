package com.habit.viewmodel

import com.habit.data.Habit

data class AgendaItem(
    val habit: Habit,
    val activityNumber: Int,
    val totalTarget: Int,
    val assignedTimeOfDay: Int? = null
) {
    val timeOfDay: Int
        get() = assignedTimeOfDay
            ?: habit.timesOfDay.getOrElse(activityNumber - 1) {
                habit.timesOfDay.lastOrNull() ?: 0
            }
}
