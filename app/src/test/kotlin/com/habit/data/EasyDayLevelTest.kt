package com.habit.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EasyDayLevelTest {

    @Test
    fun `OFF includes every priority`() {
        Priority.entries.forEach { p ->
            assertThat(EasyDayLevel.OFF.includes(p)).isTrue()
        }
    }

    @Test
    fun `LOW excludes only LOW`() {
        assertThat(EasyDayLevel.LOW.includes(Priority.LOW)).isFalse()
        assertThat(EasyDayLevel.LOW.includes(Priority.MEDIUM_LOW)).isTrue()
        assertThat(EasyDayLevel.LOW.includes(Priority.MEDIUM)).isTrue()
        assertThat(EasyDayLevel.LOW.includes(Priority.MEDIUM_HIGH)).isTrue()
        assertThat(EasyDayLevel.LOW.includes(Priority.HIGH)).isTrue()
    }

    @Test
    fun `MEDIUM_LOW excludes LOW and MEDIUM_LOW`() {
        assertThat(EasyDayLevel.MEDIUM_LOW.includes(Priority.LOW)).isFalse()
        assertThat(EasyDayLevel.MEDIUM_LOW.includes(Priority.MEDIUM_LOW)).isFalse()
        assertThat(EasyDayLevel.MEDIUM_LOW.includes(Priority.MEDIUM)).isTrue()
        assertThat(EasyDayLevel.MEDIUM_LOW.includes(Priority.MEDIUM_HIGH)).isTrue()
        assertThat(EasyDayLevel.MEDIUM_LOW.includes(Priority.HIGH)).isTrue()
    }

    @Test
    fun `MEDIUM excludes LOW MEDIUM_LOW MEDIUM`() {
        assertThat(EasyDayLevel.MEDIUM.includes(Priority.LOW)).isFalse()
        assertThat(EasyDayLevel.MEDIUM.includes(Priority.MEDIUM_LOW)).isFalse()
        assertThat(EasyDayLevel.MEDIUM.includes(Priority.MEDIUM)).isFalse()
        assertThat(EasyDayLevel.MEDIUM.includes(Priority.MEDIUM_HIGH)).isTrue()
        assertThat(EasyDayLevel.MEDIUM.includes(Priority.HIGH)).isTrue()
    }

    @Test
    fun `MEDIUM_HIGH includes only HIGH`() {
        assertThat(EasyDayLevel.MEDIUM_HIGH.includes(Priority.LOW)).isFalse()
        assertThat(EasyDayLevel.MEDIUM_HIGH.includes(Priority.MEDIUM_LOW)).isFalse()
        assertThat(EasyDayLevel.MEDIUM_HIGH.includes(Priority.MEDIUM)).isFalse()
        assertThat(EasyDayLevel.MEDIUM_HIGH.includes(Priority.MEDIUM_HIGH)).isFalse()
        assertThat(EasyDayLevel.MEDIUM_HIGH.includes(Priority.HIGH)).isTrue()
    }

    @Test
    fun `HIGH excludes everything`() {
        Priority.entries.forEach { p ->
            assertThat(EasyDayLevel.HIGH.includes(p)).isFalse()
        }
    }

    @Test
    fun `OFF keeps the full target`() {
        assertThat(EasyDayLevel.OFF.effectiveTarget(Priority.MEDIUM, 3)).isEqualTo(3)
    }

    @Test
    fun `medium 3x degrades by one occurrence per effort step`() {
        // Med Effort still shows all three; each stronger level sheds one
        assertThat(EasyDayLevel.MEDIUM_LOW.effectiveTarget(Priority.MEDIUM, 3)).isEqualTo(3)
        assertThat(EasyDayLevel.MEDIUM.effectiveTarget(Priority.MEDIUM, 3)).isEqualTo(2)
        assertThat(EasyDayLevel.MEDIUM_HIGH.effectiveTarget(Priority.MEDIUM, 3)).isEqualTo(1)
        assertThat(EasyDayLevel.HIGH.effectiveTarget(Priority.MEDIUM, 3)).isEqualTo(0)
    }

    @Test
    fun `single-occurrence habit is all-or-nothing like includes`() {
        assertThat(EasyDayLevel.MEDIUM_LOW.effectiveTarget(Priority.MEDIUM, 1)).isEqualTo(1)
        assertThat(EasyDayLevel.MEDIUM.effectiveTarget(Priority.MEDIUM, 1)).isEqualTo(0)
    }
}
