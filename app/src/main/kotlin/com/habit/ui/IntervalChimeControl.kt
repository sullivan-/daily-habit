package com.habit.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.habit.viewmodel.IntervalChimeState
import com.habit.viewmodel.IntervalOptions

@Composable
fun IntervalChimeControl(
    state: IntervalChimeState,
    intervalMs: Long,
    countdownMs: Long,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    onSelect: (Long) -> Unit,
    onChange: (Long) -> Unit,
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
            // while chimes are already running, a chip changes the interval in place
            val onPick = if (intervalMs > 0) onChange else onSelect
            Column(modifier = modifier) {
                IntervalChipRow(
                    options = IntervalOptions.seconds.map { it * 1000L to "${it}s" },
                    currentMs = intervalMs,
                    onPick = onPick
                )
                IntervalChipRow(
                    options = IntervalOptions.minutes.map { it * 60_000L to "${it}m" },
                    currentMs = intervalMs,
                    onPick = onPick
                )
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
                Text(
                    text = "every ${IntervalOptions.labelFor(intervalMs)}",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onOpen)
                )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntervalChipRow(
    options: List<Pair<Long, String>>,
    currentMs: Long,
    onPick: (Long) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { (ms, label) ->
            FilterChip(
                selected = ms == currentMs,
                onClick = { onPick(ms) },
                label = {
                    Text(
                        label,
                        maxLines = 1,
                        overflow = TextOverflow.Visible,
                        softWrap = false
                    )
                }
            )
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
