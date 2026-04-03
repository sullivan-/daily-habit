package com.habit.viewmodel

import com.habit.data.Activity
import com.habit.data.Habit

data class AgendaUiState(
    val layout: Layout = Layout.MAIN,
    val habits: List<Habit> = emptyList(),
    val todayActivities: List<Activity> = emptyList(),
    val selectedHabitId: String? = null,
    val selectedActivityId: Long? = null,
    val activeActivity: Activity? = null,
    val timerRunning: Boolean = false,
    val timerTickMs: Long = 0,
    val timedHabitId: String? = null,
    val previousLayout: Layout = Layout.MAIN,
    val historyActivities: List<Activity> = emptyList(),
    val historyIndex: Int = -1,
    val historyAnchorIndex: Int = -1
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
        get() {
            val today = java.time.LocalDate.now()
            return sortAgenda(habits, todayActivities, today)
        }

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
            .filter { java.time.LocalDate.now().dayOfWeek in it.daysActive }
            .sumOf { it.dailyTarget }

    val selectedHabit: Habit?
        get() = selectedHabitId?.let { id -> habits.find { it.id == id } }
}
