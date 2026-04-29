package com.habit.viewmodel

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Test

class StreakFormatterTest {

    private fun instantAtStartOfDay(date: LocalDate) =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant()

    @Test
    fun `23 hours 59 minutes returns null`() {
        val start = instantAtStartOfDay(LocalDate.of(2026, 1, 1))
        val now = start.plusSeconds(23 * 3600 + 59 * 60)
        assertThat(formatStreakDuration(start, now)).isNull()
    }

    @Test
    fun `exactly 24 hours returns 1 day streak`() {
        val start = instantAtStartOfDay(LocalDate.of(2026, 1, 1))
        val now = start.plusSeconds(24 * 3600)
        assertThat(formatStreakDuration(start, now)).isEqualTo("1 day streak")
    }

    @Test
    fun `59 days returns 59 day streak`() {
        val start = instantAtStartOfDay(LocalDate.of(2026, 1, 1))
        val end = instantAtStartOfDay(LocalDate.of(2026, 3, 1))
        assertThat(formatStreakDuration(start, end)).isEqualTo("59 day streak")
    }

    @Test
    fun `60 days returns 2 month streak`() {
        val start = instantAtStartOfDay(LocalDate.of(2026, 1, 1))
        val end = instantAtStartOfDay(LocalDate.of(2026, 3, 2))
        assertThat(formatStreakDuration(start, end)).isEqualTo("2 month streak")
    }

    @Test
    fun `89 days with 2 calendar months shows 2 month streak`() {
        // jan 1 to mar 31 = 89 days, Period.between = 2 months 30 days
        val start = instantAtStartOfDay(LocalDate.of(2026, 1, 1))
        val end = instantAtStartOfDay(LocalDate.of(2026, 3, 31))
        val result = formatStreakDuration(start, end)
        assertThat(result).isEqualTo("2 month streak")
    }

    @Test
    fun `11 months 29 days returns 11 month streak`() {
        val start = instantAtStartOfDay(LocalDate.of(2025, 1, 1))
        val end = instantAtStartOfDay(LocalDate.of(2025, 12, 30))
        assertThat(formatStreakDuration(start, end)).isEqualTo("11 month streak")
    }

    @Test
    fun `exactly 12 months returns 1 year streak`() {
        val start = instantAtStartOfDay(LocalDate.of(2025, 1, 1))
        val end = instantAtStartOfDay(LocalDate.of(2026, 1, 1))
        assertThat(formatStreakDuration(start, end)).isEqualTo("1 year streak")
    }

    @Test
    fun `4 years 3 months returns 4 year streak`() {
        val start = instantAtStartOfDay(LocalDate.of(2022, 1, 1))
        val end = instantAtStartOfDay(LocalDate.of(2026, 4, 1))
        assertThat(formatStreakDuration(start, end)).isEqualTo("4 year streak")
    }
}
