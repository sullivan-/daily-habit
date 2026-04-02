package com.habit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.habit.data.Priority
import com.habit.data.TargetMode
import com.habit.data.ThresholdType
import com.habit.viewmodel.HabitEditorViewModel
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HabitEditorScreen(
    viewModel: HabitEditorViewModel,
    habitId: String?,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(habitId) {
        if (habitId != null) viewModel.loadHabit(habitId)
    }

    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    var showDiscardDialog by remember { mutableStateOf(false) }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard changes?") },
            confirmButton = {
                TextButton(onClick = onBack) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep editing")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (state.isNew) "New Habit" else "Edit Habit")
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.dirty) showDiscardDialog = true else onBack()
                    }) {
                        Icon(Icons.Filled.ArrowBack, "back")
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.save() },
                        enabled = state.isValid
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text("Times of day", style = MaterialTheme.typography.labelLarge)
            TimesOfDayChips(
                times = state.timesOfDay,
                onAdd = viewModel::addTimeOfDay,
                onRemove = viewModel::removeTimeOfDay
            )

            Text("Days active", style = MaterialTheme.typography.labelLarge)
            DaysActiveRow(
                daysActive = state.daysActive,
                onToggle = viewModel::toggleDayActive
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.dailyTarget.toString(),
                    onValueChange = {
                        it.toIntOrNull()?.let(viewModel::setDailyTarget)
                    },
                    label = { Text("Daily target") },
                    modifier = Modifier.width(120.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true
                )
                Spacer(Modifier.width(16.dp))
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = state.dailyTargetMode == TargetMode.AT_LEAST,
                        onClick = {
                            viewModel.setDailyTargetMode(TargetMode.AT_LEAST)
                        },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) { Text("At least") }
                    SegmentedButton(
                        selected = state.dailyTargetMode == TargetMode.EXACTLY,
                        onClick = {
                            viewModel.setDailyTargetMode(TargetMode.EXACTLY)
                        },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) { Text("Exactly") }
                }
            }

            OutlinedTextField(
                value = state.sortOrder.toString(),
                onValueChange = {
                    it.toIntOrNull()?.let(viewModel::setSortOrder)
                },
                label = { Text("Sort order") },
                modifier = Modifier.width(120.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                singleLine = true
            )

            Text("Priority", style = MaterialTheme.typography.labelLarge)
            PrioritySelector(
                priority = state.priority,
                onSelect = viewModel::setPriority
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = state.timed,
                    onCheckedChange = { viewModel.setTimed(it) }
                )
                Text("Timed habit")
            }

            if (state.timed) {
                OutlinedTextField(
                    value = state.chimeIntervalSeconds?.toString() ?: "",
                    onValueChange = {
                        viewModel.setChimeIntervalSeconds(it.toIntOrNull())
                    },
                    label = { Text("Chime interval (seconds)") },
                    modifier = Modifier.width(220.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = state.thresholdMinutes?.toString() ?: "",
                    onValueChange = {
                        viewModel.setThresholdMinutes(it.toIntOrNull())
                    },
                    label = { Text("Threshold (minutes)") },
                    modifier = Modifier.width(220.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true
                )

                if (state.thresholdMinutes != null) {
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = state.thresholdType == ThresholdType.GOAL,
                            onClick = {
                                viewModel.setThresholdType(ThresholdType.GOAL)
                            },
                            shape = SegmentedButtonDefaults.itemShape(0, 2)
                        ) { Text("Goal") }
                        SegmentedButton(
                            selected = state.thresholdType == ThresholdType.TIME_TO_STOP,
                            onClick = {
                                viewModel.setThresholdType(ThresholdType.TIME_TO_STOP)
                            },
                            shape = SegmentedButtonDefaults.itemShape(1, 2)
                        ) { Text("Time to stop") }
                    }
                }
            }

            Text("Daily texts", style = MaterialTheme.typography.labelLarge)
            DailyTextsEditor(
                dailyTexts = state.dailyTexts,
                onSetText = viewModel::setDailyText
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimesOfDayChips(
    times: List<Int>,
    onAdd: (Int) -> Unit,
    onRemove: (Int) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newHour by remember { mutableStateOf("") }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false; newHour = "" },
            title = { Text("Add time") },
            text = {
                OutlinedTextField(
                    value = newHour,
                    onValueChange = { newHour = it },
                    label = { Text("Hour (0-23)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    newHour.toIntOrNull()?.let { h ->
                        if (h in 0..23) onAdd(h)
                    }
                    showAddDialog = false
                    newHour = ""
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false; newHour = ""
                }) { Text("Cancel") }
            }
        )
    }

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        times.forEach { hour ->
            InputChip(
                selected = false,
                onClick = { onRemove(hour) },
                label = { Text("%d:00".format(hour)) },
                trailingIcon = {
                    Icon(Icons.Filled.Close, "remove")
                }
            )
        }
        AssistChip(
            onClick = { showAddDialog = true },
            label = { Text("+") },
            leadingIcon = { Icon(Icons.Filled.Add, "add time") }
        )
    }
}

@Composable
private fun DaysActiveRow(
    daysActive: Set<DayOfWeek>,
    onToggle: (DayOfWeek) -> Unit
) {
    val days = listOf(
        DayOfWeek.SUNDAY to "S",
        DayOfWeek.MONDAY to "M",
        DayOfWeek.TUESDAY to "T",
        DayOfWeek.WEDNESDAY to "W",
        DayOfWeek.THURSDAY to "T",
        DayOfWeek.FRIDAY to "F",
        DayOfWeek.SATURDAY to "S"
    )
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        days.forEach { (day, label) ->
            FilterChip(
                selected = day in daysActive,
                onClick = { onToggle(day) },
                label = { Text(label) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrioritySelector(
    priority: Priority,
    onSelect: (Priority) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val labels = mapOf(
        Priority.HIGH to "High",
        Priority.MEDIUM_HIGH to "Med-High",
        Priority.MEDIUM to "Medium",
        Priority.MEDIUM_LOW to "Med-Low",
        Priority.LOW to "Low"
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = labels[priority] ?: "",
            onValueChange = {},
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            },
            modifier = Modifier.menuAnchor().width(160.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Priority.entries.forEach { p ->
                DropdownMenuItem(
                    text = { Text(labels[p] ?: p.name) },
                    onClick = { onSelect(p); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun DailyTextsEditor(
    dailyTexts: Map<DayOfWeek, String>,
    onSetText: (DayOfWeek, String) -> Unit
) {
    val days = listOf(
        DayOfWeek.SUNDAY to "Sun",
        DayOfWeek.MONDAY to "Mon",
        DayOfWeek.TUESDAY to "Tue",
        DayOfWeek.WEDNESDAY to "Wed",
        DayOfWeek.THURSDAY to "Thu",
        DayOfWeek.FRIDAY to "Fri",
        DayOfWeek.SATURDAY to "Sat"
    )
    days.forEach { (day, label) ->
        OutlinedTextField(
            value = dailyTexts[day] ?: "",
            onValueChange = { onSetText(day, it) },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}
