package com.habit.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val stripDateFormat = DateTimeFormatter.ofPattern("EEE MMM d")

fun dateStripLabel(selected: LocalDate, today: LocalDate): String = when (selected) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    else -> selected.format(stripDateFormat)
}

@Composable
fun DateStrip(
    label: String,
    canStepBack: Boolean,
    canStepForward: Boolean,
    onStepBack: () -> Unit,
    onStepForward: () -> Unit,
    onLabelTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        IconButton(
            onClick = onStepBack,
            enabled = canStepBack,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = "previous day")
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .align(Alignment.Center)
                .clickable(onClick = onLabelTap)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        IconButton(
            onClick = onStepForward,
            enabled = canStepForward,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(Icons.Filled.ChevronRight, contentDescription = "next day")
        }
    }
}
