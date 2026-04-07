package com.habit.viewmodel

data class ChoicesUiState(
    val tallies: List<TallyDisplayItem> = emptyList(),
    val weeklyAbstainCount: Int = 0,
    val weeklyTotalCount: Int = 0
)
