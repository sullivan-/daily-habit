package com.habit.viewmodel

import com.habit.data.Priority

data class TallyEditorState(
    val id: Long = 0,
    val name: String = "",
    val priority: Priority = Priority.MEDIUM,
    val isNew: Boolean = true,
    val dirty: Boolean = false,
    val saved: Boolean = false,
    val deleted: Boolean = false
) {
    val isValid: Boolean get() = name.isNotBlank()
}
