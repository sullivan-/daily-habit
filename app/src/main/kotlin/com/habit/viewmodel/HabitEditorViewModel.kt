package com.habit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habit.data.Habit
import com.habit.data.HabitRepository
import com.habit.data.Priority
import com.habit.data.TargetMode
import com.habit.data.ThresholdType
import java.time.DayOfWeek
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val thresholdMinutes: Int? = null,
    val thresholdType: ThresholdType? = null,
    val priority: Priority = Priority.MEDIUM,
    val dailyTexts: Map<DayOfWeek, String> = emptyMap(),
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
    private val habitRepo: HabitRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HabitEditorState())
    val state: StateFlow<HabitEditorState> = _state.asStateFlow()

    fun loadHabit(habitId: String) {
        viewModelScope.launch {
            val habit = habitRepo.getById(habitId) ?: return@launch
            _state.value = HabitEditorState(
                id = habit.id,
                name = habit.name,
                timesOfDay = habit.timesOfDay,
                sortOrder = habit.sortOrder,
                daysActive = habit.daysActive,
                dailyTarget = habit.dailyTarget,
                dailyTargetMode = habit.dailyTargetMode,
                timed = habit.timed,
                thresholdMinutes = habit.thresholdMinutes,
                thresholdType = habit.thresholdType,
                priority = habit.priority,
                dailyTexts = habit.dailyTexts,
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
            thresholdMinutes = if (!timed) null else _state.value.thresholdMinutes,
            thresholdType = if (!timed) null else _state.value.thresholdType,
            dirty = true
        )
    }

    fun setThresholdMinutes(minutes: Int?) {
        _state.value = _state.value.copy(
            thresholdMinutes = minutes,
            thresholdType = if (minutes == null) null else
                _state.value.thresholdType ?: ThresholdType.GOAL,
            dirty = true
        )
    }

    fun setThresholdType(type: ThresholdType?) {
        _state.value = _state.value.copy(thresholdType = type, dirty = true)
    }

    fun setPriority(priority: Priority) {
        _state.value = _state.value.copy(priority = priority, dirty = true)
    }

    fun setDailyText(day: DayOfWeek, text: String) {
        val current = _state.value.dailyTexts.toMutableMap()
        if (text.isBlank()) current.remove(day) else current[day] = text
        _state.value = _state.value.copy(dailyTexts = current, dirty = true)
    }

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
                thresholdMinutes = s.thresholdMinutes,
                thresholdType = s.thresholdType,
                priority = s.priority,
                dailyTexts = s.dailyTexts
            )
            if (s.isNew) {
                habitRepo.insert(habit)
            } else {
                habitRepo.update(habit)
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
