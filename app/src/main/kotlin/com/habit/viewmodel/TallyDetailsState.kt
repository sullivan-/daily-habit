package com.habit.viewmodel

import com.habit.data.Priority
import java.time.Instant
import java.time.LocalDate

data class TallyDetailsState(
    val id: String = "",
    val name: String = "",
    val priority: Priority = Priority.MEDIUM,
    val isNew: Boolean = true,
    val dirty: Boolean = false,
    val saved: Boolean = false,
    val deleted: Boolean = false,
    val streakStart: Instant? = null,
    val streakCount: Int = 0,
    val abstainCountLast10: Int = 0,
    val totalCountLast10: Int = 0,
    val abstainCountToday: Int = 0,
    val totalCountToday: Int = 0,
    val hasChoices: Boolean = false,
    val earliestChoiceDate: LocalDate? = null
) {
    val isValid: Boolean get() = name.isNotBlank()
    val showTodayStats: Boolean get() = totalCountToday >= 3
}
