package com.habit.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
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
import com.habit.data.Habit
import com.habit.viewmodel.AgendaUiState

@Composable
fun ActivityView(
    state: AgendaUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onComplete: (String) -> Unit,
    onCompleteUntimed: (String, String) -> Unit,
    onNoteChange: (String) -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val habit = state.selectedHabit

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp
    ) {
        if (habit == null) {
            CollapsedSummary(state = state)
        } else if (state.selectedActivityId != null) {
            CompletedActivityDetail(
                state = state,
                onNoteChange = onNoteChange
            )
        } else if (habit.timed) {
            TimedHabitView(
                habit = habit,
                state = state,
                onStart = onStart,
                onStop = onStop,
                onComplete = onComplete,
                onNoteChange = onNoteChange,
                onExpand = onExpand
            )
        } else {
            UntimedHabitView(
                habit = habit,
                state = state,
                onComplete = onCompleteUntimed
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
private fun TimedHabitView(
    habit: Habit,
    state: AgendaUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onComplete: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onExpand: () -> Unit
) {
    var note by remember(state.activeActivity?.id) {
        mutableStateOf(state.activeActivity?.note ?: "")
    }

    Column(modifier = Modifier.padding(16.dp)) {
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
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        TimerDisplay(
            elapsedMs = state.elapsedMs,
            isRunning = state.timerRunning,
            onStart = onStart,
            onStop = onStop,
            onDone = { onComplete(note) }
        )

        NoteField(
            value = note,
            onValueChange = { newNote ->
                note = newNote
                onNoteChange(newNote)
            },
            modifier = Modifier.padding(top = 8.dp)
        )

        TextButton(
            onClick = onExpand,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("More detail")
        }
    }
}

@Composable
private fun UntimedHabitView(
    habit: Habit,
    state: AgendaUiState,
    onComplete: (String, String) -> Unit
) {
    val dailyText = habit.dailyTexts[
        java.time.LocalDate.now().dayOfWeek
    ] ?: ""
    var note by remember(habit.id) { mutableStateOf(dailyText) }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = habit.name,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            Checkbox(
                checked = false,
                onCheckedChange = { onComplete(habit.id, note) }
            )
        }

        if (habit.dailyTarget > 1) {
            val done = state.todayActivities.count {
                it.habitId == habit.id && it.completedAt != null
            }
            Text(
                text = "${done + 1}/${habit.dailyTarget}",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        NoteField(
            value = note,
            onValueChange = { note = it },
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun CompletedActivityDetail(
    state: AgendaUiState,
    onNoteChange: (String) -> Unit
) {
    val activity = state.todayActivities.find { it.id == state.selectedActivityId }
        ?: return
    val habit = state.habits.find { it.id == activity.habitId } ?: return

    var note by remember(activity.id) { mutableStateOf(activity.note) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = habit.name,
            style = MaterialTheme.typography.headlineSmall
        )
        activity.completedAt?.let {
            val time = java.time.LocalTime.ofInstant(
                it, java.time.ZoneId.systemDefault()
            )
            Text(
                text = "Completed at ${time.hour}:%02d".format(time.minute),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (habit.timed && activity.elapsedMs > 0) {
            val minutes = activity.elapsedMs / 60000
            Text(
                text = "${minutes}m",
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
