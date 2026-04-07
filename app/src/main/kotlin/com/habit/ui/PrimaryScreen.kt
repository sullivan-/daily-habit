package com.habit.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.habit.viewmodel.AgendaViewModel
import com.habit.viewmodel.Layout
import com.habit.viewmodel.progressRatios
import java.time.LocalDateTime

@Composable
fun PrimaryScreen(
    viewModel: AgendaViewModel,
    onNewHabit: () -> Unit = {},
    onEditHabit: (String) -> Unit = {},
    onHabitList: () -> Unit = {},
    onChoices: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    var showOtherDialog by remember { mutableStateOf(false) }

    if (showOtherDialog) {
        val otherHabits = uiState.otherHabits
        AlertDialog(
            onDismissRequest = { showOtherDialog = false },
            title = { Text("Other") },
            text = {
                LazyColumn {
                    items(otherHabits, key = { it.id }) { habit ->
                        Text(
                            text = habit.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showOtherDialog = false
                                    viewModel.selectHabit(habit.id)
                                }
                                .padding(vertical = 12.dp)
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant
                                .copy(alpha = 0.3f)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOtherDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { focusManager.clearFocus() }
    ) {
        val expandedModifier = if (uiState.layout == Layout.ACTIVITY_FOCUSED)
            Modifier.weight(1f) else Modifier
        ActivityView(
            state = uiState,
            onStart = viewModel::startTimer,
            onFinish = viewModel::completeActivity,
            onCancel = viewModel::cancelTimer,
            onCompleteUntimed = viewModel::completeUntimed,
            onNoteChange = viewModel::updateNote,
            onToggleDetail = {
                if (uiState.layout == Layout.ACTIVITY_FOCUSED)
                    viewModel.collapseActivity()
                else
                    viewModel.expandActivity()
            },
            onHistoryOlder = viewModel::historyOlder,
            onHistoryNewer = viewModel::historyNewer,
            onHistoryBackToAnchor = viewModel::historyBackToAnchor,
            onEditHabit = onEditHabit,
            onUpdateStartTime = viewModel::updateActivityStartTime,
            onUpdateCompletedAt = viewModel::updateActivityCompletedAt,
            onDoAgain = viewModel::doAgain,
            onSkip = viewModel::skipActivity,
            onDelete = viewModel::deleteActivity,
            modifier = expandedModifier
        )

        when (uiState.layout) {
            Layout.MAIN -> {
                AgendaList(
                    items = uiState.agendaItems,
                    onSelect = viewModel::selectHabit,
                    hasOtherHabits = uiState.otherHabits.isNotEmpty(),
                    onOther = { showOtherDialog = true },
                    modifier = Modifier.weight(1f)
                )
                val ratios = progressRatios(
                    uiState.habits,
                    uiState.todayActivities,
                    LocalDateTime.now()
                )
                ProgressBar(
                    completed = uiState.progressCount,
                    total = uiState.totalTarget,
                    completedOverTotal = ratios.completedOverTotal,
                    completedOverExpected = ratios.completedOverExpected,
                    onClick = viewModel::switchToReview,
                    onNewHabit = onNewHabit,
                    onHabitList = onHabitList,
                    onChoices = onChoices,
                    onSwipeLeft = viewModel::switchToReview
                )
            }
            Layout.REVIEW -> {
                CompletedList(
                    items = uiState.completedItems,
                    onSelect = viewModel::selectCompletedActivity,
                    onDoAgain = viewModel::doAgain,
                    modifier = Modifier.weight(1f)
                )
                AgendaBar(
                    remaining = uiState.totalTarget - uiState.progressCount,
                    onClick = viewModel::switchToMain,
                    onNewHabit = onNewHabit,
                    onHabitList = onHabitList,
                    onChoices = onChoices,
                    onSwipeLeft = onChoices,
                    onSwipeRight = viewModel::switchToMain
                )
            }
            Layout.ACTIVITY_FOCUSED -> {
                // expanded activity view fills remaining space via weight on parent
                AgendaBar(
                    remaining = uiState.totalTarget - uiState.progressCount,
                    onClick = viewModel::switchToMain,
                    onNewHabit = onNewHabit,
                    onHabitList = onHabitList,
                    onChoices = onChoices,
                    onSwipeLeft = onChoices,
                    onSwipeRight = viewModel::switchToMain
                )
            }
        }
    }
}
