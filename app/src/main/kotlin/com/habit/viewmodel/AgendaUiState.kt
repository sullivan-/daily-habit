package com.habit.viewmodel

import com.habit.data.Activity
import com.habit.data.EasyDayLevel
import com.habit.data.Habit
import com.habit.data.Milestone
import com.habit.data.TargetMode
import com.habit.data.Track
import java.time.LocalDate

data class AgendaUiState(
    val layout: Layout = Layout.MAIN,
    val habits: List<Habit> = emptyList(),
    val selectedDateActivities: List<Activity> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val today: LocalDate = LocalDate.now(),
    val selectedHabitId: String? = null,
    val selectedActivityId: Long? = null,
    val activeActivity: Activity? = null,
    val timerRunning: Boolean = false,
    val timerTickMs: Long = 0,
    val timedHabitId: String? = null,
    val previousLayout: Layout = Layout.MAIN,
    val historyActivities: List<Activity> = emptyList(),
    val historyIndex: Int = -1,
    val historyAnchorIndex: Int = -1,
    val availableTracks: List<Track> = emptyList(),
    val selectedTrack: Track? = null,
    val selectedMilestone: Milestone? = null,
    val incompleteMilestones: List<Milestone> = emptyList(),
    val checkedMilestones: List<Milestone> = emptyList(),
    val intervalChimeState: IntervalChimeState = IntervalChimeState.IDLE,
    val intervalChimeMs: Long = 0,
    val intervalCountdownMs: Long = 0,
    val trackHistory: List<TrackHistoryItem> = emptyList(),
    val trackHistoryVisible: Boolean = false,
    val easyDayLevel: EasyDayLevel = EasyDayLevel.OFF,
    val easyDayCarryOver: Boolean = false
) {
    // what the unchecked milestone row can be switched to: open milestones not yet checked here
    val milestoneChoices: List<Milestone>
        get() = incompleteMilestones.filter { m -> checkedMilestones.none { it.id == m.id } }

    val browsingHistory: Boolean
        get() = historyIndex >= 0 && historyActivities.isNotEmpty()

    val historyActivity: Activity?
        get() = if (browsingHistory) historyActivities.getOrNull(historyIndex) else null

    val isAtOldest: Boolean
        get() = historyIndex <= 0

    val isAtNewest: Boolean
        get() = historyIndex >= historyActivities.lastIndex

    val hasSwipedFromAnchor: Boolean
        get() = historyAnchorIndex >= 0 && historyIndex != historyAnchorIndex

    val isViewingPastDay: Boolean
        get() = selectedDate != today

    // Easy Day is forward-looking only; past days always compute from raw dailyTarget.
    val effectiveEasyDay: EasyDayLevel
        get() = if (isViewingPastDay) EasyDayLevel.OFF else easyDayLevel

    val agendaItems: List<AgendaItem>
        get() = sortAgenda(
            habits,
            selectedDateActivities,
            selectedDate,
            easyDayLevel = effectiveEasyDay
        )

    val completedItems: List<CompletedItem>
        get() {
            val habitsById = habits.associateBy { it.id }
            val tracksById = availableTracks.associateBy { it.id }
            return selectedDateActivities
                .filter { it.completedAt != null && !it.skipped }
                .sortedBy { it.completedAt }
                .mapNotNull { activity ->
                    habitsById[activity.habitId]?.let { habit ->
                        CompletedItem(
                            activity = activity,
                            habit = habit,
                            trackName = activity.trackId?.let { id ->
                                tracksById[id]?.name ?: id
                            }
                        )
                    }
                }
        }

    val missedItems: List<MissedItem>
        get() {
            if (!isViewingPastDay) return emptyList()
            val completedCounts = selectedDateActivities
                .filter { it.completedAt != null && !it.skipped }
                .groupBy { it.habitId }
                .mapValues { it.value.size }
            return habits
                .filter { selectedDate.dayOfWeek in it.daysActive }
                .mapNotNull { habit ->
                    val count = completedCounts[habit.id] ?: 0
                    if (count < habit.dailyTarget) {
                        MissedItem(habit, count, habit.dailyTarget)
                    } else {
                        null
                    }
                }
                .sortedWith(
                    compareBy<MissedItem> { it.habit.timesOfDay.firstOrNull() ?: 0 }
                        .thenBy { it.habit.priority.ordinal }
                        .thenByDescending { it.habit.tieBreaker }
                )
        }

    val progressCount: Int
        get() = selectedDateActivities.count { it.completedAt != null && !it.skipped }

    val totalTarget: Int
        get() = habits
            .filter { selectedDate.dayOfWeek in it.daysActive }
            .filter { effectiveEasyDay.includes(it.priority) }
            .sumOf { it.dailyTarget }

    val selectedHabit: Habit?
        get() = selectedHabitId?.let { id -> habits.find { it.id == id } }

    val otherHabits: List<Habit>
        get() {
            val agendaHabitIds = agendaItems.map { it.habit.id }.toSet()
            val completedCounts = selectedDateActivities
                .filter { it.completedAt != null }
                .groupBy { it.habitId }
                .mapValues { it.value.size }
            return habits.filter { habit ->
                habit.id !in agendaHabitIds &&
                    !(habit.dailyTargetMode == TargetMode.EXACTLY &&
                        (completedCounts[habit.id] ?: 0) >= habit.dailyTarget)
            }
        }

    val pastDayOtherHabits: List<Habit>
        get() {
            val missedIds = missedItems.map { it.habit.id }.toSet()
            val completedCounts = selectedDateActivities
                .filter { it.completedAt != null && !it.skipped }
                .groupBy { it.habitId }
                .mapValues { it.value.size }
            return habits.filter { habit ->
                habit.id !in missedIds &&
                    !(habit.dailyTargetMode == TargetMode.EXACTLY &&
                        (completedCounts[habit.id] ?: 0) >= habit.dailyTarget)
            }
        }
}
