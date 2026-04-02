package com.habit.ui

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProgressBarGradientTest {

    private val green = Color(0xFF4CAF50)
    private val blue = Color(0xFF2196F3)
    private val red = Color(0xFFF44336)

    @Test
    fun `cot=0 coe=0 is all red`() {
        val stops = gradientStops(0f, 0f)
        assertThat(stops.size).isEqualTo(2)
        assertThat(stops.first().second).isEqualTo(red)
        assertThat(stops.last().second).isEqualTo(red)
    }

    @Test
    fun `cot=1 coe=1 is all green`() {
        val stops = gradientStops(1f, 1f)
        assertThat(stops.size).isEqualTo(2)
        assertThat(stops.first().second).isEqualTo(green)
        assertThat(stops.last().second).isEqualTo(green)
    }

    @Test
    fun `cot=0 coe=1 is all blue`() {
        val stops = gradientStops(0f, 1f)
        assertThat(stops.first().second).isEqualTo(blue)
        assertThat(stops.last().second).isEqualTo(blue)
    }

    @Test
    fun `cot=0 coe=0_85 starts blue ends red`() {
        val stops = gradientStops(0f, 0.85f)
        assertThat(stops.first().second).isEqualTo(blue)
        assertThat(stops.last().second).isEqualTo(red)
        // no green anywhere
        assertThat(stops.none { it.second == green }).isTrue()
    }

    @Test
    fun `cot=0_3 coe=0_7 has all three colors`() {
        val stops = gradientStops(0.3f, 0.7f)
        assertThat(stops.first().second).isEqualTo(green)
        assertThat(stops.last().second).isEqualTo(red)
        assertThat(stops.any { it.second == blue }).isTrue()
    }

    @Test
    fun `cot=0_5 coe=0_5 skips blue goes green to red`() {
        val stops = gradientStops(0.5f, 0.5f)
        assertThat(stops.first().second).isEqualTo(green)
        assertThat(stops.last().second).isEqualTo(red)
        assertThat(stops.none { it.second == blue }).isTrue()
    }

    @Test
    fun `stops are monotonically increasing`() {
        val cases = listOf(
            0f to 0f, 0f to 0.5f, 0f to 1f,
            0.3f to 0.7f, 0.5f to 0.5f, 0.5f to 1f,
            0.1f to 0.15f, 0.95f to 1f, 1f to 1f
        )
        for ((cot, coe) in cases) {
            val stops = gradientStops(cot, coe)
            for (i in 1 until stops.size) {
                assertThat(stops[i].first)
                    .isAtLeast(stops[i - 1].first)
            }
        }
    }
}
