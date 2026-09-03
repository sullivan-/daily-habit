package com.habit.viewmodel

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IntervalRescheduleTest {

    @Test
    fun `lengthening the interval keeps the position within the cycle`() {
        // 5s into an 8s cycle, switched to 9s: 4s remain
        val r = IntervalReschedule.of(nextAtMs = 8_000, oldMs = 8_000, newMs = 9_000, elapsedMs = 5_000)
        assertThat(r.nextAtMs).isEqualTo(9_000)
        assertThat(r.chimeNow).isFalse()
    }

    @Test
    fun `shortening below the elapsed part of the cycle chimes now and restarts`() {
        // 5s into an 8s cycle, switched to 4s: chime, then the next one 4s from now
        val r = IntervalReschedule.of(nextAtMs = 8_000, oldMs = 8_000, newMs = 4_000, elapsedMs = 5_000)
        assertThat(r.nextAtMs).isEqualTo(9_000)
        assertThat(r.chimeNow).isTrue()
    }

    @Test
    fun `an interval that lands exactly now chimes now`() {
        val r = IntervalReschedule.of(nextAtMs = 8_000, oldMs = 8_000, newMs = 5_000, elapsedMs = 5_000)
        assertThat(r.nextAtMs).isEqualTo(10_000)
        assertThat(r.chimeNow).isTrue()
    }

    @Test
    fun `works within a later cycle`() {
        // third 8s cycle runs 16s to 24s; 21s in, switched to 6s, the next chime is at 22s
        val r = IntervalReschedule.of(nextAtMs = 24_000, oldMs = 8_000, newMs = 6_000, elapsedMs = 21_000)
        assertThat(r.nextAtMs).isEqualTo(22_000)
        assertThat(r.chimeNow).isFalse()
    }
}
