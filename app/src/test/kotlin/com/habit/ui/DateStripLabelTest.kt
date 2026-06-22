package com.habit.ui

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class DateStripLabelTest {

    private val today = LocalDate.of(2026, 6, 22) // Monday

    @Test
    fun `selected today is Today`() {
        assertThat(dateStripLabel(today, today)).isEqualTo("Today")
    }

    @Test
    fun `one day before is Yesterday`() {
        assertThat(dateStripLabel(today.minusDays(1), today)).isEqualTo("Yesterday")
    }

    @Test
    fun `two days before is weekday month day`() {
        // 2026-06-20 is a Saturday
        assertThat(dateStripLabel(today.minusDays(2), today)).isEqualTo("Sat Jun 20")
    }

    @Test
    fun `oldest reachable day formats as a date with no year`() {
        // 2026-06-15 is a Monday
        assertThat(dateStripLabel(today.minusDays(7), today)).isEqualTo("Mon Jun 15")
    }
}
