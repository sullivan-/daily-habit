package com.habit.viewmodel

import com.habit.data.Tally

data class TallyDisplayItem(
    val tally: Tally,
    val abstainCount: Int,
    val totalCount: Int,
    val ratio: Float,
    val sortScore: Float
)
