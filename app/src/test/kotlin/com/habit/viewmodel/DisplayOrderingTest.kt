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
        tieBreaker: Int = 4,
        dailyTarget: Int = 1,
        priority: Priority = Priority.MEDIUM,
        daysActive: Set<DayOfWeek> = DayOfWeek.entries.toSet()
    ) = Habit(
        id = id,
        name = id,
        timesOfDay = timesOfDay,
        tieBreaker = tieBreaker,
        daysActive = daysActive,
        dailyTarget = dailyTarget,
        dailyTargetMode = TargetMode.AT_LEAST,
        timed = false,


        goalMinutes = null,
        stopMinutes = null,
        priority = priority
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
    fun `first activities sort by time of day then tie breaker`() {
        val habits = listOf(
            habit("afternoon", timesOfDay = listOf(14), tieBreaker = 4),
            habit("morning-b", timesOfDay = listOf(7), tieBreaker = 3),
            habit("morning-a", timesOfDay = listOf(7), tieBreaker = 4)
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
    fun `subsequent activities sort by priority then tie breaker`() {
        val habits = listOf(
            habit("low", dailyTarget = 2, priority = Priority.LOW, tieBreaker = 4),
            habit("high", dailyTarget = 2, priority = Priority.HIGH, tieBreaker = 4)
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
        // all priorities use 270min grace, so 8:00 slot is past at 12:31
        val habits = listOf(
            habit("low-8", timesOfDay = listOf(8), priority = Priority.LOW),
            habit("high-8", timesOfDay = listOf(8), priority = Priority.HIGH),
            habit("afternoon", timesOfDay = listOf(14), priority = Priority.MEDIUM)
        )
        // at 13:00, both 8:00 slots are past time (8:00 + 270min = 12:30)
        val at1pm = LocalDateTime.of(2026, 3, 30, 13, 0)
        val result = sortAgenda(habits, emptyList(), monday, at1pm)
        // afternoon (not past) sorts first, then the two past 8:00 slots by priority
        assertThat(result.map { it.habit.id })
            .containsExactly("afternoon", "high-8", "low-8")
            .inOrder()
    }

    @Test
    fun `grace period is uniform across priorities`() {
        val habits = listOf(
            habit("low", timesOfDay = listOf(8), priority = Priority.LOW),
            habit("med", timesOfDay = listOf(8), priority = Priority.MEDIUM),
            habit("high", timesOfDay = listOf(8), priority = Priority.HIGH)
        )
        // at 11:00: all have 270min grace (deadline 12:30), none past time
        // all at same time slot, so sorted by priority
        val at11am = LocalDateTime.of(2026, 3, 30, 11, 0)
        val result = sortAgenda(habits, emptyList(), monday, at11am)
        assertThat(result.map { it.habit.id })
            .containsExactly("high", "med", "low")
            .inOrder()
    }

    @Test
    fun `multi-time habit uses best unclaimed slot`() {
        // all priorities use 270min grace
        val kegel = habit("kegel", timesOfDay = listOf(8, 12, 16),
            dailyTarget = 3, priority = Priority.LOW)
        val other = habit("other", timesOfDay = listOf(13), priority = Priority.MEDIUM)

        // at 13:00, 0 completions: 8 is past time (8+270min=12:30)
        // 12 and 16 are not past; kegel should get the 12:00 slot
        val at1300 = LocalDateTime.of(2026, 3, 30, 13, 0)
        val result = sortAgenda(listOf(kegel, other), emptyList(), monday, at1300)
        assertThat(result.first().habit.id).isEqualTo("kegel")
        assertThat(result.first().timeOfDay).isEqualTo(12)
    }

    @Test
    fun `multi-time habit after completion picks best remaining slot`() {
        val kegel = habit("kegel", timesOfDay = listOf(8, 12, 16),
            dailyTarget = 3, priority = Priority.LOW)
        val other = habit("other", timesOfDay = listOf(13), priority = Priority.MEDIUM)

        // at 12:15 (all slots non-past with 270min grace), 1 completion
        // claims 8:00; remaining: 12 and 16; returns 12
        val at1215 = LocalDateTime.of(2026, 3, 30, 12, 15)
        val activities = listOf(completed("kegel"))
        val result = sortAgenda(listOf(kegel, other), activities, monday, at1215)
        val kegelItem = result.find { it.habit.id == "kegel" }!!
        assertThat(kegelItem.timeOfDay).isEqualTo(12)
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
    fun `multi-time habit completion claims past slots first`() {
        val kegel = habit("kegel", timesOfDay = listOf(8, 12, 16),
            dailyTarget = 3, priority = Priority.LOW)

        // at 12:15 (all non-past with 270min grace), 2 completions
        // claim 8:00 and 12:00; remaining: 16
        val at1215 = LocalDateTime.of(2026, 3, 30, 12, 15)
        val activities = listOf(completed("kegel"), completed("kegel"))
        val result = sortAgenda(listOf(kegel), activities, monday, at1215)
        val kegelItem = result.find { it.habit.id == "kegel" }!!
        assertThat(kegelItem.timeOfDay).isEqualTo(16)
        assertThat(kegelItem.isPastTime(at1215)).isFalse()
    }
}
