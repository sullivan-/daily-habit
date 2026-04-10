package com.habit.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
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
import com.habit.viewmodel.TrackEditorItem
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

            FieldGroup("Timekeeping") {
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
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.height(56.dp)
                    ) {
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

            if (!state.isNew) {
                FieldGroup("Tracks") {
                    TracksEditor(
                        tracks = state.tracks,
                        onAdd = viewModel::addTrack,
                        onToggleExpanded = viewModel::toggleTrackExpanded,
                        onUpdateName = viewModel::updateTrackName,
                        onUpdatePriority = viewModel::updateTrackPriority,
                        onUpdateDayOfWeek = viewModel::updateTrackDayOfWeek,
                        onArchive = viewModel::archiveTrack,
                        onUnarchive = viewModel::unarchiveTrack,
                        onDelete = viewModel::deleteTrack,
                        onAddMilestone = viewModel::addMilestone,
                        onDeleteMilestone = viewModel::deleteMilestone
                    )
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TracksEditor(
    tracks: List<TrackEditorItem>,
    onAdd: () -> Unit,
    onToggleExpanded: (Int) -> Unit,
    onUpdateName: (Int, String) -> Unit,
    onUpdatePriority: (Int, Priority) -> Unit,
    onUpdateDayOfWeek: (Int, DayOfWeek?) -> Unit,
    onArchive: (Int) -> Unit,
    onUnarchive: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onAddMilestone: (Int, String) -> Unit,
    onDeleteMilestone: (Int, Int) -> Unit
) {
    val active = tracks.withIndex().filter { !it.value.archived }
    val archived = tracks.withIndex().filter { it.value.archived }

    active.forEach { (index, track) ->
        if (track.expanded) {
            TrackInlineEditor(
                track = track,
                onUpdateName = { onUpdateName(index, it) },
                onUpdatePriority = { onUpdatePriority(index, it) },
                onUpdateDayOfWeek = { onUpdateDayOfWeek(index, it) },
                onArchive = { onArchive(index) },
                onDelete = { onDelete(index) },
                onDone = { onToggleExpanded(index) },
                onAddMilestone = { onAddMilestone(index, it) },
                onDeleteMilestone = { msIdx -> onDeleteMilestone(index, msIdx) }
            )
        } else {
            TrackRow(
                track = track,
                onClick = { onToggleExpanded(index) }
            )
        }
    }

    Button(
        onClick = onAdd,
        elevation = buttonElevation(),
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Text("+ Add track")
    }

    if (archived.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Archived",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        archived.forEach { (index, track) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = track.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { onUnarchive(index) }) {
                    Text("Restore")
                }
            }
        }
    }
}

@Composable
private fun TrackRow(
    track: TrackEditorItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = track.name.ifEmpty { "(new track)" },
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        track.dayOfWeek?.let { day ->
            Text(
                text = day.name.take(3).lowercase()
                    .replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackInlineEditor(
    track: TrackEditorItem,
    onUpdateName: (String) -> Unit,
    onUpdatePriority: (Priority) -> Unit,
    onUpdateDayOfWeek: (DayOfWeek?) -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onDone: () -> Unit,
    onAddMilestone: (String) -> Unit,
    onDeleteMilestone: (Int) -> Unit
) {
    var newMilestoneName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, ControlShape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = track.name,
            onValueChange = onUpdateName,
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = ControlShape,
            singleLine = true
        )

        DayOfWeekSelector(
            selected = track.dayOfWeek,
            onSelect = onUpdateDayOfWeek
        )

        if (track.milestones.isNotEmpty()) {
            Text("Series", style = MaterialTheme.typography.labelMedium)
            track.milestones.forEachIndexed { msIdx, milestone ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${msIdx + 1}.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(24.dp)
                    )
                    Checkbox(
                        checked = milestone.completed,
                        onCheckedChange = null,
                        enabled = false
                    )
                    Text(
                        text = milestone.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    if (milestone.id == 0L || track.canDelete) {
                        IconButton(onClick = { onDeleteMilestone(msIdx) }) {
                            Icon(Icons.Filled.Close, "delete milestone",
                                modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newMilestoneName,
                onValueChange = { newMilestoneName = it },
                label = { Text("New milestone") },
                modifier = Modifier.weight(1f),
                shape = ControlShape,
                singleLine = true
            )
            IconButton(
                onClick = {
                    if (newMilestoneName.isNotBlank()) {
                        onAddMilestone(newMilestoneName.trim())
                        newMilestoneName = ""
                    }
                }
            ) {
                Icon(Icons.Filled.Add, "add milestone")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
        ) {
            if (track.canDelete) {
                TextButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            } else {
                TextButton(onClick = onArchive) {
                    Text("Archive")
                }
            }
            Button(onClick = onDone, elevation = buttonElevation()) {
                Text("Close")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayOfWeekSelector(
    selected: DayOfWeek?,
    onSelect: (DayOfWeek?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val labels = listOf(null to "None") + DayOfWeek.entries.map {
        it to it.name.take(3).lowercase().replaceFirstChar { c -> c.uppercase() }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = labels.first { it.first == selected }.second,
            onValueChange = {},
            readOnly = true,
            label = { Text("Day") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            shape = ControlShape,
            modifier = Modifier.menuAnchor().width(140.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            labels.forEach { (day, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = { onSelect(day); expanded = false }
                )
            }
        }
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
