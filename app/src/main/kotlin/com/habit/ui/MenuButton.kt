package com.habit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MenuButton(
    onNewHabit: () -> Unit,
    onHabitList: () -> Unit,
    onChoices: () -> Unit = {},
    onEasyDay: (() -> Unit)? = null,
    easyDaySubLabel: String? = null,
    onDayPlan: (() -> Unit)? = null,
    onDoneToday: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val easyDayLabel = if (easyDaySubLabel != null)
        "Easy Day · $easyDaySubLabel"
    else
        "Easy Day"

    val entries = buildList {
        if (onDayPlan != null) add("Day Plan" to onDayPlan)
        if (onDoneToday != null) add("Done Today" to onDoneToday)
        add("Choices" to onChoices)
        if (onEasyDay != null) add(easyDayLabel to onEasyDay)
        add("Edit Habits" to onHabitList)
        add("New Habit" to onNewHabit)
    }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.Menu, "menu", tint = Color.White)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = androidx.compose.ui.unit.DpOffset(0.dp, (-4).dp),
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            entries.forEachIndexed { index, (label, onClick) ->
                if (index > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant
                            .copy(alpha = 0.3f)
                    )
                }
                DropdownMenuItem(
                    text = {
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    },
                    onClick = {
                        expanded = false
                        onClick()
                    }
                )
            }
        }
    }
}
