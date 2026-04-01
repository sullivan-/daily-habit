package com.habit.viewmodel

import com.google.common.truth.Truth.assertThat
import com.habit.data.Activity
import com.habit.data.Habit
import com.habit.data.Priority
import com.habit.data.TargetMode
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Test

class ProgressStatusTest {

    private val monday = LocalDateTime.of(2026, 3, 30, 11, 0)

    private fun habit(
        timeOfDay: Int,
        priority: Priority = Priority.MEDIUM
    ) = Habit(
        id = "h$timeOfDay-$priority",
        name = "habit",
        timesOfDay = listOf(timeOfDay),
        sortOrder = 1,
        daysActive = DayOfWeek.entries.toSet(),
        dailyTarget = 1,
        dailyTargetMode = TargetMode.EXACTLY,
        timed = false,
        chimeIntervalSeconds = null,
        thresholdMinutes = null,
        thresholdType = null,
        priority = priority,
        dailyTexts = emptyMap()
    )

    private fun completed(habitId: String) = Activity(
        id = 0,
        habitId = habitId,
        attributedDate = LocalDate.of(2026, 3, 30),
        startTime = null,
        endTime = null,
        elapsedMs = 0,
        note = "",
        completedAt = Instant.now()
    )

    @Test
    fun `all done gives completedOverTotal = 1`() {
        val habits = listOf(habit(8), habit(10))
        val activities = habits.map { completed(it.id) }
        val ratios = progressRatios(habits, activities, monday)
        assertThat(ratios.completedOverTotal).isEqualTo(1f)
    }

    @Test
    fun `nothing done gives completedOverTotal = 0`() {
        val habits = listOf(habit(8), habit(10))
        val ratios = progressRatios(habits, emptyList(), monday)
        assertThat(ratios.completedOverTotal).isEqualTo(0f)
    }

    @Test
    fun `expectedOverTotal reflects time of day`() {
        // at 11:00, habits at 8 and 10 are due, habit at 14 is not
        val habits = listOf(habit(8), habit(10), habit(14))
        val ratios = progressRatios(habits, emptyList(), monday)
        // all same priority (MEDIUM=3), expected = 2*3=6, total = 3*3=9
        assertThat(ratios.expectedOverTotal).isWithin(0.01f).of(6f / 9f)
    }

    @Test
    fun `high priority weighs more in ratios`() {
        val high = habit(8, Priority.HIGH)
        val low = habit(8, Priority.LOW)

        // complete only the low one
        val ratios1 = progressRatios(
            listOf(high, low),
            listOf(completed(low.id)),
            monday
        )

        // complete only the high one
        val ratios2 = progressRatios(
            listOf(high, low),
            listOf(completed(high.id)),
            monday
        )

        assertThat(ratios2.completedOverTotal)
            .isGreaterThan(ratios1.completedOverTotal)
    }

    @Test
    fun `completedOverTotal never exceeds 1`() {
        val habits = listOf(habit(8))
        val activities = habits.map { completed(it.id) }
        val ratios = progressRatios(habits, activities, monday)
        assertThat(ratios.completedOverTotal).isAtMost(1f)
    }
}
