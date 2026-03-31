package com.habit.viewmodel

import com.google.common.truth.Truth.assertThat
import com.habit.data.Activity
import com.habit.data.Habit
import com.habit.data.Priority
import com.habit.data.TargetMode
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import org.junit.Test

class DisplayOrderingTest {

    private val monday = LocalDate.of(2026, 3, 30) // a Monday

    private fun habit(
        id: String,
        timeOfDay: Int = 8,
        sortOrder: Int = 1,
        dailyTarget: Int = 1,
        priority: Priority = Priority.MEDIUM,
        daysActive: Set<DayOfWeek> = DayOfWeek.entries.toSet()
    ) = Habit(
        id = id,
        name = id,
        timeOfDay = timeOfDay,
        sortOrder = sortOrder,
        daysActive = daysActive,
        dailyTarget = dailyTarget,
        dailyTargetMode = TargetMode.AT_LEAST,
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
        attributedDate = monday,
        startTime = null,
        endTime = null,
        elapsedMs = 0,
        note = "",
        completedAt = Instant.now()
    )

    @Test
    fun `first activities sort by time of day then sort order`() {
        val habits = listOf(
            habit("afternoon", timeOfDay = 14, sortOrder = 1),
            habit("morning-b", timeOfDay = 7, sortOrder = 2),
            habit("morning-a", timeOfDay = 7, sortOrder = 1)
        )
        val result = sortAgenda(habits, emptyList(), monday)
        assertThat(result.map { it.habit.id })
            .containsExactly("morning-a", "morning-b", "afternoon")
            .inOrder()
    }

    @Test
    fun `first activities appear before second activities`() {
        val habits = listOf(
            habit("a", timeOfDay = 7, dailyTarget = 2, priority = Priority.HIGH),
            habit("b", timeOfDay = 14)
        )
        val activities = listOf(completed("a"))
        val result = sortAgenda(habits, activities, monday)
        assertThat(result.map { it.habit.id })
            .containsExactly("b", "a")
            .inOrder()
    }

    @Test
    fun `subsequent activities sort by priority then sort order`() {
        val habits = listOf(
            habit("low", dailyTarget = 2, priority = Priority.LOW, sortOrder = 1),
            habit("high", dailyTarget = 2, priority = Priority.HIGH, sortOrder = 1)
        )
        val activities = listOf(completed("low"), completed("high"))
        val result = sortAgenda(habits, activities, monday)
        assertThat(result.map { it.habit.id })
            .containsExactly("high", "low")
            .inOrder()
    }

    @Test
    fun `habits not active today are excluded`() {
        val habits = listOf(
            habit("weekday", daysActive = setOf(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
            )),
            habit("weekend", daysActive = setOf(
                DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
            ))
        )
        val result = sortAgenda(habits, emptyList(), monday)
        assertThat(result.map { it.habit.id })
            .containsExactly("weekday")
    }

    @Test
    fun `habits with all target activities complete are excluded`() {
        val habits = listOf(
            habit("done", dailyTarget = 1),
            habit("remaining", dailyTarget = 1)
        )
        val activities = listOf(completed("done"))
        val result = sortAgenda(habits, activities, monday)
        assertThat(result.map { it.habit.id })
            .containsExactly("remaining")
    }
}
