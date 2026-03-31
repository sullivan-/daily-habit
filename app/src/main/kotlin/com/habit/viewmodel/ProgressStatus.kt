package com.habit.viewmodel

import com.habit.data.Habit
import java.time.LocalDateTime

fun progressStatus(
    completed: Int,
    habits: List<Habit>,
    now: LocalDateTime
): ProgressColor {
    val expectedByNow = habits.count { it.timeOfDay <= now.hour }
    return when {
        completed >= expectedByNow + 1 -> ProgressColor.GREEN
        completed >= expectedByNow - 1 -> ProgressColor.BLUE
        else -> ProgressColor.RED
    }
}
