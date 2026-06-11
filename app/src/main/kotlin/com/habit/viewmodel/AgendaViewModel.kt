package com.habit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habit.data.Activity
import com.habit.data.ActivityRepository
import com.habit.data.DayBoundary
import com.habit.data.EasyDayLevel
import com.habit.data.EasyDayRepository
import com.habit.data.HabitRepository
import com.habit.data.priorityToScore
import com.habit.data.TrackRepository
import com.habit.data.TargetMode
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AgendaViewModel(
    private val habitRepo: HabitRepository,
    private val activityRepo: ActivityRepository,
    private val dayBoundary: DayBoundary,
    private val trackRepo: TrackRepository? = null,
    private val easyDayRepo: EasyDayRepository? = null,
    private val tickDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _uiState = MutableStateFlow(AgendaUiState())
    val uiState: StateFlow<AgendaUiState> = _uiState.asStateFlow()

    private val _chimeEvents = MutableSharedFlow<ChimeEvent>(extraBufferCapacity = 5)
    val chimeEvents: SharedFlow<ChimeEvent> = _chimeEvents.asSharedFlow()

    private val today = MutableStateFlow(dayBoundary.today())

    private var timerJob: Job? = null
    private var nextIntervalChimeAtMs: Long = 0

    init {
        viewModelScope.launch {
            today.flatMapLatest { date ->
                val easyDayFlow = easyDayRepo?.flowForDate(date)
                    ?: flowOf(EasyDayLevel.OFF)
                val carryOverFlow = easyDayRepo?.carryOverFlow() ?: flowOf(false)
                combine(
                    habitRepo.allHabits(),
                    activityRepo.activitiesForDate(date),
                    easyDayFlow,
                    carryOverFlow
                ) { habits, activities, easyDayLevel, carryOver ->
                    _uiState.value.copy(
                        habits = habits,
                        todayActivities = activities,
                        today = date,
                        easyDayLevel = easyDayLevel,
                        easyDayCarryOver = carryOver
                    )
                }
            }.collect { newState ->
                _uiState.value = newState
            }
        }
        viewModelScope.launch(tickDispatcher) {
            while (isActive) {
                delay(30_000L)
                refreshToday()
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
            loadAndSetTracks(active.habitId)
            hydrateTrackStateForActivity(active)
        }
    }

    fun switchToReview() {
        _uiState.value = _uiState.value.copy(
            layout = Layout.REVIEW,
            selectedActivityId = null
        )
    }

    fun switchToMain() {
        val state = _uiState.value
        val resumeHabitId = if (state.timerRunning) state.timedHabitId else null
        _uiState.value = state.copy(
            layout = Layout.MAIN,
            selectedHabitId = resumeHabitId,
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
            historyAnchorIndex = -1,
            trackHistoryVisible = false,
            trackHistory = emptyList()
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
            layout = Layout.MAIN,
            selectedTrack = null,
            selectedMilestone = null,
            milestoneChecked = false,
            incompleteMilestones = emptyList(),
            availableTracks = emptyList()
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
                    note = "",
                    completedAt = null
                )
                val id = activityRepo.create(new)
                _uiState.value = _uiState.value.copy(
                    activeActivity = new.copy(id = id)
                )
            }
            loadHistory(habitId)
            loadAndSetTracks(habitId)
            autoSelectTodayTrack()
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
        val activity = _uiState.value.todayActivities.find { it.id == activityId } ?: return
        _uiState.value = _uiState.value.copy(
            selectedActivityId = activityId,
            selectedHabitId = activity.habitId
        )
        viewModelScope.launch {
            loadHistory(activity.habitId, activityId)
            loadAndSetTracks(activity.habitId)
            hydrateTrackStateForActivity(activity)
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
        nextIntervalChimeAtMs = 0
        val skipped = activity.copy(
            completedAt = Instant.now(),
            skipped = true
        )
        _uiState.value = _uiState.value.copy(
            selectedHabitId = null,
            selectedActivityId = null,
            activeActivity = null,
            layout = Layout.MAIN,
            historyActivities = emptyList(),
            historyIndex = -1,
            historyAnchorIndex = -1,
            intervalChimeState = IntervalChimeState.IDLE,
            intervalChimeMs = 0,
            intervalCountdownMs = 0
        )
        viewModelScope.launch {
            activityRepo.update(skipped)
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

    fun deleteCompletedActivity() {
        val state = _uiState.value
        val activity = state.todayActivities.find {
            it.id == state.selectedActivityId
        } ?: return
        _uiState.value = state.copy(
            selectedHabitId = null,
            selectedActivityId = null,
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

        startTimerTick()
    }

    private fun startTimerTick() {
        timerJob?.cancel()
        val state = _uiState.value
        val timedHabitId = state.timedHabitId ?: state.selectedHabitId

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

                _uiState.value = _uiState.value.copy(timerTickMs = elapsed)

                val chimeMs = _uiState.value.intervalChimeMs
                if (chimeMs > 0 && elapsed >= nextIntervalChimeAtMs) {
                    nextIntervalChimeAtMs += chimeMs
                }
                if (chimeMs > 0) {
                    _uiState.value = _uiState.value.copy(
                        intervalCountdownMs = maxOf(0, nextIntervalChimeAtMs - elapsed)
                    )
                }
            }
        }
    }

    fun cancelTimer() {
        val state = _uiState.value
        val activity = state.activeActivity ?: return

        timerJob?.cancel()
        timerJob = null
        nextIntervalChimeAtMs = 0

        viewModelScope.launch {
            activityRepo.delete(activity)
        }

        // create a fresh activity for this habit
        val habitId = activity.habitId
        _uiState.value = state.copy(
            activeActivity = null,
            timerRunning = false,
            timedHabitId = null,
            timerTickMs = 0,
            intervalChimeState = IntervalChimeState.IDLE,
            intervalChimeMs = 0,
            intervalCountdownMs = 0
        )

        viewModelScope.launch {
            val today = dayBoundary.today()
            val habit = habitRepo.getById(habitId) ?: return@launch
            val new = Activity(
                habitId = habitId,
                attributedDate = today,
                startTime = null,
                note = "",
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
        nextIntervalChimeAtMs = 0

        val now = Instant.now()

        _uiState.value = state.copy(
            activeActivity = null,
            timerRunning = false,
            timedHabitId = null,
            timerTickMs = 0,
            selectedHabitId = null,
            selectedActivityId = null,
            historyActivities = emptyList(),
            historyIndex = -1,
            historyAnchorIndex = -1,
            intervalChimeState = IntervalChimeState.IDLE,
            intervalChimeMs = 0,
            intervalCountdownMs = 0
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
            persistMilestoneIfChecked()
        }
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
                    note = note,
                    completedAt = Instant.now()
                )
                activityRepo.create(new)
            }

            persistMilestoneIfChecked()

            _uiState.value = _uiState.value.copy(
                activeActivity = null,
                selectedHabitId = null,
                selectedActivityId = null,
                historyActivities = emptyList(),
                historyIndex = -1,
                historyAnchorIndex = -1
            )
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

    fun showTrackHistory() {
        val habitId = _uiState.value.selectedHabitId ?: return
        viewModelScope.launch {
            val repo = trackRepo ?: return@launch
            val activities = activityRepo.completedHistoryForHabit(habitId)
            val recent = activities.takeLast(10)
            val items = recent.map { activity ->
                val trackName = activity.trackId?.let { repo.getById(it)?.name }
                val milestoneName = activity.milestoneId?.let {
                    repo.getMilestoneById(it)?.name
                }
                TrackHistoryItem(
                    activityId = activity.id,
                    completedAt = activity.completedAt!!,
                    trackName = trackName,
                    milestoneName = milestoneName,
                    note = activity.note
                )
            }.reversed()
            _uiState.value = _uiState.value.copy(
                trackHistory = items,
                trackHistoryVisible = true
            )
        }
    }

    fun hideTrackHistory() {
        _uiState.value = _uiState.value.copy(
            trackHistoryVisible = false,
            trackHistory = emptyList()
        )
    }

    fun refreshTracks() {
        val habitId = _uiState.value.selectedHabitId ?: return
        viewModelScope.launch { loadAndSetTracks(habitId) }
    }

    fun loadTracksForHabit(habitId: String) {
        viewModelScope.launch { loadAndSetTracks(habitId) }
    }

    private suspend fun hydrateTrackStateForActivity(activity: Activity?) {
        val repo = trackRepo
        val trackId = activity?.trackId
        if (repo == null || trackId == null) {
            _uiState.value = _uiState.value.copy(
                selectedTrack = null,
                selectedMilestone = null,
                incompleteMilestones = emptyList(),
                milestoneChecked = false
            )
            return
        }
        val track = repo.getById(trackId)
        val milestone = activity.milestoneId?.let { repo.getMilestoneById(it) }
        val incomplete = repo.incompleteMilestones(trackId)
        _uiState.value = _uiState.value.copy(
            selectedTrack = track,
            selectedMilestone = milestone,
            incompleteMilestones = incomplete,
            milestoneChecked = false
        )
    }

    private suspend fun loadAndSetTracks(habitId: String) {
        val repo = trackRepo ?: return
        val tracks = repo.activeTracksForHabit(habitId)
        val today = dayBoundary.today().dayOfWeek
        val sorted = tracks.sortedWith(
            compareByDescending<com.habit.data.Track> { it.dayOfWeek == today }
                .thenByDescending { priorityToScore(it.priority) }
        )
        _uiState.value = _uiState.value.copy(availableTracks = sorted)
    }

    private suspend fun autoSelectTodayTrack() {
        val repo = trackRepo ?: return
        val activity = _uiState.value.activeActivity ?: return
        if (activity.trackId != null) return
        val today = dayBoundary.today().dayOfWeek
        val todayTrack = _uiState.value.availableTracks.find { it.dayOfWeek == today }
            ?: return
        val milestone = repo.defaultMilestone(todayTrack.id)
        val incomplete = repo.incompleteMilestones(todayTrack.id)
        val updated = activity.copy(trackId = todayTrack.id, milestoneId = milestone?.id)
        activityRepo.update(updated)
        _uiState.value = _uiState.value.copy(
            activeActivity = updated,
            selectedTrack = todayTrack,
            selectedMilestone = milestone,
            incompleteMilestones = incomplete
        )
    }

    fun selectTrack(trackId: String?) {
        val repo = trackRepo ?: return
        viewModelScope.launch {
            val activity = _uiState.value.activeActivity ?: return@launch
            val track = trackId?.let { repo.getById(it) }
            val milestone = track?.let { repo.defaultMilestone(it.id) }
            val incomplete = track?.let { repo.incompleteMilestones(it.id) } ?: emptyList()

            val updated = activity.copy(trackId = trackId, milestoneId = milestone?.id)
            activityRepo.update(updated)

            _uiState.value = _uiState.value.copy(
                activeActivity = updated,
                selectedTrack = track,
                selectedMilestone = milestone,
                incompleteMilestones = incomplete
            )
        }
    }

    fun selectMilestone(milestoneId: Long) {
        val repo = trackRepo ?: return
        viewModelScope.launch {
            val activity = _uiState.value.activeActivity ?: return@launch
            val milestone = repo.getMilestoneById(milestoneId) ?: return@launch
            val updated = activity.copy(milestoneId = milestoneId)
            activityRepo.update(updated)

            _uiState.value = _uiState.value.copy(
                activeActivity = updated,
                selectedMilestone = milestone
            )
        }
    }

    fun toggleMilestoneChecked() {
        val current = _uiState.value.milestoneChecked
        _uiState.value = _uiState.value.copy(milestoneChecked = !current)
    }

    private suspend fun persistMilestoneIfChecked() {
        val repo = trackRepo ?: return
        if (!_uiState.value.milestoneChecked) return
        val milestone = _uiState.value.selectedMilestone ?: return
        repo.updateMilestone(milestone.copy(completed = true))
        _uiState.value = _uiState.value.copy(milestoneChecked = false)
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
        viewModelScope.launch { hydrateTrackStateForActivity(null) }
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

    fun openIntervalSelector() {
        _uiState.value = _uiState.value.copy(
            intervalChimeState = IntervalChimeState.SELECTING
        )
    }

    fun closeIntervalSelector() {
        _uiState.value = _uiState.value.copy(
            intervalChimeState = IntervalChimeState.IDLE
        )
    }

    fun startIntervalChime(intervalMs: Long) {
        val state = _uiState.value

        if (!state.timerRunning) {
            startTimer()
        }

        val isSeconds = IntervalOptions.isSecondsInterval(intervalMs)
        _chimeEvents.tryEmit(ChimeEvent.Interval(isSeconds))

        val elapsed = _uiState.value.activeActivity?.elapsedMs ?: 0
        nextIntervalChimeAtMs = elapsed + intervalMs

        _uiState.value = _uiState.value.copy(
            intervalChimeState = IntervalChimeState.RUNNING,
            intervalChimeMs = intervalMs,
            intervalCountdownMs = intervalMs
        )
    }

    fun cancelIntervalChime() {
        nextIntervalChimeAtMs = 0
        _uiState.value = _uiState.value.copy(
            intervalChimeState = IntervalChimeState.IDLE,
            intervalChimeMs = 0,
            intervalCountdownMs = 0
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

    fun refreshToday() {
        val current = dayBoundary.today()
        if (current != today.value) {
            today.value = current
        }
    }

    fun setEasyDayLevel(level: EasyDayLevel) {
        val repo = easyDayRepo ?: return
        viewModelScope.launch {
            if (_uiState.value.easyDayCarryOver) {
                repo.setCarryOver(enabled = true, level = level)
            } else {
                repo.setLevel(dayBoundary.today(), level)
            }
        }
    }

    fun setEasyDayCarryOver(enabled: Boolean) {
        val repo = easyDayRepo ?: return
        viewModelScope.launch {
            val level = _uiState.value.easyDayLevel
            if (enabled) {
                repo.setCarryOver(enabled = true, level = level)
            } else {
                repo.setCarryOver(enabled = false, level = level)
                repo.setLevel(dayBoundary.today(), level)
            }
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
