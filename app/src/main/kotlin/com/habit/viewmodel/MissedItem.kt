package com.habit.viewmodel

import com.habit.data.Habit

data class MissedItem(
    val habit: Habit,
    val count: Int,
    val target: Int
)
