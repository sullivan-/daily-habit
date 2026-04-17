package com.habit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.habit.viewmodel.IntervalChimeState
import com.habit.viewmodel.IntervalOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntervalChimeControl(
    state: IntervalChimeState,
    intervalMs: Long,
    countdownMs: Long,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    onSelect: (Long) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        IntervalChimeState.IDLE -> {
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onOpen) {
                    Text("Interval Chime")
                }
            }
        }
        IntervalChimeState.SELECTING -> {
            Column(modifier = modifier) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IntervalOptions.seconds.forEach { sec ->
                        FilterChip(
                            selected = false,
                            onClick = { onSelect(sec * 1000L) },
                            label = { Text("${sec}s") }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IntervalOptions.minutes.forEach { min ->
                        FilterChip(
                            selected = false,
                            onClick = { onSelect(min * 60_000L) },
                            label = { Text("${min}m") }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onClose) {
                        Text("Close")
                    }
                }
            }
        }
        IntervalChimeState.RUNNING -> {
            Row(
                modifier = modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("every ${IntervalOptions.labelFor(intervalMs)}")
                val countdownText = formatCountdown(countdownMs, intervalMs)
                Text(
                    text = " — next in $countdownText",
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onCancel) { Text("Cancel") }
            }
        }
    }
}

private fun formatCountdown(countdownMs: Long, intervalMs: Long): String {
    val totalSec = (countdownMs + 999) / 1000
    return if (IntervalOptions.isSecondsInterval(intervalMs)) {
        "$totalSec"
    } else {
        val min = totalSec / 60
        val sec = totalSec % 60
        "%d:%02d".format(min, sec)
    }
}
