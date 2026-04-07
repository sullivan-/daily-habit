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

class DisplayOrderingTest {

    private val monday = LocalDate.of(2026, 3, 30)
    // early morning: nothing is past time
    private val earlyMorning = LocalDateTime.of(2026, 3, 30, 6, 0)

    private fun habit(
        id: String,
        timesOfDay: List<Int> = listOf(8),
        sortOrder: Int = 1,
        dailyTarget: Int = 1,
        priority: Priority = Priority.MEDIUM,
        daysActive: Set<DayOfWeek> = DayOfWeek.entries.toSet()
    ) = Habit(
        id = id,
        name = id,
        timesOfDay = timesOfDay,
        sortOrder = sortOrder,
        daysActive = daysActive,
        dailyTarget = dailyTarget,
        dailyTargetMode = TargetMode.AT_LEAST,
        timed = false,


        goalMinutes = null,
        stopMinutes = null,
        priority = priority,
        dailyTexts = emptyMap()
    )

    private fun completed(habitId: String) = Activity(
        id = 0,
        habitId = habitId,
        attributedDate = monday,
        startTime = null,




        note = "",
        completedAt = Instant.now()
    )

    @Test
    fun `first activities sort by time of day then sort order`() {
        val habits = listOf(
            habit("afternoon", timesOfDay = listOf(14), sortOrder = 1),
            habit("morning-b", timesOfDay = listOf(7), sortOrder = 2),
            habit("morning-a", timesOfDay = listOf(7), sortOrder = 1)
        )
        val result = sortAgenda(habits, emptyList(), monday, earlyMorning)
        assertThat(result.map { it.habit.id })
            .containsExactly("morning-a", "morning-b", "afternoon")
            .inOrder()
    }

    @Test
    fun `activities sort by their specific time slot`() {
        val habits = listOf(
            habit("a", timesOfDay = listOf(7, 15), dailyTarget = 2, priority = Priority.HIGH),
            habit("b", timesOfDay = listOf(14))
        )
        val activities = listOf(completed("a"))
        val result = sortAgenda(habits, activities, monday, earlyMorning)
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
        val result = sortAgenda(habits, activities, monday, earlyMorning)
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
        val result = sortAgenda(habits, emptyList(), monday, earlyMorning)
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
        val result = sortAgenda(habits, activities, monday, earlyMorning)
        assertThat(result.map { it.habit.id })
            .containsExactly("remaining")
    }

    @Test
    fun `past time items sort after non-past-time items`() {
        // low prio at 8 => past time at 8:31
        // high prio at 8 => past time at 12:31
        val habits = listOf(
            habit("low-8", timesOfDay = listOf(8), priority = Priority.LOW),
            habit("high-8", timesOfDay = listOf(8), priority = Priority.HIGH),
            habit("afternoon", timesOfDay = listOf(14), priority = Priority.MEDIUM)
        )
        // at 9:00, low-8 is past time (8:00 + 30min = 8:30), high-8 is not
        val at9am = LocalDateTime.of(2026, 3, 30, 9, 0)
        val result = sortAgenda(habits, emptyList(), monday, at9am)
        assertThat(result.map { it.habit.id })
            .containsExactly("high-8", "afternoon", "low-8")
            .inOrder()
    }

    @Test
    fun `grace period scales with priority`() {
        val habits = listOf(
            habit("low", timesOfDay = listOf(8), priority = Priority.LOW),
            habit("med", timesOfDay = listOf(8), priority = Priority.MEDIUM),
            habit("high", timesOfDay = listOf(8), priority = Priority.HIGH)
        )
        // at 11:00: low past (8:30), med not yet (10:30), high not yet (12:30)
        // high and med both not past time, high sorts first by priority
        val at11am = LocalDateTime.of(2026, 3, 30, 11, 0)
        val result = sortAgenda(habits, emptyList(), monday, at11am)
        assertThat(result.map { it.habit.id })
            .containsExactly("high", "med", "low")
            .inOrder()
    }

    @Test
    fun `multi-time habit uses best unclaimed slot`() {
        // kegel at 8, 12, 16 — low priority (30min grace)
        val kegel = habit("kegel", timesOfDay = listOf(8, 12, 16),
            dailyTarget = 3, priority = Priority.LOW)
        val other = habit("other", timesOfDay = listOf(13), priority = Priority.MEDIUM)

        // at 12:15, 0 completions: 8 is past time (8:30), 12 is not (12:30)
        // kegel should get the 12:00 slot
        val at1215 = LocalDateTime.of(2026, 3, 30, 12, 15)
        val result = sortAgenda(listOf(kegel, other), emptyList(), monday, at1215)
        assertThat(result.first().habit.id).isEqualTo("kegel")
        assertThat(result.first().timeOfDay).isEqualTo(12)
    }

    @Test
    fun `multi-time habit after completion picks best remaining slot`() {
        val kegel = habit("kegel", timesOfDay = listOf(8, 12, 16),
            dailyTarget = 3, priority = Priority.LOW)
        val other = habit("other", timesOfDay = listOf(13), priority = Priority.MEDIUM)

        // at 12:15, 1 completion: claims the 12:00 slot
        // remaining: 8 (past time) and 16 (not past time)
        // kegel should get the 16:00 slot
        val at1215 = LocalDateTime.of(2026, 3, 30, 12, 15)
        val activities = listOf(completed("kegel"))
        val result = sortAgenda(listOf(kegel, other), activities, monday, at1215)
        val kegelItem = result.find { it.habit.id == "kegel" }!!
        assertThat(kegelItem.timeOfDay).isEqualTo(16)
    }

    @Test
    fun `in-progress activity on non-active day includes habit on agenda`() {
        val weekdayOnly = habit(
            "weekday",
            daysActive = setOf(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
            )
        )
        val saturday = LocalDate.of(2026, 4, 4)
        val satMorning = LocalDateTime.of(2026, 4, 4, 9, 0)
        val inProgress = Activity(
            id = 1, habitId = "weekday", attributedDate = saturday,
            startTime = null, note = "", completedAt = null
        )
        val result = sortAgenda(listOf(weekdayOnly), listOf(inProgress), saturday, satMorning)
        assertThat(result.map { it.habit.id }).containsExactly("weekday")
    }

    @Test
    fun `completed activity on non-active day does not include habit on agenda`() {
        val weekdayOnly = habit(
            "weekday",
            daysActive = setOf(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
            )
        )
        val saturday = LocalDate.of(2026, 4, 4)
        val satMorning = LocalDateTime.of(2026, 4, 4, 9, 0)
        val completed = Activity(
            id = 1, habitId = "weekday", attributedDate = saturday,
            startTime = null, note = "", completedAt = Instant.now()
        )
        val result = sortAgenda(listOf(weekdayOnly), listOf(completed), saturday, satMorning)
        assertThat(result).isEmpty()
    }

    @Test
    fun `multi-time habit completion claims non-past slots first`() {
        val kegel = habit("kegel", timesOfDay = listOf(8, 12, 16),
            dailyTarget = 3, priority = Priority.LOW)

        // at 12:15, 2 completions: claim 12:00 and 16:00 (non-past first)
        // remaining: 8 (past time)
        val at1215 = LocalDateTime.of(2026, 3, 30, 12, 15)
        val activities = listOf(completed("kegel"), completed("kegel"))
        val result = sortAgenda(listOf(kegel), activities, monday, at1215)
        val kegelItem = result.find { it.habit.id == "kegel" }!!
        assertThat(kegelItem.timeOfDay).isEqualTo(8)
        assertThat(kegelItem.isPastTime(at1215)).isTrue()
    }
}
