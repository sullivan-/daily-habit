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
    private var lastIntervalChimeMs: Long = -1
    private var thresholdChimeFired: Boolean = false

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
            selectedActivityId = null,
            activeActivity = null
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

    fun expandActivity() {
        _uiState.value = _uiState.value.copy(layout = Layout.ACTIVITY_FOCUSED)
        val habitId = _uiState.value.selectedHabitId ?: return
        viewModelScope.launch {
            val completed = activityRepo.completedHistoryForHabit(habitId)
            val inProgress = _uiState.value.activeActivity
            val all = if (inProgress != null) completed + inProgress else completed
            _uiState.value = _uiState.value.copy(
                historyActivities = all,
                historyIndex = all.lastIndex
            )
        }
    }

    fun selectHabit(habitId: String) {
        if (_uiState.value.timerRunning && _uiState.value.selectedHabitId != habitId) {
            return
        }
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
        }
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
            selectedActivityId = null,
            activeActivity = null
        )
    }

    fun startTimer() {
        val state = _uiState.value
        val activity = state.activeActivity ?: return
        if (state.timerRunning) return

        val started = activity.copy(startTime = Instant.now())
        _uiState.value = _uiState.value.copy(
            activeActivity = started,
            timerRunning = true
        )

        viewModelScope.launch { activityRepo.update(started) }

        lastIntervalChimeMs = 0
        thresholdChimeFired = false
        startTimerTick()
    }

    private fun startTimerTick() {
        timerJob?.cancel()
        val habit = _uiState.value.selectedHabit
        val chimeIntervalMs = habit?.chimeIntervalSeconds?.let { it * 1000L } ?: 0
        val thresholdMs = habit?.thresholdMinutes?.let { it * 60 * 1000L } ?: 0

        _uiState.value = _uiState.value.copy(timerRunning = true)

        timerJob = viewModelScope.launch(tickDispatcher) {
            while (isActive) {
                delay(200)
                val activity = _uiState.value.activeActivity ?: break
                val elapsed = activity.elapsedMs

                // trigger UI recomposition
                _uiState.value = _uiState.value.copy(timerTickMs = elapsed)

                if (chimeIntervalMs > 0) {
                    val prevCount = lastIntervalChimeMs / chimeIntervalMs
                    val currCount = elapsed / chimeIntervalMs
                    if (currCount > prevCount) {
                        _chimeEvents.tryEmit(ChimeEvent.Interval)
                        lastIntervalChimeMs = elapsed
                    }
                }

                if (thresholdMs > 0 && !thresholdChimeFired && elapsed >= thresholdMs) {
                    _chimeEvents.tryEmit(ChimeEvent.Threshold)
                    thresholdChimeFired = true
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
        if (state.browsingHistory) {
            val activity = state.historyActivity ?: return
            val updated = activity.copy(note = note)
            val newHistory = state.historyActivities.toMutableList()
            newHistory[state.historyIndex] = updated
            _uiState.value = state.copy(historyActivities = newHistory)
            viewModelScope.launch { activityRepo.update(updated) }
        } else {
            val activity = state.activeActivity ?: return
            val updated = activity.copy(note = note)
            _uiState.value = state.copy(activeActivity = updated)
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
        val idx = state.historyActivities.indexOfFirst { it.id == activityId }
        if (idx < 0) return
        val updated = transform(state.historyActivities[idx])
        val newHistory = state.historyActivities.toMutableList()
        newHistory[idx] = updated
        _uiState.value = state.copy(historyActivities = newHistory)
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
            timerTickMs = 0,
            selectedHabitId = habitId,
            selectedActivityId = null
        )
    }

    fun historyOlder() {
        val state = _uiState.value
        if (state.historyIndex > 0) {
            _uiState.value = state.copy(historyIndex = state.historyIndex - 1)
        }
    }

    fun historyNewer() {
        val state = _uiState.value
        if (state.historyIndex < state.historyActivities.lastIndex) {
            _uiState.value = state.copy(historyIndex = state.historyIndex + 1)
        }
    }

    fun historyBackToCurrent() {
        val state = _uiState.value
        _uiState.value = state.copy(historyIndex = state.historyActivities.lastIndex)
    }
}
