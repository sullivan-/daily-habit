package com.habit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val green = Color(0xFF4CAF50)
private val blue = Color(0xFF2196F3)
private val red = Color(0xFFF44336)
private const val HALF = 0.1f

fun gradientStops(cot: Float, coe: Float): Array<Pair<Float, Color>> {
    val greenSize = cot
    val blueSize = coe - cot
    val redSize = 1f - coe
    val skipBlue = blueSize < 0.001f

    val stops = mutableListOf<Pair<Float, Color>>()

    if (skipBlue) {
        if (greenSize < 0.001f) {
            stops.add(0f to red)
            stops.add(1f to red)
        } else if (redSize < 0.001f) {
            stops.add(0f to green)
            stops.add(1f to green)
        } else {
            val left = minOf(HALF, greenSize)
            val right = minOf(HALF, redSize)
            stops.add(0f to green)
            if (cot - left > 0.001f) stops.add((cot - left) to green)
            if (cot + right < 0.999f) stops.add((cot + right) to red)
            stops.add(1f to red)
        }
    } else if (greenSize < 0.001f) {
        val b2rLeft = minOf(HALF, blueSize)
        val b2rRight = minOf(HALF, redSize)
        stops.add(0f to blue)
        if (redSize < 0.001f) {
            stops.add(1f to blue)
        } else {
            val blueEnd = coe - b2rLeft
            if (blueEnd > 0.001f) stops.add(blueEnd to blue)
            val redStart = (coe + b2rRight).coerceAtMost(1f)
            if (redStart < 0.999f) stops.add(redStart to red)
            stops.add(1f to red)
        }
    } else if (redSize < 0.001f) {
        val g2bLeft = minOf(HALF, greenSize)
        val g2bRight = minOf(HALF, blueSize)
        stops.add(0f to green)
        val greenEnd = cot - g2bLeft
        if (greenEnd > 0.001f) stops.add(greenEnd to green)
        val blueStart = (cot + g2bRight).coerceAtMost(1f)
        if (blueStart < 0.999f) stops.add(blueStart to blue)
        stops.add(1f to blue)
    } else {
        val g2bLeft = minOf(HALF, greenSize)
        val g2bRight = minOf(HALF, blueSize / 2f)
        val b2rLeft = minOf(HALF, blueSize / 2f)
        val b2rRight = minOf(HALF, redSize)

        stops.add(0f to green)
        val greenEnd = cot - g2bLeft
        if (greenEnd > 0.001f) stops.add(greenEnd to green)
        stops.add((cot + g2bRight) to blue)
        val blueEnd = coe - b2rLeft
        if (blueEnd > cot + g2bRight + 0.001f) stops.add(blueEnd to blue)
        val redStart = (coe + b2rRight).coerceAtMost(1f)
        if (redStart < 0.999f) stops.add(redStart to red)
        stops.add(1f to red)
    }

    return stops.toTypedArray()
}

@Composable
fun ProgressBar(
    completed: Int,
    total: Int,
    completedOverTotal: Float,
    completedOverExpected: Float,
    onClick: () -> Unit,
    onNewHabit: () -> Unit,
    onHabitList: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cot = completedOverTotal.coerceIn(0f, 1f)
    val coe = completedOverExpected.coerceIn(cot, 1f)

    val gradient = Brush.horizontalGradient(
        colorStops = gradientStops(cot, coe)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(gradient)
    ) {
        MenuButton(onNewHabit = onNewHabit, onHabitList = onHabitList, modifier = Modifier.align(Alignment.CenterStart))
        Text(
            text = "$completed/$total",
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .align(Alignment.Center)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp)
        )
    }
}
