package com.habit.data

import com.google.common.truth.Truth.assertThat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `round-trip DayOfWeek set`() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        val encoded = converters.fromDayOfWeekSet(days)
        val decoded = converters.toDayOfWeekSet(encoded)
        assertThat(decoded).isEqualTo(days)
    }

    @Test
    fun `empty DayOfWeek set`() {
        val encoded = converters.fromDayOfWeekSet(emptySet())
        val decoded = converters.toDayOfWeekSet(encoded)
        assertThat(decoded).isEmpty()
    }

    @Test
    fun `round-trip nullable DayOfWeek`() {
        val encoded = converters.fromDayOfWeek(DayOfWeek.MONDAY)
        val decoded = converters.toDayOfWeek(encoded)
        assertThat(decoded).isEqualTo(DayOfWeek.MONDAY)
    }

    @Test
    fun `null DayOfWeek round-trip`() {
        val encoded = converters.fromDayOfWeek(null)
        assertThat(encoded).isNull()
        val decoded = converters.toDayOfWeek(null)
        assertThat(decoded).isNull()
    }

    @Test
    fun `round-trip LocalDate`() {
        val date = LocalDate.of(2026, 3, 30)
        val encoded = converters.fromLocalDate(date)
        val decoded = converters.toLocalDate(encoded)
        assertThat(decoded).isEqualTo(date)
    }

    @Test
    fun `round-trip Instant`() {
        val instant = Instant.ofEpochMilli(1711800000000)
        val encoded = converters.fromInstant(instant)
        val decoded = converters.toInstant(encoded)
        assertThat(decoded).isEqualTo(instant)
    }

    @Test
    fun `null Instant round-trip`() {
        val encoded = converters.fromInstant(null)
        assertThat(encoded).isNull()
        val decoded = converters.toInstant(null)
        assertThat(decoded).isNull()
    }

    @Test
    fun `round-trip enums`() {
        assertThat(converters.toTargetMode(converters.fromTargetMode(TargetMode.AT_LEAST)))
            .isEqualTo(TargetMode.AT_LEAST)
        assertThat(converters.toPriority(converters.fromPriority(Priority.HIGH)))
            .isEqualTo(Priority.HIGH)
    }
}
