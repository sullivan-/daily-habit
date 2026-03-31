package com.habit.data

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Test

class DayBoundaryTest {

    private val boundary = DayBoundary(boundaryHour = 2)

    @Test
    fun `attributedDate before boundary returns previous date`() {
        // 2026-03-30 at 1:30 AM should attribute to 2026-03-29
        val instant = ZonedDateTime.of(
            2026, 3, 30, 1, 30, 0, 0, ZoneId.systemDefault()
        ).toInstant()
        assertThat(boundary.attributedDate(instant))
            .isEqualTo(LocalDate.of(2026, 3, 29))
    }

    @Test
    fun `attributedDate at boundary returns current date`() {
        // 2026-03-30 at 2:00 AM should attribute to 2026-03-30
        val instant = ZonedDateTime.of(
            2026, 3, 30, 2, 0, 0, 0, ZoneId.systemDefault()
        ).toInstant()
        assertThat(boundary.attributedDate(instant))
            .isEqualTo(LocalDate.of(2026, 3, 30))
    }

    @Test
    fun `attributedDate after boundary returns current date`() {
        // 2026-03-30 at 10:00 AM should attribute to 2026-03-30
        val instant = ZonedDateTime.of(
            2026, 3, 30, 10, 0, 0, 0, ZoneId.systemDefault()
        ).toInstant()
        assertThat(boundary.attributedDate(instant))
            .isEqualTo(LocalDate.of(2026, 3, 30))
    }

    @Test
    fun `attributedDate at midnight returns previous date`() {
        // 2026-03-30 at 0:00 AM should attribute to 2026-03-29
        val instant = ZonedDateTime.of(
            2026, 3, 30, 0, 0, 0, 0, ZoneId.systemDefault()
        ).toInstant()
        assertThat(boundary.attributedDate(instant))
            .isEqualTo(LocalDate.of(2026, 3, 29))
    }
}
