package com.habit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
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

@Composable
fun ProgressBar(
    completed: Int,
    total: Int,
    completedOverTotal: Float,
    expectedOverTotal: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stops = buildList {
        add(0f to green)
        if (completedOverTotal > 0f) {
            add(completedOverTotal to blue)
        } else {
            add(0f to blue)
        }
        if (expectedOverTotal > completedOverTotal) {
            add(expectedOverTotal to red)
        }
        add(1f to red)
    }

    val gradient = Brush.horizontalGradient(
        colorStops = stops.toTypedArray()
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick)
            .background(gradient)
    ) {
        Text(
            text = "$completed/$total",
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 16.dp)
        )
    }
}
