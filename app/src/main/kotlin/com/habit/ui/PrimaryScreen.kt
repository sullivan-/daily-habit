package com.habit.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
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
            modifier = expandedModifier
        )

        when (uiState.layout) {
            Layout.MAIN -> {
                AgendaList(
                    items = uiState.agendaItems,
                    onSelect = viewModel::selectHabit,
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
                    onChoices = onChoices
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
                    onChoices = onChoices
                )
            }
            Layout.ACTIVITY_FOCUSED -> {
                // expanded activity view fills remaining space via weight on parent
                AgendaBar(
                    remaining = uiState.totalTarget - uiState.progressCount,
                    onClick = viewModel::switchToMain,
                    onNewHabit = onNewHabit,
                    onHabitList = onHabitList,
                    onChoices = onChoices
                )
            }
        }
    }
}
