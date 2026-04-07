package com.habit.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.habit.data.Priority
import com.habit.data.TargetMode
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

    LaunchedEffect(state.deleted) {
        if (state.deleted) onBack()
    }

    var showDiscardDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete ${state.name}?") },
            text = {
                Text(
                    "this will delete the habit and all its activity history." +
                    " this cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.delete() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

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
                        enabled = state.isValid,
                        elevation = buttonElevation()
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
                shape = ControlShape,
                singleLine = true
            )

            FieldGroup("Schedule") {
                TimesOfDayChips(
                    times = state.timesOfDay,
                    onAdd = viewModel::addTimeOfDay,
                    onRemove = viewModel::removeTimeOfDay
                )
                Spacer(Modifier.height(8.dp))
                DaysActiveRow(
                    daysActive = state.daysActive,
                    onToggle = viewModel::toggleDayActive
                )
            }

            FieldGroup("Priority") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PrioritySelector(
                        priority = state.priority,
                        onSelect = viewModel::setPriority,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = state.sortOrder.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let(viewModel::setSortOrder)
                        },
                        label = { Text("Tie breaker") },
                        modifier = Modifier.weight(1f),
                        shape = ControlShape,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        singleLine = true
                    )
                }
            }

            FieldGroup("Tracking") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = state.dailyTarget.toString(),
                        onValueChange = {
                            it.toIntOrNull()?.let(viewModel::setDailyTarget)
                        },
                        label = { Text("Daily target") },
                        modifier = Modifier.width(120.dp),
                        shape = ControlShape,
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
                            shape = segmentedShape(0, 2)
                        ) { Text("At least") }
                        SegmentedButton(
                            selected = state.dailyTargetMode == TargetMode.EXACTLY,
                            onClick = {
                                viewModel.setDailyTargetMode(TargetMode.EXACTLY)
                            },
                            shape = segmentedShape(1, 2)
                        ) { Text("Exactly") }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = state.timed,
                        onCheckedChange = { viewModel.setTimed(it) }
                    )
                    Text("Timed habit")
                }
                if (state.timed) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = state.goalMinutes?.toString() ?: "",
                            onValueChange = {
                                viewModel.setGoalMinutes(it.toIntOrNull())
                            },
                            label = { Text("Goal") },
                            modifier = Modifier.weight(1f),
                            shape = ControlShape,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            suffix = { Text("min") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = state.stopMinutes?.toString() ?: "",
                            onValueChange = {
                                viewModel.setStopMinutes(it.toIntOrNull())
                            },
                            label = { Text("Stop time") },
                            modifier = Modifier.weight(1f),
                            shape = ControlShape,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            suffix = { Text("min") },
                            singleLine = true
                        )
                    }
                }
            }

            FieldGroup("Daily texts") {
                DailyTextsEditor(
                    dailyTexts = state.dailyTexts,
                    onSetText = viewModel::setDailyText
                )
            }

            if (!state.isNew) {
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    elevation = buttonElevation(),
                    colors = androidx.compose.material3.ButtonDefaults
                        .outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete Habit")
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FieldGroup(
    title: String,
    content: @Composable () -> Unit
) {
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
    ) {
        Column(
            modifier = Modifier.padding(
                start = 12.dp, end = 12.dp, top = 20.dp, bottom = 12.dp
            )
        ) {
            content()
        }
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
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

    if (showAddDialog) {
        HourPickerDialog(
            initialHour = ((times.maxOrNull() ?: 7) + 1).coerceIn(0, 23),
            existingHours = times.toSet(),
            onConfirm = { hour ->
                onAdd(hour)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
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
    onSelect: (Priority) -> Unit,
    modifier: Modifier = Modifier
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
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = labels[priority] ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Priority") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            },
            shape = ControlShape,
            modifier = Modifier.menuAnchor().fillMaxWidth()
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
            shape = ControlShape,
            singleLine = true
        )
    }
}

@Composable
private fun HourPickerDialog(
    initialHour: Int,
    existingHours: Set<Int>,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val hours = (0..23).toList()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        val scrollTo = (initialHour - 2).coerceIn(0, 21)
        listState.scrollToItem(scrollTo)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add time") },
        text = {
            LazyColumn(
                state = listState,
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                items(hours) { hour ->
                    val alreadyUsed = hour in existingHours
                    val textColor = if (alreadyUsed)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.onSurface
                    Text(
                        text = "%d:00".format(hour),
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (alreadyUsed) Modifier
                                else Modifier.clickable { onConfirm(hour) }
                            )
                            .padding(vertical = 10.dp, horizontal = 16.dp)
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}
