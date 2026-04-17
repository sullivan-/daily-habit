package com.habit.viewmodel

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IntervalOptionsTest {

    @Test
    fun `labelFor returns seconds format for sub-minute intervals`() {
        assertThat(IntervalOptions.labelFor(8_000)).isEqualTo("8s")
        assertThat(IntervalOptions.labelFor(12_000)).isEqualTo("12s")
    }

    @Test
    fun `labelFor returns minutes format for minute intervals`() {
        assertThat(IntervalOptions.labelFor(180_000)).isEqualTo("3m")
        assertThat(IntervalOptions.labelFor(600_000)).isEqualTo("10m")
    }

    @Test
    fun `isSecondsInterval returns true for under 60000`() {
        assertThat(IntervalOptions.isSecondsInterval(8_000)).isTrue()
        assertThat(IntervalOptions.isSecondsInterval(59_999)).isTrue()
    }

    @Test
    fun `isSecondsInterval returns false for 60000 and above`() {
        assertThat(IntervalOptions.isSecondsInterval(60_000)).isFalse()
        assertThat(IntervalOptions.isSecondsInterval(180_000)).isFalse()
    }
}
