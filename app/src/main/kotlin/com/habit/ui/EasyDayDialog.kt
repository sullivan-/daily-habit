package com.habit.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
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
    EasyDayLevel.LOW to "Med Hi Effort",
    EasyDayLevel.MEDIUM_LOW to "Med Effort",
    EasyDayLevel.MEDIUM to "Med Lo Effort",
    EasyDayLevel.MEDIUM_HIGH to "Lo Effort"
)

fun easyDayLabel(level: EasyDayLevel): String = when (level) {
    EasyDayLevel.OFF -> "Off"
    EasyDayLevel.LOW -> "Med Hi Effort"
    EasyDayLevel.MEDIUM_LOW -> "Med Effort"
    EasyDayLevel.MEDIUM -> "Med Lo Effort"
    EasyDayLevel.MEDIUM_HIGH -> "Lo Effort"
    EasyDayLevel.HIGH -> "Lo Effort"
}

@Composable
fun EasyDayDialog(
    current: EasyDayLevel,
    carryOver: Boolean,
    onSelect: (EasyDayLevel) -> Unit,
    onToggleCarryOver: (Boolean) -> Unit,
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
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleCarryOver(!carryOver) }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = carryOver,
                        onCheckedChange = { onToggleCarryOver(it) }
                    )
                    Text(
                        text = "Carry over to following days",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
