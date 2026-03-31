package com.habit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habit.data.Activity
import com.habit.data.ActivityRepository
import com.habit.data.DayBoundary
import com.habit.data.HabitRepository
import com.habit.data.TargetMode
import java.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AgendaViewModel(
    private val habitRepo: HabitRepository,
    private val activityRepo: ActivityRepository,
    private val dayBoundary: DayBoundary,
    private val tickDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _uiState = MutableStateFlow(AgendaUiState())
    val uiState: StateFlow<AgendaUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var timerStartEpochMs: Long = 0
    private var timerAccumulatedMs: Long = 0

    init {
        viewModelScope.launch {
            val today = dayBoundary.today()
            combine(
                habitRepo.allHabits(),
                activityRepo.activitiesForDate(today)
            ) { habits, activities ->
                _uiState.value.copy(
                    habits = habits,
                    todayActivities = activities
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun switchToReview() {
        _uiState.value = _uiState.value.copy(
            layout = Layout.REVIEW,
            selectedHabitId = null,
            selectedActivityId = null
        )
    }

    fun switchToMain() {
        _uiState.value = _uiState.value.copy(
            layout = Layout.MAIN,
            selectedActivityId = null
        )
    }

    fun expandActivity() {
        _uiState.value = _uiState.value.copy(layout = Layout.ACTIVITY_FOCUSED)
    }

    fun selectHabit(habitId: String) {
        if (_uiState.value.timerRunning && _uiState.value.selectedHabitId != habitId) {
            return
        }
        _uiState.value = _uiState.value.copy(
            selectedHabitId = habitId,
            selectedActivityId = null,
            layout = Layout.MAIN
        )
    }

    fun selectCompletedActivity(activityId: Long) {
        _uiState.value = _uiState.value.copy(
            selectedActivityId = activityId,
            selectedHabitId = _uiState.value.todayActivities
                .find { it.id == activityId }?.habitId
        )
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedHabitId = null,
            selectedActivityId = null
        )
    }

    fun startTimer() {
        val state = _uiState.value
        val habitId = state.selectedHabitId ?: return

        if (state.timerRunning) return

        viewModelScope.launch {
            val activity = state.activeActivity ?: run {
                val today = dayBoundary.today()
                val new = Activity(
                    habitId = habitId,
                    attributedDate = today,
                    startTime = Instant.now(),
                    endTime = null,
                    elapsedMs = 0,
                    note = state.selectedHabit?.dailyTexts
                        ?.get(today.dayOfWeek) ?: "",
                    completedAt = null
                )
                val id = activityRepo.create(new)
                new.copy(id = id)
            }

            timerAccumulatedMs = activity.elapsedMs
            timerStartEpochMs = System.currentTimeMillis()

            _uiState.value = _uiState.value.copy(
                activeActivity = activity,
                timerRunning = true,
                elapsedMs = timerAccumulatedMs
            )

            timerJob = launch(tickDispatcher) {
                while (isActive) {
                    delay(200)
                    val elapsed = timerAccumulatedMs +
                        (System.currentTimeMillis() - timerStartEpochMs)
                    _uiState.value = _uiState.value.copy(elapsedMs = elapsed)
                }
            }
        }
    }

    fun stopTimer() {
        if (!_uiState.value.timerRunning) return

        timerJob?.cancel()
        timerJob = null

        val elapsed = timerAccumulatedMs +
            (System.currentTimeMillis() - timerStartEpochMs)
        timerAccumulatedMs = elapsed

        val activity = _uiState.value.activeActivity?.copy(elapsedMs = elapsed)
        _uiState.value = _uiState.value.copy(
            timerRunning = false,
            elapsedMs = elapsed,
            activeActivity = activity
        )

        activity?.let {
            viewModelScope.launch { activityRepo.update(it) }
        }
    }

    fun completeActivity(note: String) {
        val state = _uiState.value
        val activity = state.activeActivity ?: return

        timerJob?.cancel()
        timerJob = null

        val finalElapsed = if (state.timerRunning) {
            timerAccumulatedMs + (System.currentTimeMillis() - timerStartEpochMs)
        } else {
            state.elapsedMs
        }

        val completed = activity.copy(
            endTime = Instant.now(),
            elapsedMs = finalElapsed,
            note = note,
            completedAt = Instant.now()
        )

        viewModelScope.launch {
            activityRepo.update(completed)
        }

        timerAccumulatedMs = 0

        val nextHabit = state.agendaItems
            .firstOrNull { it.habit.id != state.selectedHabitId }

        _uiState.value = state.copy(
            activeActivity = null,
            timerRunning = false,
            elapsedMs = 0,
            selectedHabitId = nextHabit?.habit?.id
        )
    }

    fun completeUntimed(habitId: String, note: String) {
        viewModelScope.launch {
            val today = dayBoundary.today()
            val habit = habitRepo.getById(habitId) ?: return@launch
            val activity = Activity(
                habitId = habitId,
                attributedDate = today,
                startTime = null,
                endTime = null,
                elapsedMs = 0,
                note = note.ifEmpty {
                    habit.dailyTexts[today.dayOfWeek] ?: ""
                },
                completedAt = Instant.now()
            )
            activityRepo.create(activity)

            val nextHabit = _uiState.value.agendaItems
                .firstOrNull { it.habit.id != habitId }
            _uiState.value = _uiState.value.copy(
                selectedHabitId = nextHabit?.habit?.id
            )
        }
    }

    fun updateNote(note: String) {
        val activity = _uiState.value.activeActivity ?: return
        val updated = activity.copy(note = note)
        _uiState.value = _uiState.value.copy(activeActivity = updated)
        viewModelScope.launch { activityRepo.update(updated) }
    }

    fun doAgain(habitId: String) {
        val habit = _uiState.value.habits.find { it.id == habitId } ?: return
        if (habit.dailyTargetMode != TargetMode.AT_LEAST) return

        _uiState.value = _uiState.value.copy(
            layout = Layout.MAIN,
            selectedHabitId = habitId,
            selectedActivityId = null,
            activeActivity = null
        )
    }

    fun forceSelectHabit(habitId: String) {
        stopTimer()
        val activity = _uiState.value.activeActivity
        if (activity != null) {
            val completed = activity.copy(
                endTime = Instant.now(),
                completedAt = Instant.now()
            )
            viewModelScope.launch { activityRepo.update(completed) }
        }
        _uiState.value = _uiState.value.copy(
            activeActivity = null,
            timerRunning = false,
            elapsedMs = 0,
            selectedHabitId = habitId,
            selectedActivityId = null
        )
    }
}
