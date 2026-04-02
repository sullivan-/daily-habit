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
    fun `all due items completed gives completedOverExpected = 1`() {
        val habits = listOf(habit(8), habit(10))
        val activities = habits.map { completed(it.id) }
        val ratios = progressRatios(habits, activities, monday)
        assertThat(ratios.completedOverExpected).isEqualTo(1f)
    }

    @Test
    fun `all items completed gives completedOverTotal = 1`() {
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
    fun `nothing due yet gives completedOverExpected = 1`() {
        val early = LocalDateTime.of(2026, 3, 30, 6, 0)
        val habits = listOf(habit(8))
        val ratios = progressRatios(habits, emptyList(), early)
        assertThat(ratios.completedOverExpected).isEqualTo(1f)
    }

    @Test
    fun `completedOverExpected always gte completedOverTotal`() {
        val habits = listOf(habit(8), habit(10), habit(14))
        val activities = listOf(completed(habits[0].id))
        val ratios = progressRatios(habits, activities, monday)
        assertThat(ratios.completedOverExpected)
            .isAtLeast(ratios.completedOverTotal)
    }

    @Test
    fun `high priority missing hurts completedOverExpected more`() {
        val highHabit = habit(8, Priority.HIGH)
        val lowHabit = habit(8, Priority.LOW)

        val ratios1 = progressRatios(
            listOf(highHabit, lowHabit),
            listOf(completed(lowHabit.id)),
            monday
        )

        val ratios2 = progressRatios(
            listOf(highHabit, lowHabit),
            listOf(completed(highHabit.id)),
            monday
        )

        assertThat(ratios2.completedOverExpected)
            .isGreaterThan(ratios1.completedOverExpected)
    }

    @Test
    fun `caught up early morning is all blue no red`() {
        // at 8:30, only kegel (8am, LOW) is due, and it's done
        val kegel = habit(8, Priority.LOW)
        val future = habit(14, Priority.HIGH)
        val at830 = LocalDateTime.of(2026, 3, 30, 8, 30)
        val ratios = progressRatios(
            listOf(kegel, future),
            listOf(completed(kegel.id)),
            at830
        )
        assertThat(ratios.completedOverExpected).isEqualTo(1f)
        assertThat(ratios.completedOverTotal).isLessThan(1f)
    }
}
