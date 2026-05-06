package com.habit.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.habit.data.EasyDayLevel

private val easyDayOptions = listOf(
    EasyDayLevel.OFF to "Regular day",
    EasyDayLevel.LOW to "Skip Lo",
    EasyDayLevel.MEDIUM_LOW to "Skip Med Lo and below",
    EasyDayLevel.MEDIUM to "Skip Med and below",
    EasyDayLevel.MEDIUM_HIGH to "Skip everything but Hi",
    EasyDayLevel.HIGH to "Skip everything"
)

fun easyDayLabel(level: EasyDayLevel): String = when (level) {
    EasyDayLevel.OFF -> "Off"
    EasyDayLevel.LOW -> "Lo"
    EasyDayLevel.MEDIUM_LOW -> "Med Lo"
    EasyDayLevel.MEDIUM -> "Med"
    EasyDayLevel.MEDIUM_HIGH -> "Med Hi"
    EasyDayLevel.HIGH -> "Hi"
}

@Composable
fun EasyDayDialog(
    current: EasyDayLevel,
    onSelect: (EasyDayLevel) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Easy Day") },
        text = {
            Column {
                easyDayOptions.forEach { (level, description) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(level)
                                onDismiss()
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = level == current,
                            onClick = {
                                onSelect(level)
                                onDismiss()
                            }
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
