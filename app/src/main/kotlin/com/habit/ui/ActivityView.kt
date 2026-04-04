package com.habit.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.habit.data.Activity
import com.habit.data.Habit
import com.habit.viewmodel.AgendaUiState
import com.habit.viewmodel.Layout
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ActivityView(
    state: AgendaUiState,
    onStart: () -> Unit,
    onFinish: (String) -> Unit,
    onCancel: () -> Unit,
    onCompleteUntimed: (String, String) -> Unit,
    onNoteChange: (String) -> Unit,
    onToggleDetail: () -> Unit,
    onHistoryOlder: () -> Unit,
    onHistoryNewer: () -> Unit,
    onHistoryBackToAnchor: () -> Unit,
    onEditHabit: (String) -> Unit,
    onUpdateStartTime: (Long, java.time.Instant?) -> Unit,
    onUpdateCompletedAt: (Long, java.time.Instant?) -> Unit,
    onDoAgain: (String) -> Unit,
    onSkip: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val habit = state.selectedHabit

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp
    ) {
        if (habit == null) {
            CollapsedSummary(state = state)
        } else if (state.selectedActivityId != null &&
            state.layout != Layout.ACTIVITY_FOCUSED
        ) {
            CompletedActivityDetail(
                state = state,
                onNoteChange = onNoteChange,
                onToggleDetail = onToggleDetail,
                onHistoryOlder = onHistoryOlder,
                onHistoryNewer = onHistoryNewer
            )
        } else {
            HabitView(
                habit = habit,
                state = state,
                onStart = onStart,
                onFinish = onFinish,
                onCancel = onCancel,
                onCompleteUntimed = onCompleteUntimed,
                onNoteChange = onNoteChange,
                onToggleDetail = onToggleDetail,
                onHistoryOlder = onHistoryOlder,
                onHistoryNewer = onHistoryNewer,
                onHistoryBackToAnchor = onHistoryBackToAnchor,
                onEditHabit = onEditHabit,
                onUpdateStartTime = onUpdateStartTime,
                onUpdateCompletedAt = onUpdateCompletedAt,
                onDoAgain = onDoAgain,
                onSkip = onSkip,
                isExpanded = state.layout == Layout.ACTIVITY_FOCUSED
            )
        }
    }
}

@Composable
private fun CollapsedSummary(state: AgendaUiState) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "${state.progressCount}/${state.totalTarget} activities complete",
            style = MaterialTheme.typography.bodyLarge
        )
        val remaining = state.totalTarget - state.progressCount
        if (remaining > 0) {
            Text(
                text = "$remaining remaining",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun HabitView(
    habit: Habit,
    state: AgendaUiState,
    onStart: () -> Unit,
    onFinish: (String) -> Unit,
    onCancel: () -> Unit,
    onCompleteUntimed: (String, String) -> Unit,
    onNoteChange: (String) -> Unit,
    onToggleDetail: () -> Unit,
    onHistoryOlder: () -> Unit,
    onHistoryNewer: () -> Unit,
    onHistoryBackToAnchor: () -> Unit,
    onEditHabit: (String) -> Unit,
    onUpdateStartTime: (Long, java.time.Instant?) -> Unit,
    onUpdateCompletedAt: (Long, java.time.Instant?) -> Unit,
    onDoAgain: (String) -> Unit,
    onSkip: () -> Unit,
    isExpanded: Boolean
) {
    val showHistory = isExpanded && state.browsingHistory &&
        (!state.isAtNewest || state.historyActivity?.completedAt != null)
    if (showHistory) {
        HistoryActivityView(
            activity = state.historyActivity!!,
            habit = habit,
            state = state,
            onNoteChange = onNoteChange,
            onToggleDetail = onToggleDetail,
            onHistoryOlder = onHistoryOlder,
            onHistoryNewer = onHistoryNewer,
            onHistoryBackToAnchor = onHistoryBackToAnchor,
            onEditHabit = onEditHabit,
            onUpdateStartTime = onUpdateStartTime,
            onUpdateCompletedAt = onUpdateCompletedAt,
            onDoAgain = onDoAgain
        )
    } else {
        CurrentActivityView(
            habit = habit,
            state = state,
            onStart = onStart,
            onFinish = onFinish,
            onCancel = onCancel,
            onCompleteUntimed = onCompleteUntimed,
            onNoteChange = onNoteChange,
            onToggleDetail = onToggleDetail,
            onHistoryOlder = onHistoryOlder,
            onHistoryNewer = onHistoryNewer,
            onEditHabit = onEditHabit,
            onDoAgain = onDoAgain,
            onSkip = onSkip,
            isExpanded = isExpanded
        )
    }
}

@Composable
private fun CurrentActivityView(
    habit: Habit,
    state: AgendaUiState,
    onStart: () -> Unit,
    onFinish: (String) -> Unit,
    onCancel: () -> Unit,
    onCompleteUntimed: (String, String) -> Unit,
    onNoteChange: (String) -> Unit,
    onToggleDetail: () -> Unit,
    onHistoryOlder: () -> Unit,
    onHistoryNewer: () -> Unit,
    onEditHabit: (String) -> Unit,
    onDoAgain: (String) -> Unit,
    onSkip: () -> Unit,
    isExpanded: Boolean
) {
    var note by remember(state.activeActivity?.id) {
        mutableStateOf(state.activeActivity?.note ?: "")
    }

    val swipeModifier = if (state.browsingHistory) {
        Modifier.swipeHistoryGesture(
            onSwipeLeft = onHistoryNewer,
            onSwipeRight = onHistoryOlder,
            isAtLeft = state.isAtNewest,
            isAtRight = state.isAtOldest
        )
    } else Modifier

    Column(modifier = swipeModifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = habit.name,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            if (habit.dailyTarget > 1) {
                val done = state.todayActivities.count {
                    it.habitId == habit.id && it.completedAt != null
                }
                Text(
                    text = "${done + 1}/${habit.dailyTarget}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            IconButton(onClick = onToggleDetail, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.UnfoldLess
                        else Icons.Filled.UnfoldMore,
                    contentDescription = if (isExpanded) "collapse" else "expand"
                )
            }
            Checkbox(
                checked = false,
                onCheckedChange = {
                    if (habit.timed) onFinish(note) else onCompleteUntimed(habit.id, note)
                }
            )
        }

        if (habit.timed) {
            val elapsed = if (state.timerRunning) {
                state.activeActivity?.elapsedMs ?: 0
            } else {
                0L
            }
            TimerDisplay(
                elapsedMs = elapsed,
                isRunning = state.timerRunning,
                onStart = onStart,
                onFinish = { onFinish(note) },
                onCancel = onCancel
            )
        }

        NoteField(
            value = note,
            onValueChange = { newNote ->
                note = newNote
                onNoteChange(newNote)
            },
            modifier = Modifier.padding(top = 8.dp)
        )

        if (isExpanded) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                if (habit.dailyTargetMode == com.habit.data.TargetMode.AT_LEAST &&
                    state.activeActivity?.completedAt != null
                ) {
                    TextButton(onClick = { onDoAgain(habit.id) }) {
                        Text("Again")
                    }
                }
                if (state.activeActivity?.completedAt == null) {
                    TextButton(onClick = onSkip) {
                        Text("Skip")
                    }
                }
                TextButton(onClick = { onEditHabit(habit.id) }) {
                    Text("Edit habit")
                }
            }
        }
    }
}

@Composable
private fun HistoryActivityView(
    activity: Activity,
    habit: Habit,
    state: AgendaUiState,
    onNoteChange: (String) -> Unit,
    onToggleDetail: () -> Unit,
    onHistoryOlder: () -> Unit,
    onHistoryNewer: () -> Unit,
    onHistoryBackToAnchor: () -> Unit,
    onEditHabit: (String) -> Unit,
    onUpdateStartTime: (Long, java.time.Instant?) -> Unit,
    onUpdateCompletedAt: (Long, java.time.Instant?) -> Unit,
    onDoAgain: (String) -> Unit
) {
    var note by remember(activity.id) { mutableStateOf(activity.note) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val zone = ZoneId.systemDefault()

    val sameDay = state.historyActivities.filter {
        it.habitId == habit.id && it.attributedDate == activity.attributedDate
    }
    val activityNumber = sameDay.indexOfFirst { it.id == activity.id } + 1

    Column(
        modifier = Modifier
            .swipeHistoryGesture(
                onSwipeLeft = onHistoryNewer,
                onSwipeRight = onHistoryOlder,
                isAtLeft = state.isAtNewest,
                isAtRight = state.isAtOldest
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = habit.name,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            if (habit.dailyTarget > 1 && activityNumber > 0) {
                Text(
                    text = "$activityNumber/${habit.dailyTarget}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            IconButton(onClick = onToggleDetail, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Filled.UnfoldLess,
                    contentDescription = "collapse"
                )
            }
        }

        Text(
            text = activity.attributedDate.format(dateFormatter),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 4.dp)
        )

        if (habit.timed) {
            activity.startTime?.let {
                Text(
                    text = "started ${it.atZone(zone).format(timeFormatter)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        activity.completedAt?.let {
            Text(
                text = "completed ${it.atZone(zone).format(timeFormatter)}",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (habit.timed && activity.elapsedMs > 0) {
            Text(
                text = "duration: ${formatElapsed(activity.elapsedMs)}",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        NoteField(
            value = note,
            onValueChange = { newNote ->
                note = newNote
                onNoteChange(newNote)
            },
            modifier = Modifier.padding(top = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            if (habit.dailyTargetMode == com.habit.data.TargetMode.AT_LEAST &&
                activity.completedAt != null
            ) {
                TextButton(onClick = { onDoAgain(habit.id) }) {
                    Text("Again")
                }
            }
            if (state.hasSwipedFromAnchor) {
                TextButton(onClick = onHistoryBackToAnchor) {
                    Text("Back to start")
                }
            }
            TextButton(onClick = { onEditHabit(habit.id) }) {
                Text("Edit habit")
            }
        }
    }
}

@Composable
private fun CompletedActivityDetail(
    state: AgendaUiState,
    onNoteChange: (String) -> Unit,
    onToggleDetail: () -> Unit,
    onHistoryOlder: () -> Unit,
    onHistoryNewer: () -> Unit
) {
    val activity = state.todayActivities.find { it.id == state.selectedActivityId }
        ?: return
    val habit = state.habits.find { it.id == activity.habitId } ?: return

    var note by remember(activity.id) { mutableStateOf(activity.note) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val zone = ZoneId.systemDefault()

    val swipeModifier = if (state.browsingHistory) {
        Modifier.swipeHistoryGesture(
            onSwipeLeft = onHistoryNewer,
            onSwipeRight = onHistoryOlder,
            isAtLeft = state.isAtNewest,
            isAtRight = state.isAtOldest
        )
    } else Modifier

    Column(modifier = swipeModifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = habit.name,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onToggleDetail, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.UnfoldMore, "expand")
            }
        }
        activity.completedAt?.let {
            Text(
                text = "completed ${it.atZone(zone).format(timeFormatter)}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (habit.timed && activity.elapsedMs > 0) {
            Text(
                text = "duration: ${formatElapsed(activity.elapsedMs)}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        NoteField(
            value = note,
            onValueChange = { newNote ->
                note = newNote
                onNoteChange(newNote)
            },
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

