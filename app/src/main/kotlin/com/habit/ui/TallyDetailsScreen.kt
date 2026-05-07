package com.habit.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.habit.data.Priority
import com.habit.viewmodel.TallyDetailsViewModel
import com.habit.viewmodel.formatStreakDuration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val priorityLabels = mapOf(
    Priority.HIGH to "High",
    Priority.MEDIUM_HIGH to "Med-High",
    Priority.MEDIUM to "Medium",
    Priority.MEDIUM_LOW to "Med-Low",
    Priority.LOW to "Low"
)

private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
private val timeFormatter = DateTimeFormatter.ofPattern("H:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TallyDetailsScreen(
    viewModel: TallyDetailsViewModel,
    tallyId: String?,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(tallyId) {
        if (tallyId != null) viewModel.loadTally(tallyId)
    }

    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    LaunchedEffect(state.deleted) {
        if (state.deleted) onBack()
    }

    var showDiscardDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete ${state.name}?") },
            text = {
                Text(
                    "this will delete the tally and all its choice history." +
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

    if (showDatePicker) {
        RecordFirstNoDatePicker(
            earliestChoiceDate = state.earliestChoiceDate,
            onConfirm = { date ->
                showDatePicker = false
                viewModel.recordFirstNo(date)
            },
            onDismiss = { showDatePicker = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isNew) "New Tally"
                        else state.name.ifBlank { "Details" }
                    )
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

            Text("Priority", style = MaterialTheme.typography.labelLarge)
            DetailsPrioritySelector(
                priority = state.priority,
                onSelect = viewModel::setPriority
            )

            if (state.hasChoices) {
                SectionDivider("Streak")
                StreakSection(
                    streakStart = state.streakStart,
                    streakCount = state.streakCount
                )

                SectionDivider("Choices")
                ChoicesStatsSection(
                    abstainCountLast10 = state.abstainCountLast10,
                    totalCountLast10 = state.totalCountLast10,
                    abstainCountToday = state.abstainCountToday,
                    totalCountToday = state.totalCountToday,
                    showToday = state.showTodayStats
                )
            }

            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                elevation = buttonElevation()
            ) {
                Text("Record First No")
            }

            if (!state.isNew) {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    elevation = buttonElevation(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(
                        1.dp, MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete Tally")
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionDivider(title: String) {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun StreakSection(streakStart: Instant?, streakCount: Int) {
    if (streakStart == null) {
        Text(
            text = "no current streak",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val now = Instant.now()
    Text(
        text = formatStreakDuration(streakStart, now, streakCount),
        style = MaterialTheme.typography.bodyLarge,
        color = Color.Green
    )

    val startZoned = streakStart.atZone(ZoneId.systemDefault())
    val startDate = startZoned.toLocalDate()
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    val sinceText = when (startDate) {
        today -> "since ${timeFormatter.format(startZoned)} today"
        yesterday -> "since ${timeFormatter.format(startZoned)} yesterday"
        else -> "since ${dateFormatter.format(startDate)}"
    }

    Text(
        text = sinceText,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ChoicesStatsSection(
    abstainCountLast10: Int,
    totalCountLast10: Int,
    abstainCountToday: Int,
    totalCountToday: Int,
    showToday: Boolean
) {
    val last10Ratio = if (totalCountLast10 > 0) {
        abstainCountLast10.toFloat() / totalCountLast10
    } else 1f

    Text(
        text = "$abstainCountLast10/$totalCountLast10 last 10",
        style = MaterialTheme.typography.bodyMedium,
        color = indicatorColor(last10Ratio)
    )

    if (showToday) {
        val todayRatio = if (totalCountToday > 0) {
            abstainCountToday.toFloat() / totalCountToday
        } else 1f

        Text(
            text = "$abstainCountToday/$totalCountToday today",
            style = MaterialTheme.typography.bodyMedium,
            color = indicatorColor(todayRatio)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordFirstNoDatePicker(
    earliestChoiceDate: LocalDate?,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val maxDateMillis = earliestChoiceDate?.let {
        it.minusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    } ?: Instant.now().toEpochMilli()

    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= maxDateMillis
            }
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                        onConfirm(date)
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsPrioritySelector(
    priority: Priority,
    onSelect: (Priority) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = priorityLabels[priority] ?: "",
            onValueChange = {},
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            },
            shape = ControlShape,
            modifier = Modifier.menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Priority.entries.forEach { p ->
                DropdownMenuItem(
                    text = { Text(priorityLabels[p] ?: p.name) },
                    onClick = { onSelect(p); expanded = false }
                )
            }
        }
    }
}
