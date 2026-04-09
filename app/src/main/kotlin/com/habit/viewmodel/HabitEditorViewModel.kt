package com.habit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habit.data.Habit
import com.habit.data.HabitRepository
import com.habit.data.Milestone
import com.habit.data.Priority
import com.habit.data.TargetMode
import com.habit.data.Track
import com.habit.data.TrackRepository
import java.time.DayOfWeek
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class HabitEditorState(
    val id: String = "",
    val name: String = "",
    val timesOfDay: List<Int> = listOf(8),
    val sortOrder: Int = 1,
    val daysActive: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val dailyTarget: Int = 1,
    val dailyTargetMode: TargetMode = TargetMode.AT_LEAST,
    val timed: Boolean = false,
    val goalMinutes: Int? = null,
    val stopMinutes: Int? = null,
    val priority: Priority = Priority.MEDIUM,
    val tracks: List<TrackEditorItem> = emptyList(),
    val isNew: Boolean = true,
    val dirty: Boolean = false,
    val saved: Boolean = false,
    val deleted: Boolean = false
) {
    val isValid: Boolean
        get() = name.isNotBlank() &&
            timesOfDay.isNotEmpty() &&
            daysActive.isNotEmpty() &&
            dailyTarget >= 1
}

class HabitEditorViewModel(
    private val habitRepo: HabitRepository,
    private val trackRepo: TrackRepository? = null
) : ViewModel() {

    private val _state = MutableStateFlow(HabitEditorState())
    val state: StateFlow<HabitEditorState> = _state.asStateFlow()

    fun loadHabit(habitId: String) {
        viewModelScope.launch {
            val habit = habitRepo.getById(habitId) ?: return@launch
            val tracks = trackRepo
                ?.tracksForHabit(habitId)?.first()
                ?: emptyList()
            val trackItems = tracks.map { track ->
                val milestones = trackRepo?.milestonesForTrack(track.id) ?: emptyList()
                val canDelete = trackRepo?.canDelete(track.id) ?: true
                TrackEditorItem(
                    id = track.id,
                    name = track.name,
                    priority = track.priority,
                    dayOfWeek = track.dayOfWeek,
                    archived = track.archived,
                    milestones = milestones,
                    canDelete = canDelete
                )
            }
            _state.value = HabitEditorState(
                id = habit.id,
                name = habit.name,
                timesOfDay = habit.timesOfDay,
                sortOrder = habit.sortOrder,
                daysActive = habit.daysActive,
                dailyTarget = habit.dailyTarget,
                dailyTargetMode = habit.dailyTargetMode,
                timed = habit.timed,
                goalMinutes = habit.goalMinutes,
                stopMinutes = habit.stopMinutes,
                priority = habit.priority,
                tracks = trackItems,
                isNew = false,
                dirty = false
            )
        }
    }

    fun setName(name: String) {
        _state.value = _state.value.copy(name = name, dirty = true)
    }

    fun addTimeOfDay(hour: Int) {
        val current = _state.value.timesOfDay
        if (hour !in current) {
            _state.value = _state.value.copy(
                timesOfDay = (current + hour).sorted(),
                dirty = true
            )
        }
    }

    fun removeTimeOfDay(hour: Int) {
        val current = _state.value.timesOfDay
        if (current.size > 1) {
            _state.value = _state.value.copy(
                timesOfDay = current - hour,
                dirty = true
            )
        }
    }

    fun setSortOrder(order: Int) {
        _state.value = _state.value.copy(sortOrder = order, dirty = true)
    }

    fun toggleDayActive(day: DayOfWeek) {
        val current = _state.value.daysActive
        val updated = if (day in current && current.size > 1) {
            current - day
        } else {
            current + day
        }
        _state.value = _state.value.copy(daysActive = updated, dirty = true)
    }

    fun setDailyTarget(target: Int) {
        if (target >= 1) {
            _state.value = _state.value.copy(dailyTarget = target, dirty = true)
        }
    }

    fun setDailyTargetMode(mode: TargetMode) {
        _state.value = _state.value.copy(dailyTargetMode = mode, dirty = true)
    }

    fun setTimed(timed: Boolean) {
        _state.value = _state.value.copy(
            timed = timed,
            goalMinutes = if (!timed) null else _state.value.goalMinutes,
            stopMinutes = if (!timed) null else _state.value.stopMinutes,
            dirty = true
        )
    }

    fun setGoalMinutes(minutes: Int?) {
        _state.value = _state.value.copy(goalMinutes = minutes, dirty = true)
    }

    fun setStopMinutes(minutes: Int?) {
        _state.value = _state.value.copy(stopMinutes = minutes, dirty = true)
    }

    fun setPriority(priority: Priority) {
        _state.value = _state.value.copy(priority = priority, dirty = true)
    }

    // --- track management ---

    fun addTrack() {
        val items = _state.value.tracks
        val newItem = TrackEditorItem(
            id = "", name = "", priority = Priority.MEDIUM,
            dayOfWeek = null, archived = false, milestones = emptyList(),
            canDelete = true, isNew = true, expanded = true
        )
        _state.value = _state.value.copy(tracks = items + newItem, dirty = true)
    }

    fun updateTrackName(index: Int, name: String) {
        val items = _state.value.tracks.toMutableList()
        items[index] = items[index].copy(name = name)
        _state.value = _state.value.copy(tracks = items, dirty = true)
    }

    fun updateTrackPriority(index: Int, priority: Priority) {
        val items = _state.value.tracks.toMutableList()
        items[index] = items[index].copy(priority = priority)
        _state.value = _state.value.copy(tracks = items, dirty = true)
    }

    fun updateTrackDayOfWeek(index: Int, dayOfWeek: DayOfWeek?) {
        val items = _state.value.tracks.toMutableList()
        items[index] = items[index].copy(dayOfWeek = dayOfWeek)
        _state.value = _state.value.copy(tracks = items, dirty = true)
    }

    fun toggleTrackExpanded(index: Int) {
        val items = _state.value.tracks.toMutableList()
        items[index] = items[index].copy(expanded = !items[index].expanded)
        _state.value = _state.value.copy(tracks = items)
    }

    fun archiveTrack(index: Int) {
        val items = _state.value.tracks.toMutableList()
        items[index] = items[index].copy(archived = true, expanded = false)
        _state.value = _state.value.copy(tracks = items, dirty = true)
    }

    fun unarchiveTrack(index: Int) {
        val items = _state.value.tracks.toMutableList()
        items[index] = items[index].copy(archived = false)
        _state.value = _state.value.copy(tracks = items, dirty = true)
    }

    fun deleteTrack(index: Int) {
        val items = _state.value.tracks.toMutableList()
        items.removeAt(index)
        _state.value = _state.value.copy(tracks = items, dirty = true)
    }

    fun addMilestone(trackIndex: Int, name: String) {
        val items = _state.value.tracks.toMutableList()
        val track = items[trackIndex]
        val nextOrder = (track.milestones.maxOfOrNull { it.sortOrder } ?: 0) + 1
        val milestone = Milestone(
            trackId = track.id, name = name,
            sortOrder = nextOrder, completed = false
        )
        items[trackIndex] = track.copy(milestones = track.milestones + milestone)
        _state.value = _state.value.copy(tracks = items, dirty = true)
    }

    fun deleteMilestone(trackIndex: Int, milestoneIndex: Int) {
        val items = _state.value.tracks.toMutableList()
        val track = items[trackIndex]
        val milestones = track.milestones.toMutableList()
        milestones.removeAt(milestoneIndex)
        items[trackIndex] = track.copy(milestones = milestones)
        _state.value = _state.value.copy(tracks = items, dirty = true)
    }

    // --- save and delete ---

    fun save() {
        val s = _state.value
        if (!s.isValid) return

        viewModelScope.launch {
            val id = if (s.isNew) generateId(s.name) else s.id
            val habit = Habit(
                id = id,
                name = s.name,
                timesOfDay = s.timesOfDay,
                sortOrder = s.sortOrder,
                daysActive = s.daysActive,
                dailyTarget = s.dailyTarget,
                dailyTargetMode = s.dailyTargetMode,
                timed = s.timed,
                goalMinutes = s.goalMinutes,
                stopMinutes = s.stopMinutes,
                priority = s.priority
            )
            if (s.isNew) {
                habitRepo.insert(habit)
            } else {
                habitRepo.update(habit)
            }

            trackRepo?.let { repo ->
                for (trackItem in s.tracks) {
                    val trackId = if (trackItem.isNew) {
                        val generated = trackItem.name.lowercase()
                            .replace(Regex("[^a-z0-9]+"), "-").trim('-')
                            .ifEmpty { UUID.randomUUID().toString() }
                        val track = Track(
                            id = generated,
                            habitId = id,
                            name = trackItem.name,
                            priority = trackItem.priority,
                            dayOfWeek = trackItem.dayOfWeek,
                            archived = trackItem.archived
                        )
                        repo.insert(track)
                        generated
                    } else {
                        repo.update(Track(
                            id = trackItem.id,
                            habitId = id,
                            name = trackItem.name,
                            priority = trackItem.priority,
                            dayOfWeek = trackItem.dayOfWeek,
                            archived = trackItem.archived
                        ))
                        trackItem.id
                    }

                    for (milestone in trackItem.milestones) {
                        if (milestone.id == 0L) {
                            repo.insertMilestone(milestone.copy(trackId = trackId))
                        } else {
                            repo.updateMilestone(milestone)
                        }
                    }
                }
            }

            _state.value = s.copy(saved = true, dirty = false)
        }
    }

    fun delete() {
        val s = _state.value
        if (s.isNew) return
        viewModelScope.launch {
            habitRepo.deleteById(s.id)
            _state.value = s.copy(deleted = true)
        }
    }

    private suspend fun generateId(name: String): String {
        val base = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        val existing = habitRepo.allIds().toSet()
        if (base !in existing) return base
        var i = 2
        while ("$base-$i" in existing) i++
        return "$base-$i"
    }
}
