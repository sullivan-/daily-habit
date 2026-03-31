package com.habit.viewmodel

import com.habit.data.Habit

data class AgendaItem(
    val habit: Habit,
    val activityNumber: Int,
    val totalTarget: Int
)
