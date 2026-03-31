package com.habit.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.habit.viewmodel.AgendaUiState

@Composable
fun ActivityDetail(
    state: AgendaUiState,
    modifier: Modifier = Modifier
) {
    val habit = state.selectedHabit ?: return
    val previousActivities = state.todayActivities.filter {
        it.habitId == habit.id && it.completedAt != null
    }

    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = "Previous activities today",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (previousActivities.isEmpty()) {
            Text(
                text = "None yet",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            previousActivities.forEach { activity ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    if (habit.timed && activity.elapsedMs > 0) {
                        Text(
                            text = "${activity.elapsedMs / 60000}m",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    if (activity.note.isNotEmpty()) {
                        Text(
                            text = activity.note,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
