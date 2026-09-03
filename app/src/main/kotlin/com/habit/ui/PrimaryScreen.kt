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
import com.habit.data.EasyDayLevel
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
    var showEasyDayDialog by remember { mutableStateOf(false) }

    if (showEasyDayDialog) {
        EasyDayDialog(
            current = uiState.easyDayLevel,
            carryOver = uiState.easyDayCarryOver,
            onSelect = viewModel::setEasyDayLevel,
            onToggleCarryOver = viewModel::setEasyDayCarryOver,
            onDismiss = { showEasyDayDialog = false }
        )
    }
    val easyDaySubLabel = if (uiState.easyDayLevel == EasyDayLevel.OFF) null
        else easyDayLabel(uiState.easyDayLevel)

    if (showOtherDialog) {
        val isPast = uiState.isViewingPastDay
        val otherHabits = if (isPast) uiState.pastDayOtherHabits else uiState.otherHabits
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
                                    if (isPast) viewModel.backFill(habit.id)
                                    else viewModel.selectHabit(habit.id)
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
        if (uiState.layout == Layout.REVIEW) {
            DateStrip(
                label = dateStripLabel(uiState.selectedDate, uiState.today),
                canStepBack = uiState.selectedDate > uiState.today.minusDays(7),
                canStepForward = uiState.isViewingPastDay,
                onStepBack = { viewModel.stepDate(-1) },
                onStepForward = { viewModel.stepDate(1) },
                onLabelTap = viewModel::goToToday
            )
        }
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
            onDelete = {
                if (uiState.selectedActivityId != null && uiState.activeActivity == null) {
                    viewModel.deleteCompletedActivity()
                } else {
                    viewModel.deleteActivity()
                }
            },
            onSelectTrack = viewModel::selectTrack,
            onSelectMilestone = viewModel::selectMilestone,
            onCompleteMilestone = viewModel::toggleMilestoneChecked,
            onOpenIntervalSelector = viewModel::openIntervalSelector,
            onCloseIntervalSelector = viewModel::closeIntervalSelector,
            onStartIntervalChime = viewModel::startIntervalChime,
            onCancelIntervalChime = viewModel::cancelIntervalChime,
            onChangeIntervalChime = viewModel::changeIntervalChime,
            onShowTrackHistory = viewModel::showTrackHistory,
            onHideTrackHistory = viewModel::hideTrackHistory,
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
                    uiState.selectedDateActivities,
                    LocalDateTime.now(),
                    uiState.easyDayLevel
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
                    onEasyDay = { showEasyDayDialog = true },
                    easyDaySubLabel = easyDaySubLabel,
                    onDayPlan = viewModel::switchToMain,
                    onDoneToday = viewModel::switchToReview,
                    onSwipeLeft = viewModel::switchToReview,
                    onSwipeRight = onChoices
                )
            }
            Layout.REVIEW -> {
                val isPast = uiState.isViewingPastDay
                CompletedList(
                    items = uiState.completedItems,
                    missed = uiState.missedItems,
                    showOther = isPast,
                    onSelect = viewModel::selectCompletedActivity,
                    onDoAgain = if (isPast) viewModel::backFill else viewModel::doAgain,
                    onBackfillMissed = viewModel::backFill,
                    onOther = { showOtherDialog = true },
                    modifier = Modifier.weight(1f)
                )
                AgendaBar(
                    remaining = uiState.totalTarget - uiState.progressCount,
                    onClick = viewModel::switchToMain,
                    onNewHabit = onNewHabit,
                    onHabitList = onHabitList,
                    onChoices = onChoices,
                    onEasyDay = if (isPast) null else ({ showEasyDayDialog = true }),
                    easyDaySubLabel = if (isPast) null else easyDaySubLabel,
                    onDayPlan = viewModel::switchToMain,
                    onDoneToday = viewModel::goToToday,
                    onSwipeRight = viewModel::switchToMain
                )
            }
            Layout.ACTIVITY_FOCUSED -> {
                // expanded activity view fills remaining space via weight on parent
                val isPast = uiState.isViewingPastDay
                AgendaBar(
                    remaining = uiState.totalTarget - uiState.progressCount,
                    onClick = viewModel::switchToMain,
                    onNewHabit = onNewHabit,
                    onHabitList = onHabitList,
                    onChoices = onChoices,
                    onEasyDay = if (isPast) null else ({ showEasyDayDialog = true }),
                    easyDaySubLabel = if (isPast) null else easyDaySubLabel,
                    onDayPlan = viewModel::switchToMain,
                    onDoneToday = if (isPast) viewModel::goToToday else viewModel::switchToReview,
                    onSwipeRight = viewModel::switchToMain
                )
            }
        }
    }
}
