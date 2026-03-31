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
    val elapsedMs: Long = 0
) {
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
