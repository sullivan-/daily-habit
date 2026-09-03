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
import com.habit.data.Milestone
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

    private val selectedDate = MutableStateFlow(dayBoundary.today())
    private val realToday = MutableStateFlow(dayBoundary.today())

    private var timerJob: Job? = null
    private var nextIntervalChimeAtMs: Long = 0

    init {
        sweepStalePlaceholders()
        viewModelScope.launch {
            combine(selectedDate, realToday) { sel, today -> sel to today }
                .flatMapLatest { (sel, today) ->
                    val easyDayFlow = easyDayRepo?.flowForDate(today)
                        ?: flowOf(EasyDayLevel.OFF)
                    val carryOverFlow = easyDayRepo?.carryOverFlow() ?: flowOf(false)
                    combine(
                        habitRepo.allHabits(),
                        activityRepo.activitiesForDate(sel),
                        easyDayFlow,
                        carryOverFlow
                    ) { habits, activities, easyDayLevel, carryOver ->
                        _uiState.value.copy(
                            habits = habits,
                            selectedDateActivities = activities,
                            selectedDate = sel,
                            today = today,
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
        selectedDate.value = realToday.value
        val state = _uiState.value
        val resumeHabitId = if (state.timerRunning) state.timedHabitId else null
        _uiState.value = state.copy(
            layout = Layout.MAIN,
            selectedHabitId = resumeHabitId,
            selectedActivityId = null,
            historyActivities = emptyList(),
            historyIndex = -1
        )
        if (resumeHabitId != null) restoreTimedActivityTracks(resumeHabitId)
    }

    // review may have put another activity's tracks and milestone on screen
    private fun restoreTimedActivityTracks(habitId: String) {
        viewModelScope.launch {
            loadAndSetTracks(habitId)
            hydrateTrackStateForActivity(_uiState.value.activeActivity)
        }
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
            checkedMilestones = emptyList(),
            incompleteMilestones = emptyList(),
            availableTracks = emptyList()
        )
        viewModelScope.launch {
            val today = dayBoundary.today()
            val existing = activityRepo.inProgressActivity(habitId, today)
            if (existing != null) {
                resumeInProgressActivity(existing)
            } else {
                createInProgressActivity(habitId, today) ?: return@launch
            }
            loadHistory(habitId)
            loadAndSetTracks(habitId)
            // an existing activity keeps whatever track the user chose, including none; only a
            // fresh activity gets the day-of-week default
            if (existing != null) hydrateTrackStateForActivity(existing) else autoSelectTodayTrack()
        }
    }

    private fun resumeInProgressActivity(existing: Activity) {
        _uiState.value = _uiState.value.copy(activeActivity = existing)
        if (existing.startTime != null) {
            startTimerTick()
        }
    }

    private suspend fun createInProgressActivity(
        habitId: String,
        today: LocalDate,
        trackId: String? = null,
        milestoneId: Long? = null
    ): Activity? {
        habitRepo.getById(habitId) ?: return null
        val new = Activity(
            habitId = habitId,
            attributedDate = today,
            startTime = null,
            note = "",
            completedAt = null,
            trackId = trackId,
            milestoneId = milestoneId
        )
        val id = activityRepo.create(new)
        val created = new.copy(id = id)
        _uiState.value = _uiState.value.copy(activeActivity = created)
        return created
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
        val activity = _uiState.value.selectedDateActivities.find { it.id == activityId } ?: return
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
            trackRepo?.releaseClaimedMilestones(activity.id)
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
        val activity = state.selectedDateActivities.find {
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
                    currentState.selectedDateActivities.find {
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

        // the fresh activity keeps the track, milestone and pending checks the user had
        viewModelScope.launch {
            activityRepo.delete(activity)
            val fresh = createInProgressActivity(
                habitId, dayBoundary.today(),
                trackId = activity.trackId, milestoneId = activity.milestoneId
            ) ?: return@launch
            reclaimMilestones(state.checkedMilestones, fresh.id)
        }
    }

    private suspend fun reclaimMilestones(milestones: List<Milestone>, activityId: Long) {
        val repo = trackRepo ?: return
        milestones.forEach { repo.checkMilestone(it.id, activityId, completed = false) }
        _uiState.value = _uiState.value.copy(
            checkedMilestones = milestones.map { it.copy(activityId = activityId) }
        )
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
            completeCheckedMilestones(activity?.takeIf { it.habitId == habitId }?.id)
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

            completeCheckedMilestones(activity?.takeIf { it.habitId == habitId }?.id)

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
            val activity = state.selectedDateActivities.find {
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
            val trackNames = activities.mapNotNull { it.trackId }.distinct()
                .associateWith { repo.getById(it)?.name }
            val milestoneNames = activities.mapNotNull { it.milestoneId }.distinct()
                .associateWith { repo.getMilestoneById(it)?.name }
            val doneByActivity = repo.claimedMilestonesByAny(activities.map { it.id })
                .groupBy { it.activityId }
            val items = activities.map { activity ->
                // an activity that checked nothing off still shows the milestone it was on
                val done = doneByActivity[activity.id]?.map { it.name }
                    ?: listOfNotNull(activity.milestoneId?.let { milestoneNames[it] })
                TrackHistoryItem(
                    activityId = activity.id,
                    completedAt = activity.completedAt!!,
                    trackName = activity.trackId?.let { trackNames[it] },
                    milestoneNames = done,
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
        viewModelScope.launch {
            loadAndSetTracks(habitId)
            refreshEditableActivity()
        }
    }

    // the editor may have deleted the track or milestone the cached activity points at, or
    // given its track a series it did not have before
    private suspend fun refreshEditableActivity() {
        val cached = currentEditableActivity() ?: return
        val current = activityRepo.getById(cached.id) ?: return
        if (_uiState.value.activeActivity?.id == current.id) {
            _uiState.value = _uiState.value.copy(activeActivity = current)
        }
        hydrateTrackStateForActivity(current)
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
                checkedMilestones = emptyList()
            )
            return
        }
        val track = repo.getById(trackId)
        val checked = repo.claimedMilestones(activity.id)
        val incomplete = repo.incompleteMilestones(trackId)
        val milestone = currentMilestone(activity, checked, incomplete, repo)
        _uiState.value = _uiState.value.copy(
            selectedTrack = track,
            selectedMilestone = milestone,
            incompleteMilestones = incomplete,
            checkedMilestones = checked
        )
    }

    // the unchecked row: the milestone the activity points at unless that is already checked
    // off, otherwise (only while in progress) the next open one after the last check
    private suspend fun currentMilestone(
        activity: Activity,
        checked: List<Milestone>,
        incomplete: List<Milestone>,
        repo: TrackRepository
    ): Milestone? {
        val explicit = activity.milestoneId?.let { repo.getMilestoneById(it) }
        if (explicit != null && checked.none { it.id == explicit.id }) return explicit
        if (activity.completedAt != null) return null
        if (explicit == null && checked.isEmpty()) return assignDefaultMilestone(activity, repo)
        return nextOpenMilestone(incomplete, checked)
    }

    private fun nextOpenMilestone(incomplete: List<Milestone>, checked: List<Milestone>): Milestone? {
        val open = incomplete.filter { m -> checked.none { it.id == m.id } }
        val last = checked.lastOrNull() ?: return open.firstOrNull()
        return open.firstOrNull { it.sortOrder > last.sortOrder } ?: open.firstOrNull()
    }

    // an in-progress activity whose track gained a series after the track was chosen picks up
    // the first milestone, as it would have had the series existed when the track was selected
    private suspend fun assignDefaultMilestone(activity: Activity, repo: TrackRepository): Milestone? {
        if (activity.completedAt != null) return null
        val trackId = activity.trackId ?: return null
        val milestone = repo.defaultMilestone(trackId) ?: return null
        val updated = activity.copy(milestoneId = milestone.id)
        activityRepo.update(updated)
        val current = _uiState.value
        if (current.activeActivity?.id == updated.id) {
            _uiState.value = current.copy(activeActivity = updated)
        }
        return milestone
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

    // the activity the detail surface is currently editing: the selected completed activity when
    // one is open (e.g. a back-fill or any tapped completed row), otherwise the live in-progress
    // one. the selected activity must win — an in-progress activity for today can linger while a
    // completed activity's detail is open, and edits belong to the activity on screen
    private fun currentEditableActivity(): Activity? {
        val s = _uiState.value
        return s.selectedActivityId?.let { id -> s.selectedDateActivities.find { it.id == id } }
            ?: s.activeActivity
    }

    fun selectTrack(trackId: String?) {
        val repo = trackRepo ?: return
        viewModelScope.launch {
            val activity = currentEditableActivity() ?: return@launch
            val track = trackId?.let { repo.getById(it) }
            val milestone = track?.let { repo.defaultMilestone(it.id) }
            val incomplete = track?.let { repo.incompleteMilestones(it.id) } ?: emptyList()

            val updated = activity.copy(trackId = trackId, milestoneId = milestone?.id)
            activityRepo.update(updated)
            repo.releaseClaimedMilestones(activity.id)

            val current = _uiState.value
            _uiState.value = current.copy(
                activeActivity = if (current.activeActivity?.id == updated.id) updated
                    else current.activeActivity,
                selectedTrack = track,
                selectedMilestone = milestone,
                incompleteMilestones = incomplete,
                checkedMilestones = emptyList()
            )
        }
    }

    fun selectMilestone(milestoneId: Long) {
        val repo = trackRepo ?: return
        viewModelScope.launch {
            val activity = currentEditableActivity() ?: return@launch
            val milestone = repo.getMilestoneById(milestoneId) ?: return@launch
            val updated = activity.copy(milestoneId = milestoneId)
            activityRepo.update(updated)

            val current = _uiState.value
            _uiState.value = current.copy(
                activeActivity = if (current.activeActivity?.id == updated.id) updated
                    else current.activeActivity,
                selectedMilestone = milestone
            )
        }
    }

    fun toggleMilestoneChecked(milestoneId: Long) {
        val repo = trackRepo ?: return
        viewModelScope.launch {
            val activity = currentEditableActivity() ?: return@launch
            if (_uiState.value.checkedMilestones.any { it.id == milestoneId }) {
                uncheckMilestone(milestoneId, activity, repo)
            } else {
                checkMilestone(milestoneId, activity, repo)
            }
        }
    }

    private suspend fun checkMilestone(milestoneId: Long, activity: Activity, repo: TrackRepository) {
        val shown = _uiState.value
        val milestone = shown.incompleteMilestones.find { it.id == milestoneId }
            ?: shown.selectedMilestone?.takeIf { it.id == milestoneId }
            ?: repo.getMilestoneById(milestoneId) ?: return
        // a finished activity is being corrected after the fact, so the milestone completes
        // now; an in-progress one waits for the finish
        val done = activity.completedAt != null
        repo.checkMilestone(milestoneId, activity.id, completed = done)
        val state = _uiState.value
        val checked = (state.checkedMilestones + milestone.copy(
            activityId = activity.id, completed = done || milestone.completed
        )).sortedBy { it.sortOrder }
        _uiState.value = state.copy(
            checkedMilestones = checked,
            selectedMilestone = if (done) null else nextOpenMilestone(state.incompleteMilestones, checked)
        )
    }

    // the milestone becomes the one the activity is on again
    private suspend fun uncheckMilestone(milestoneId: Long, activity: Activity, repo: TrackRepository) {
        repo.uncheckMilestone(milestoneId)
        val updated = activity.copy(milestoneId = milestoneId)
        activityRepo.update(updated)
        val state = _uiState.value
        val milestone = state.checkedMilestones.first { it.id == milestoneId }
            .copy(activityId = null, completed = false)
        val incomplete = if (state.incompleteMilestones.any { it.id == milestoneId }) {
            state.incompleteMilestones
        } else {
            (state.incompleteMilestones + milestone).sortedBy { it.sortOrder }
        }
        _uiState.value = state.copy(
            activeActivity = if (state.activeActivity?.id == updated.id) updated
                else state.activeActivity,
            checkedMilestones = state.checkedMilestones.filter { it.id != milestoneId },
            incompleteMilestones = incomplete,
            selectedMilestone = milestone
        )
    }

    private suspend fun completeCheckedMilestones(activityId: Long?) {
        val repo = trackRepo ?: return
        if (activityId == null) return
        repo.completeClaimedMilestones(activityId)
        _uiState.value = _uiState.value.copy(checkedMilestones = emptyList())
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

        selectHabit(habitId)
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
        val state = _uiState.value
        _uiState.value = state.copy(
            intervalChimeState = if (state.intervalChimeMs > 0) IntervalChimeState.RUNNING
                else IntervalChimeState.IDLE
        )
    }

    fun startIntervalChime(intervalMs: Long) {
        val state = _uiState.value

        if (!state.timerRunning) {
            startTimer()
        }

        val elapsed = _uiState.value.activeActivity?.elapsedMs ?: 0
        nextIntervalChimeAtMs = elapsed + intervalMs

        _uiState.value = _uiState.value.copy(
            intervalChimeState = IntervalChimeState.RUNNING,
            intervalChimeMs = intervalMs,
            intervalCountdownMs = intervalMs
        )
    }

    fun changeIntervalChime(intervalMs: Long) {
        val state = _uiState.value
        if (state.intervalChimeMs <= 0) {
            startIntervalChime(intervalMs)
            return
        }
        val elapsed = state.activeActivity?.elapsedMs ?: 0
        nextIntervalChimeAtMs = IntervalReschedule.of(
            nextAtMs = nextIntervalChimeAtMs,
            oldMs = state.intervalChimeMs,
            newMs = intervalMs,
            elapsedMs = elapsed
        ).nextAtMs
        _uiState.value = state.copy(
            intervalChimeState = IntervalChimeState.RUNNING,
            intervalChimeMs = intervalMs,
            intervalCountdownMs = nextIntervalChimeAtMs - elapsed
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
        if (state.historyIndex > 0) showHistoryActivity(state.historyIndex - 1)
    }

    fun historyNewer() {
        val state = _uiState.value
        if (state.historyIndex < state.historyActivities.lastIndex) {
            showHistoryActivity(state.historyIndex + 1)
        }
    }

    private fun showHistoryActivity(index: Int) {
        val state = _uiState.value
        val activity = state.historyActivities.getOrNull(index)
        _uiState.value = state.copy(
            historyIndex = index,
            selectedActivityId = activity?.let { if (it.completedAt != null) it.id else null },
            activeActivity = activity?.let {
                if (it.completedAt == null) it else state.activeActivity
            } ?: state.activeActivity
        )
        if (activity != null) {
            viewModelScope.launch { hydrateTrackStateForActivity(activity) }
        }
    }

    fun refreshToday() {
        val current = dayBoundary.today()
        if (current != realToday.value) {
            realToday.value = current
            sweepStalePlaceholders()
            if (selectedDate.value !in current.minusDays(WINDOW_DAYS)..current) {
                clearViewSelection()
                selectedDate.value = current
            }
        }
    }

    private fun sweepStalePlaceholders() {
        viewModelScope.launch { activityRepo.deleteStalePlaceholders(dayBoundary.today()) }
    }

    fun stepDate(deltaDays: Int) {
        val today = realToday.value
        val candidate = selectedDate.value.plusDays(deltaDays.toLong())
        if (candidate in today.minusDays(WINDOW_DAYS)..today) {
            clearViewSelection()
            selectedDate.value = candidate
        }
    }

    fun goToToday() {
        val running = _uiState.value.timerRunning
        val timedId = _uiState.value.timedHabitId
        clearViewSelection()
        selectedDate.value = realToday.value
        if (running && timedId != null) resumeRunningTimerView(timedId)
    }

    private fun resumeRunningTimerView(timedHabitId: String) {
        _uiState.value = _uiState.value.copy(selectedHabitId = timedHabitId)
        startTimerTick()
    }

    private fun clearViewSelection() {
        val s = _uiState.value
        _uiState.value = s.copy(
            selectedHabitId = null,
            selectedActivityId = null,
            layout = if (s.layout == Layout.ACTIVITY_FOCUSED) Layout.REVIEW else s.layout,
            historyActivities = emptyList(),
            historyIndex = -1,
            historyAnchorIndex = -1,
            trackHistoryVisible = false,
            trackHistory = emptyList()
        )
    }

    fun backFill(habitId: String) {
        val date = selectedDate.value
        viewModelScope.launch {
            val activity = Activity(
                habitId = habitId,
                attributedDate = date,
                startTime = null,
                note = "",
                completedAt = dayBoundary.lastInstantOf(date),
                trackId = null,
                milestoneId = null,
                skipped = false
            )
            val id = activityRepo.create(activity)
            selectBackFilledActivity(activity.copy(id = id))
        }
    }

    private suspend fun selectBackFilledActivity(activity: Activity) {
        val s = _uiState.value
        val activities =
            if (s.selectedDateActivities.any { it.id == activity.id }) s.selectedDateActivities
            else s.selectedDateActivities + activity
        _uiState.value = s.copy(
            selectedDateActivities = activities,
            selectedActivityId = activity.id,
            selectedHabitId = activity.habitId
        )
        loadHistory(activity.habitId, activity.id)
        loadAndSetTracks(activity.habitId)
        hydrateTrackStateForActivity(null)
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
        showHistoryActivity(_uiState.value.historyAnchorIndex)
    }

    companion object {
        const val WINDOW_DAYS = 7L
    }
}
