package com.habit.viewmodel

import com.habit.data.Activity
import com.habit.data.Habit

data class CompletedItem(
    val activity: Activity,
    val habit: Habit
)
