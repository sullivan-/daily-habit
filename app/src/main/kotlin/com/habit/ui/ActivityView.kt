package com.habit.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.habit.data.Activity
import com.habit.data.Habit
import com.habit.viewmodel.AgendaUiState
import com.habit.viewmodel.Layout
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun ActivityView(
    state: AgendaUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onComplete: (String) -> Unit,
    onCompleteUntimed: (String, String) -> Unit,
    onNoteChange: (String) -> Unit,
    onToggleDetail: () -> Unit,
    onHistoryOlder: () -> Unit,
    onHistoryNewer: () -> Unit,
    onHistoryBackToCurrent: () -> Unit,
    onEditHabit: (String) -> Unit,
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
        } else {
            HabitView(
                habit = habit,
                state = state,
                onStart = onStart,
                onStop = onStop,
                onComplete = onComplete,
                onCompleteUntimed = onCompleteUntimed,
                onNoteChange = onNoteChange,
                onToggleDetail = onToggleDetail,
                onHistoryOlder = onHistoryOlder,
                onHistoryNewer = onHistoryNewer,
                onHistoryBackToCurrent = onHistoryBackToCurrent,
                onEditHabit = onEditHabit,
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
    onStop: () -> Unit,
    onComplete: (String) -> Unit,
    onCompleteUntimed: (String, String) -> Unit,
    onNoteChange: (String) -> Unit,
    onToggleDetail: () -> Unit,
    onHistoryOlder: () -> Unit,
    onHistoryNewer: () -> Unit,
    onHistoryBackToCurrent: () -> Unit,
    onEditHabit: (String) -> Unit,
    isExpanded: Boolean
) {
    if (isExpanded && state.browsingHistory && !state.isAtNewest) {
        HistoryActivityView(
            activity = state.historyActivity!!,
            habit = habit,
            state = state,
            onNoteChange = onNoteChange,
            onToggleDetail = onToggleDetail,
            onHistoryOlder = onHistoryOlder,
            onHistoryNewer = onHistoryNewer,
            onHistoryBackToCurrent = onHistoryBackToCurrent,
            onEditHabit = onEditHabit
        )
    } else {
        CurrentActivityView(
            habit = habit,
            state = state,
            onStart = onStart,
            onStop = onStop,
            onComplete = onComplete,
            onCompleteUntimed = onCompleteUntimed,
            onNoteChange = onNoteChange,
            onToggleDetail = onToggleDetail,
            onHistoryOlder = onHistoryOlder,
            onEditHabit = onEditHabit,
            isExpanded = isExpanded
        )
    }
}

@Composable
private fun CurrentActivityView(
    habit: Habit,
    state: AgendaUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onComplete: (String) -> Unit,
    onCompleteUntimed: (String, String) -> Unit,
    onNoteChange: (String) -> Unit,
    onToggleDetail: () -> Unit,
    onHistoryOlder: () -> Unit,
    onEditHabit: (String) -> Unit,
    isExpanded: Boolean
) {
    var note by remember(state.activeActivity?.id) {
        mutableStateOf(state.activeActivity?.note ?: "")
    }

    val swipeModifier = if (isExpanded) {
        Modifier.swipeHistoryGesture(
            onSwipeLeft = onHistoryOlder,
            onSwipeRight = {},
            isAtLeft = false,
            isAtRight = true
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
            if (isExpanded) {
                IconButton(
                    onClick = { onEditHabit(habit.id) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Filled.Edit, "edit habit")
                }
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
                    if (habit.timed) {
                        onComplete(note)
                    } else {
                        onCompleteUntimed(habit.id, note)
                    }
                }
            )
        }

        if (habit.timed) {
            TimerDisplay(
                elapsedMs = state.elapsedMs,
                isRunning = state.timerRunning,
                onStart = onStart,
                onStop = onStop
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

@Composable
private fun HistoryActivityView(
    activity: Activity,
    habit: Habit,
    state: AgendaUiState,
    onNoteChange: (String) -> Unit,
    onToggleDetail: () -> Unit,
    onHistoryOlder: () -> Unit,
    onHistoryNewer: () -> Unit,
    onHistoryBackToCurrent: () -> Unit,
    onEditHabit: (String) -> Unit
) {
    var note by remember(activity.id) { mutableStateOf(activity.note) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }

    Column(
        modifier = Modifier
            .swipeHistoryGesture(
                onSwipeLeft = onHistoryOlder,
                onSwipeRight = onHistoryNewer,
                isAtLeft = state.isAtOldest,
                isAtRight = state.isAtNewest
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
            IconButton(
                onClick = { onEditHabit(habit.id) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Filled.Edit, "edit habit")
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

        activity.completedAt?.let {
            val time = java.time.LocalTime.ofInstant(it, ZoneId.systemDefault())
            Text(
                text = "completed at %d:%02d".format(time.hour, time.minute),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (habit.timed && activity.elapsedMs > 0) {
            val totalSeconds = activity.elapsedMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            Text(
                text = "%dm %ds".format(minutes, seconds),
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

        TextButton(
            onClick = onHistoryBackToCurrent,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Back to today")
        }
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
                text = "completed at ${time.hour}:%02d".format(time.minute),
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

@Composable
private fun Modifier.swipeHistoryGesture(
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    isAtLeft: Boolean,
    isAtRight: Boolean
): Modifier {
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    return this
        .offset { IntOffset(offsetX.value.roundToInt(), 0) }
        .pointerInput(isAtLeft, isAtRight) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    val threshold = 100f
                    when {
                        offsetX.value < -threshold && !isAtLeft -> {
                            onSwipeLeft()
                        }
                        offsetX.value > threshold && !isAtRight -> {
                            onSwipeRight()
                        }
                    }
                    scope.launch {
                        offsetX.animateTo(0f, spring())
                    }
                },
                onHorizontalDrag = { _, dragAmount ->
                    scope.launch {
                        val dampened = if (
                            (dragAmount < 0 && isAtLeft) ||
                            (dragAmount > 0 && isAtRight)
                        ) {
                            dragAmount * 0.3f
                        } else {
                            dragAmount
                        }
                        offsetX.snapTo(offsetX.value + dampened)
                    }
                }
            )
        }
}
