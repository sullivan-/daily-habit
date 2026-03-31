package com.habit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.habit.viewmodel.ProgressColor

@Composable
fun ProgressBar(
    completed: Int,
    total: Int,
    color: ProgressColor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when (color) {
        ProgressColor.BLUE -> Color(0xFF2196F3)
        ProgressColor.GREEN -> Color(0xFF4CAF50)
        ProgressColor.RED -> Color(0xFFF44336)
    }

    val fraction = if (total > 0) completed.toFloat() / total else 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick)
            .background(Color.DarkGray)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .background(bgColor)
        )
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
