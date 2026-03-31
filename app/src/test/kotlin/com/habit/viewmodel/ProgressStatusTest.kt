package com.habit.viewmodel

import com.google.common.truth.Truth.assertThat
import com.habit.data.Habit
import com.habit.data.Priority
import com.habit.data.TargetMode
import java.time.DayOfWeek
import java.time.LocalDateTime
import org.junit.Test

class ProgressStatusTest {

    private fun habit(timeOfDay: Int) = Habit(
        id = "h$timeOfDay",
        name = "habit at $timeOfDay",
        timeOfDay = timeOfDay,
        sortOrder = 1,
        daysActive = DayOfWeek.entries.toSet(),
        dailyTarget = 1,
        dailyTargetMode = TargetMode.EXACTLY,
        timed = false,
        chimeIntervalSeconds = null,
        thresholdMinutes = null,
        thresholdType = null,
        priority = Priority.MEDIUM,
        dailyTexts = emptyMap()
    )

    private val habits = listOf(habit(7), habit(8), habit(10), habit(14))

    @Test
    fun `on track returns blue`() {
        // at 11:00, 3 habits due (7, 8, 10), 2 completed — within 1
        val result = progressStatus(
            completed = 2,
            habits = habits,
            now = LocalDateTime.of(2026, 3, 30, 11, 0)
        )
        assertThat(result).isEqualTo(ProgressColor.BLUE)
    }

    @Test
    fun `ahead returns green`() {
        // at 9:00, 2 habits due (7, 8), 4 completed — ahead
        val result = progressStatus(
            completed = 4,
            habits = habits,
            now = LocalDateTime.of(2026, 3, 30, 9, 0)
        )
        assertThat(result).isEqualTo(ProgressColor.GREEN)
    }

    @Test
    fun `behind returns red`() {
        // at 15:00, 4 habits due, 0 completed — behind
        val result = progressStatus(
            completed = 0,
            habits = habits,
            now = LocalDateTime.of(2026, 3, 30, 15, 0)
        )
        assertThat(result).isEqualTo(ProgressColor.RED)
    }
}
