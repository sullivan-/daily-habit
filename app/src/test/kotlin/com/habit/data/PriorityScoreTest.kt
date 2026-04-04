package com.habit.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PriorityScoreTest {

    @Test
    fun `HIGH maps to 1 point 0`() {
        assertThat(priorityToScore(Priority.HIGH)).isEqualTo(1.0f)
    }

    @Test
    fun `MEDIUM_HIGH maps to 0 point 8`() {
        assertThat(priorityToScore(Priority.MEDIUM_HIGH)).isEqualTo(0.8f)
    }

    @Test
    fun `MEDIUM maps to 0 point 6`() {
        assertThat(priorityToScore(Priority.MEDIUM)).isEqualTo(0.6f)
    }

    @Test
    fun `MEDIUM_LOW maps to 0 point 4`() {
        assertThat(priorityToScore(Priority.MEDIUM_LOW)).isEqualTo(0.4f)
    }

    @Test
    fun `LOW maps to 0 point 2`() {
        assertThat(priorityToScore(Priority.LOW)).isEqualTo(0.2f)
    }
}
