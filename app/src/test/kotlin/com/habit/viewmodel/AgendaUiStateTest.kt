package com.habit.viewmodel

import com.google.common.truth.Truth.assertThat
import com.habit.data.Activity
import com.habit.data.EasyDayLevel
import com.habit.data.Habit
import com.habit.data.Priority
import com.habit.data.TargetMode
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import org.junit.Test

class AgendaUiStateTest {

    private val monday = LocalDate.of(2026, 6, 22) // Monday — "today"
    private val sunday = LocalDate.of(2026, 6, 21) // Sunday — past day

    private fun habit(
        id: String,
        target: Int = 1,
        mode: TargetMode = TargetMode.AT_LEAST,
        priority: Priority = Priority.MEDIUM,
        days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
        timesOfDay: List<Int> = listOf(7),
        tieBreaker: Int = 0
    ) = Habit(
        id = id,
        name = id,
        timesOfDay = timesOfDay,
        tieBreaker = tieBreaker,
        daysActive = days,
        dailyTarget = target,
        dailyTargetMode = mode,
        timed = false,
        goalMinutes = null,
        stopMinutes = null,
        priority = priority
    )

    private fun done(habitId: String, date: LocalDate, skipped: Boolean = false) = Activity(
        habitId = habitId,
        attributedDate = date,
        startTime = null,
        note = "",
        completedAt = Instant.parse("2026-06-21T12:00:00Z"),
        skipped = skipped
    )

    @Test
    fun `isViewingPastDay is false when selected date equals today`() {
        val s = AgendaUiState(selectedDate = monday, today = monday)
        assertThat(s.isViewingPastDay).isFalse()
    }

    @Test
    fun `isViewingPastDay is true when selected date differs from today`() {
        val s = AgendaUiState(selectedDate = sunday, today = monday)
        assertThat(s.isViewingPastDay).isTrue()
    }

    @Test
    fun `effectiveEasyDay is OFF on a past day even when a level is set`() {
        val s = AgendaUiState(
            selectedDate = sunday, today = monday, easyDayLevel = EasyDayLevel.HIGH
        )
        assertThat(s.effectiveEasyDay).isEqualTo(EasyDayLevel.OFF)
    }

    @Test
    fun `effectiveEasyDay equals the level on today`() {
        val s = AgendaUiState(
            selectedDate = monday, today = monday, easyDayLevel = EasyDayLevel.LOW
        )
        assertThat(s.effectiveEasyDay).isEqualTo(EasyDayLevel.LOW)
    }

    @Test
    fun `missedItems is empty on today`() {
        val s = AgendaUiState(
            habits = listOf(habit("a")), selectedDate = monday, today = monday
        )
        assertThat(s.missedItems).isEmpty()
    }

    @Test
    fun `missedItems includes an under-target active habit with count and target`() {
        val s = AgendaUiState(
            habits = listOf(habit("badux", target = 3)),
            selectedDateActivities = listOf(done("badux", sunday)),
            selectedDate = sunday,
            today = monday
        )
        val missed = s.missedItems
        assertThat(missed).hasSize(1)
        assertThat(missed[0].habit.id).isEqualTo("badux")
        assertThat(missed[0].count).isEqualTo(1)
        assertThat(missed[0].target).isEqualTo(3)
    }

    @Test
    fun `missedItems excludes at-target habits and habits inactive that weekday`() {
        val s = AgendaUiState(
            habits = listOf(
                habit("done", target = 1),
                habit("mondayonly", target = 1, days = setOf(DayOfWeek.MONDAY))
            ),
            selectedDateActivities = listOf(done("done", sunday)),
            selectedDate = sunday,
            today = monday
        )
        assertThat(s.missedItems).isEmpty()
    }

    @Test
    fun `missedItems ignores easy day and uses the raw target`() {
        val s = AgendaUiState(
            habits = listOf(habit("low", target = 1, priority = Priority.LOW)),
            selectedDate = sunday,
            today = monday,
            easyDayLevel = EasyDayLevel.HIGH
        )
        assertThat(s.missedItems.map { it.habit.id }).containsExactly("low")
    }

    @Test
    fun `missedItems treats a skip as not a completion`() {
        val s = AgendaUiState(
            habits = listOf(habit("v", target = 1)),
            selectedDateActivities = listOf(done("v", sunday, skipped = true)),
            selectedDate = sunday,
            today = monday
        )
        assertThat(s.missedItems.map { it.habit.id }).containsExactly("v")
    }

    @Test
    fun `missedItems orders by time of day then priority then tie-breaker`() {
        val s = AgendaUiState(
            habits = listOf(
                habit("late", timesOfDay = listOf(14)),
                habit("earlyLow", timesOfDay = listOf(7), priority = Priority.LOW),
                habit("earlyHigh", timesOfDay = listOf(7), priority = Priority.HIGH)
            ),
            selectedDate = sunday,
            today = monday
        )
        assertThat(s.missedItems.map { it.habit.id })
            .containsExactly("earlyHigh", "earlyLow", "late")
            .inOrder()
    }

    @Test
    fun `pastDayOtherHabits excludes missed rows and exactly-at-target, keeps the rest`() {
        val s = AgendaUiState(
            habits = listOf(
                habit("missed", target = 2),
                habit("atleastdone", target = 1, mode = TargetMode.AT_LEAST),
                habit("exactlydone", target = 1, mode = TargetMode.EXACTLY),
                habit("mondayonly", target = 1, days = setOf(DayOfWeek.MONDAY))
            ),
            selectedDateActivities = listOf(
                done("atleastdone", sunday),
                done("exactlydone", sunday)
            ),
            selectedDate = sunday,
            today = monday
        )
        assertThat(s.pastDayOtherHabits.map { it.id })
            .containsExactly("atleastdone", "mondayonly")
    }

    @Test
    fun `totalTarget and progressCount use the selected date and raw targets`() {
        val s = AgendaUiState(
            habits = listOf(
                habit("a", target = 2),
                habit("mondayonly", target = 1, days = setOf(DayOfWeek.MONDAY))
            ),
            selectedDateActivities = listOf(done("a", sunday)),
            selectedDate = sunday,
            today = monday,
            easyDayLevel = EasyDayLevel.HIGH
        )
        assertThat(s.totalTarget).isEqualTo(2)
        assertThat(s.progressCount).isEqualTo(1)
    }
}
