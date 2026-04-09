package com.habit.viewmodel

import com.habit.data.Activity
import com.habit.data.Habit
import com.habit.data.Milestone
import com.habit.data.TargetMode
import com.habit.data.Track
import java.time.LocalDate

data class AgendaUiState(
    val layout: Layout = Layout.MAIN,
    val habits: List<Habit> = emptyList(),
    val todayActivities: List<Activity> = emptyList(),
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
    val incompleteMilestones: List<Milestone> = emptyList()
) {
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

    val agendaItems: List<AgendaItem>
        get() = sortAgenda(habits, todayActivities, today)

    val completedItems: List<CompletedItem>
        get() {
            val habitsById = habits.associateBy { it.id }
            return todayActivities
                .filter { it.completedAt != null }
                .sortedBy { it.completedAt }
                .mapNotNull { activity ->
                    habitsById[activity.habitId]?.let { habit ->
                        CompletedItem(activity = activity, habit = habit)
                    }
                }
        }

    val progressCount: Int
        get() = todayActivities.count { it.completedAt != null }

    val totalTarget: Int
        get() = habits
            .filter { today.dayOfWeek in it.daysActive }
            .sumOf { it.dailyTarget }

    val selectedHabit: Habit?
        get() = selectedHabitId?.let { id -> habits.find { it.id == id } }

    val otherHabits: List<Habit>
        get() {
            val agendaHabitIds = agendaItems.map { it.habit.id }.toSet()
            val completedCounts = todayActivities
                .filter { it.completedAt != null }
                .groupBy { it.habitId }
                .mapValues { it.value.size }
            return habits.filter { habit ->
                habit.id !in agendaHabitIds &&
                    !(habit.dailyTargetMode == TargetMode.EXACTLY &&
                        (completedCounts[habit.id] ?: 0) >= habit.dailyTarget)
            }
        }
}
