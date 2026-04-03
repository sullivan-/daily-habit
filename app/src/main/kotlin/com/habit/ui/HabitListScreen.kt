package com.habit.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.habit.data.Habit
import com.habit.data.Priority
import com.habit.viewmodel.AgendaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitListScreen(
    viewModel: AgendaViewModel,
    onEditHabit: (String) -> Unit,
    onNewHabit: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val habits = uiState.habits.sortedWith(
        compareBy<Habit> { it.timesOfDay.firstOrNull() ?: 0 }
            .thenBy { it.priority.ordinal }
            .thenBy { it.sortOrder }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Habits") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewHabit) {
                Icon(Icons.Filled.Add, "new habit")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(habits, key = { it.id }) { habit ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditHabit(habit.id) }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = habit.name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = buildSummary(habit),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = priorityLabel(habit.priority),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            }
        }
    }
}

private fun buildSummary(habit: Habit): String {
    val times = habit.timesOfDay.joinToString(", ") { "%d:00".format(it) }
    val days = if (habit.daysActive.size == 7) "every day"
        else habit.daysActive.joinToString(", ") {
            it.name.take(3).lowercase().replaceFirstChar { c -> c.uppercase() }
        }
    val timed = if (habit.timed) "timed" else "untimed"
    return "$times · $days · $timed"
}

private fun priorityLabel(priority: Priority): String = when (priority) {
    Priority.HIGH -> "Hi"
    Priority.MEDIUM_HIGH -> "M+"
    Priority.MEDIUM -> "Med"
    Priority.MEDIUM_LOW -> "M-"
    Priority.LOW -> "Lo"
}
