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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    private val _chimeEvents = MutableSharedFlow<ChimeEvent>(extraBufferCapacity = 5)
    val chimeEvents: SharedFlow<ChimeEvent> = _chimeEvents.asSharedFlow()

    private var timerJob: Job? = null
    private var goalChimeFired: Boolean = false
    private var stopChimeFired: Boolean = false

    init {
        viewModelScope.launch {
            val today = dayBoundary.today()
            combine(
                habitRepo.allHabits(),
                activityRepo.activitiesForDate(today)
            ) { habits, activities ->
                _uiState.value.copy(
                    habits = habits,
                    todayActivities = activities,
                    today = today
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
        viewModelScope.launch {
            val active = activityRepo.activeActivity() ?: return@launch
            _uiState.value = _uiState.value.copy(
                selectedHabitId = active.habitId,
                activeActivity = active,
                timedHabitId = active.habitId
            )
            val habit = habitRepo.getById(active.habitId)
            if (habit?.timed == true) {
                startTimerTick()
            }
        }
    }

    fun switchToReview() {
        _uiState.value = _uiState.value.copy(
            layout = Layout.REVIEW,
            selectedActivityId = null
        )
    }

    fun switchToMain() {
        _uiState.value = _uiState.value.copy(
            layout = Layout.MAIN,
            selectedActivityId = null,
            historyActivities = emptyList(),
            historyIndex = -1
        )
    }

    fun collapseActivity() {
        _uiState.value = _uiState.value.copy(
            layout = _uiState.value.previousLayout,
            historyActivities = emptyList(),
            historyIndex = -1,
            historyAnchorIndex = -1
        )
    }

    fun expandActivity() {
        val state = _uiState.value
        _uiState.value = state.copy(
            previousLayout = state.layout,
            layout = Layout.ACTIVITY_FOCUSED
        )
        val habitId = state.selectedHabitId ?: return
        viewModelScope.launch {
            loadHistory(habitId, state.selectedActivityId)
        }
    }

    fun selectHabit(habitId: String) {
        _uiState.value = _uiState.value.copy(
            selectedHabitId = habitId,
            selectedActivityId = null,
            activeActivity = null,
            layout = Layout.MAIN
        )
        viewModelScope.launch {
            val today = dayBoundary.today()
            val existing = activityRepo.inProgressActivity(habitId, today)
            if (existing != null) {
                _uiState.value = _uiState.value.copy(activeActivity = existing)
                if (existing.startTime != null) {
                    startTimerTick()
                }
            } else {
                val habit = habitRepo.getById(habitId) ?: return@launch
                val new = Activity(
                    habitId = habitId,
                    attributedDate = today,
                    startTime = null,
                    note = habit.dailyTexts[today.dayOfWeek] ?: "",
                    completedAt = null
                )
                val id = activityRepo.create(new)
                _uiState.value = _uiState.value.copy(
                    activeActivity = new.copy(id = id)
                )
            }
            loadHistory(habitId)
        }
    }

    private suspend fun loadHistory(habitId: String, selectedActivityId: Long? = null) {
        val completed = activityRepo.completedHistoryForHabit(habitId)
        val inProgress = _uiState.value.activeActivity
            ?: activityRepo.inProgressActivity(habitId, dayBoundary.today())
        val all = if (inProgress != null) completed + inProgress else completed
        val index = if (selectedActivityId != null) {
            all.indexOfFirst { it.id == selectedActivityId }
                .takeIf { it >= 0 } ?: all.lastIndex
        } else {
            all.lastIndex
        }
        _uiState.value = _uiState.value.copy(
            activeActivity = _uiState.value.activeActivity ?: inProgress,
            historyActivities = all,
            historyIndex = index,
            historyAnchorIndex = index
        )
    }

    fun selectCompletedActivity(activityId: Long) {
        val habitId = _uiState.value.todayActivities
            .find { it.id == activityId }?.habitId
        _uiState.value = _uiState.value.copy(
            selectedActivityId = activityId,
            selectedHabitId = habitId
        )
        if (habitId != null) {
            viewModelScope.launch { loadHistory(habitId, activityId) }
        }
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedHabitId = null,
            selectedActivityId = null,
            activeActivity = null
        )
    }

    fun skipActivity() {
        val activity = _uiState.value.activeActivity ?: return
        if (activity.completedAt != null) return
        _uiState.value = _uiState.value.copy(
            selectedHabitId = null,
            selectedActivityId = null,
            activeActivity = null,
            layout = Layout.MAIN,
            historyActivities = emptyList(),
            historyIndex = -1,
            historyAnchorIndex = -1
        )
        viewModelScope.launch {
            activityRepo.delete(activity)
        }
    }

    fun deleteActivity() {
        val activity = _uiState.value.activeActivity ?: return
        if (_uiState.value.timerRunning) {
            timerJob?.cancel()
        }
        _uiState.value = _uiState.value.copy(
            selectedHabitId = null,
            selectedActivityId = null,
            activeActivity = null,
            timerRunning = false,
            timedHabitId = null,
            layout = Layout.MAIN,
            historyActivities = emptyList(),
            historyIndex = -1,
            historyAnchorIndex = -1
        )
        viewModelScope.launch {
            activityRepo.delete(activity)
        }
    }

    fun startTimer() {
        val state = _uiState.value
        val activity = state.activeActivity ?: return
        if (state.timerRunning) return

        val started = activity.copy(startTime = Instant.now())
        _uiState.value = _uiState.value.copy(
            activeActivity = started,
            timerRunning = true,
            timedHabitId = state.selectedHabitId
        )

        viewModelScope.launch { activityRepo.update(started) }

        goalChimeFired = false
        stopChimeFired = false
        startTimerTick()
    }

    private fun startTimerTick() {
        timerJob?.cancel()
        val state = _uiState.value
        val timedHabitId = state.timedHabitId ?: state.selectedHabitId
        val habit = state.habits.find { it.id == timedHabitId }
        val goalMs = habit?.goalMinutes?.let { it * 60 * 1000L } ?: 0
        val stopMs = habit?.stopMinutes?.let { it * 60 * 1000L } ?: 0
        val currentElapsed = state.activeActivity?.elapsedMs ?: 0
        if (goalMs > 0 && currentElapsed >= goalMs) goalChimeFired = true
        if (stopMs > 0 && currentElapsed >= stopMs) stopChimeFired = true

        _uiState.value = state.copy(timerRunning = true, timedHabitId = timedHabitId)

        timerJob = viewModelScope.launch(tickDispatcher) {
            while (isActive) {
                delay(200)
                // find the timed activity even if user switched to a different habit
                val currentState = _uiState.value
                val timedActivity = if (currentState.selectedHabitId == timedHabitId) {
                    currentState.activeActivity
                } else {
                    currentState.todayActivities.find {
                        it.habitId == timedHabitId && it.completedAt == null && it.startTime != null
                    }
                } ?: break
                val elapsed = timedActivity.elapsedMs

                _uiState.value = currentState.copy(timerTickMs = elapsed)

                if (goalMs > 0 && !goalChimeFired && elapsed >= goalMs) {
                    _chimeEvents.tryEmit(ChimeEvent.Threshold)
                    goalChimeFired = true
                }
                if (stopMs > 0 && !stopChimeFired && elapsed >= stopMs) {
                    _chimeEvents.tryEmit(ChimeEvent.Threshold)
                    stopChimeFired = true
                }
            }
        }
    }

    fun cancelTimer() {
        val state = _uiState.value
        val activity = state.activeActivity ?: return

        timerJob?.cancel()
        timerJob = null

        viewModelScope.launch {
            activityRepo.delete(activity)
        }

        // create a fresh activity for this habit
        val habitId = activity.habitId
        _uiState.value = state.copy(
            activeActivity = null,
            timerRunning = false,
            timedHabitId = null,
            timerTickMs = 0
        )

        viewModelScope.launch {
            val today = dayBoundary.today()
            val habit = habitRepo.getById(habitId) ?: return@launch
            val new = Activity(
                habitId = habitId,
                attributedDate = today,
                startTime = null,
                note = habit.dailyTexts[today.dayOfWeek] ?: "",
                completedAt = null
            )
            val id = activityRepo.create(new)
            _uiState.value = _uiState.value.copy(
                activeActivity = new.copy(id = id)
            )
        }
    }

    fun completeActivity(note: String) {
        val state = _uiState.value
        val habitId = state.selectedHabitId ?: return

        timerJob?.cancel()
        timerJob = null

        val now = Instant.now()

        val nextHabit = state.agendaItems
            .firstOrNull { it.habit.id != habitId }

        _uiState.value = state.copy(
            activeActivity = null,
            timerRunning = false,
            timedHabitId = null,
            timerTickMs = 0,
            selectedHabitId = nextHabit?.habit?.id
        )

        viewModelScope.launch {
            val activity = state.activeActivity
                ?: activityRepo.inProgressActivity(habitId, dayBoundary.today())
            if (activity != null) {
                val completed = activity.copy(
                    note = note,
                    completedAt = now
                )
                activityRepo.update(completed)
            } else {
                activityRepo.create(Activity(
                    habitId = habitId,
                    attributedDate = dayBoundary.today(),
                    startTime = null,
                    note = note,
                    completedAt = now
                ))
            }
        }

        nextHabit?.let { selectHabit(it.habit.id) }
    }

    fun completeUntimed(habitId: String, note: String) {
        val state = _uiState.value
        val activity = state.activeActivity

        viewModelScope.launch {
            if (activity != null && activity.habitId == habitId) {
                val completed = activity.copy(
                    note = note,
                    completedAt = Instant.now()
                )
                activityRepo.update(completed)
            } else {
                val today = dayBoundary.today()
                val habit = habitRepo.getById(habitId) ?: return@launch
                val new = Activity(
                    habitId = habitId,
                    attributedDate = today,
                    startTime = null,
                    note = note.ifEmpty {
                        habit.dailyTexts[today.dayOfWeek] ?: ""
                    },
                    completedAt = Instant.now()
                )
                activityRepo.create(new)
            }

            val nextHabit = _uiState.value.agendaItems
                .firstOrNull { it.habit.id != habitId }
            _uiState.value = _uiState.value.copy(
                activeActivity = null,
                selectedHabitId = nextHabit?.habit?.id
            )
            nextHabit?.let { selectHabit(it.habit.id) }
        }
    }

    fun updateNote(note: String) {
        val state = _uiState.value
        val showingHistory = state.browsingHistory &&
            (!state.isAtNewest || state.historyActivity?.completedAt != null)

        if (showingHistory) {
            val activity = state.historyActivity ?: return
            val updated = activity.copy(note = note)
            val newHistory = state.historyActivities.toMutableList()
            newHistory[state.historyIndex] = updated
            _uiState.value = state.copy(historyActivities = newHistory)
            viewModelScope.launch { activityRepo.update(updated) }
        } else if (state.selectedActivityId != null) {
            val activity = state.todayActivities.find {
                it.id == state.selectedActivityId
            } ?: return
            val updated = activity.copy(note = note)
            viewModelScope.launch { activityRepo.update(updated) }
        } else {
            val activity = state.activeActivity ?: return
            val updated = activity.copy(note = note)
            val newHistory = if (state.browsingHistory) {
                val list = state.historyActivities.toMutableList()
                val idx = list.indexOfFirst { it.id == updated.id }
                if (idx >= 0) list[idx] = updated
                list
            } else state.historyActivities
            _uiState.value = state.copy(
                activeActivity = updated,
                historyActivities = newHistory
            )
            viewModelScope.launch { activityRepo.update(updated) }
        }
    }

    fun updateActivityStartTime(activityId: Long, startTime: Instant?) {
        updateHistoryActivity(activityId) { it.copy(startTime = startTime) }
    }

    fun updateActivityCompletedAt(activityId: Long, completedAt: Instant?) {
        updateHistoryActivity(activityId) { activity ->
            val updated = activity.copy(completedAt = completedAt)
            val newAttributedDate = completedAt?.let { dayBoundary.attributedDate(it) }
                ?: activity.attributedDate
            updated.copy(attributedDate = newAttributedDate)
        }
    }

    private fun updateHistoryActivity(activityId: Long, transform: (Activity) -> Activity) {
        val state = _uiState.value

        val activeUpdated = if (state.activeActivity?.id == activityId) {
            transform(state.activeActivity)
        } else null

        val idx = state.historyActivities.indexOfFirst { it.id == activityId }
        val newHistory = if (idx >= 0) {
            state.historyActivities.toMutableList().also {
                it[idx] = activeUpdated ?: transform(it[idx])
            }
        } else state.historyActivities

        val updated = activeUpdated
            ?: newHistory.getOrNull(idx)
            ?: return

        _uiState.value = state.copy(
            historyActivities = newHistory,
            activeActivity = activeUpdated ?: state.activeActivity
        )
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
        timerJob?.cancel()
        timerJob = null
        val activity = _uiState.value.activeActivity
        if (activity != null) {
            val completed = activity.copy(
                completedAt = Instant.now()
            )
            viewModelScope.launch { activityRepo.update(completed) }
        }
        _uiState.value = _uiState.value.copy(
            activeActivity = null,
            timerRunning = false,
            timedHabitId = null,
            timerTickMs = 0,
            selectedHabitId = habitId,
            selectedActivityId = null
        )
    }

    fun historyOlder() {
        val state = _uiState.value
        if (state.historyIndex > 0) {
            val newIndex = state.historyIndex - 1
            val activity = state.historyActivities[newIndex]
            _uiState.value = state.copy(
                historyIndex = newIndex,
                selectedActivityId = if (activity.completedAt != null) activity.id else null,
                activeActivity = if (activity.completedAt == null) activity else state.activeActivity
            )
        }
    }

    fun historyNewer() {
        val state = _uiState.value
        if (state.historyIndex < state.historyActivities.lastIndex) {
            val newIndex = state.historyIndex + 1
            val activity = state.historyActivities[newIndex]
            _uiState.value = state.copy(
                historyIndex = newIndex,
                selectedActivityId = if (activity.completedAt != null) activity.id else null,
                activeActivity = if (activity.completedAt == null) activity else state.activeActivity
            )
        }
    }

    fun historyBackToAnchor() {
        val state = _uiState.value
        val anchorActivity = state.historyActivities.getOrNull(state.historyAnchorIndex)
        _uiState.value = state.copy(
            historyIndex = state.historyAnchorIndex,
            selectedActivityId = anchorActivity?.let {
                if (it.completedAt != null) it.id else null
            },
            activeActivity = anchorActivity?.let {
                if (it.completedAt == null) it else state.activeActivity
            } ?: state.activeActivity
        )
    }
}
