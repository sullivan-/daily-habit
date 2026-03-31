package com.habit.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class DayBoundary(private val boundaryHour: Int) {
    fun today(): LocalDate {
        val now = LocalDateTime.now()
        return if (now.hour < boundaryHour) {
            now.toLocalDate().minusDays(1)
        } else {
            now.toLocalDate()
        }
    }

    fun attributedDate(instant: Instant): LocalDate {
        val local = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()
        return if (local.hour < boundaryHour) {
            local.toLocalDate().minusDays(1)
        } else {
            local.toLocalDate()
        }
    }
}
