package com.habit.viewmodel

import com.habit.data.Milestone
import com.habit.data.Priority
import java.time.DayOfWeek

data class TrackEditorItem(
    val id: String,
    val name: String,
    val priority: Priority,
    val dayOfWeek: DayOfWeek?,
    val archived: Boolean,
    val milestones: List<Milestone>,
    val canDelete: Boolean = false,
    val isNew: Boolean = false,
    val expanded: Boolean = false
)
