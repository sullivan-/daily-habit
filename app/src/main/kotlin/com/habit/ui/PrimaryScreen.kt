package com.habit.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.habit.viewmodel.AgendaViewModel
import com.habit.viewmodel.Layout
import com.habit.viewmodel.progressRatios
import java.time.LocalDateTime

@Composable
fun PrimaryScreen(viewModel: AgendaViewModel, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        ActivityView(
            state = uiState,
            onStart = viewModel::startTimer,
            onStop = viewModel::stopTimer,
            onComplete = viewModel::completeActivity,
            onCompleteUntimed = viewModel::completeUntimed,
            onNoteChange = viewModel::updateNote,
            onToggleDetail = {
                if (uiState.layout == Layout.ACTIVITY_FOCUSED)
                    viewModel.switchToMain()
                else
                    viewModel.expandActivity()
            }
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
                    onClick = viewModel::switchToReview
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
                    onClick = viewModel::switchToMain
                )
            }
            Layout.ACTIVITY_FOCUSED -> {
                ActivityDetail(
                    state = uiState,
                    modifier = Modifier.weight(1f)
                )
                AgendaBar(
                    remaining = uiState.totalTarget - uiState.progressCount,
                    onClick = viewModel::switchToMain
                )
            }
        }
    }
}
