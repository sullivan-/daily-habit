package com.habit.viewmodel

import com.habit.data.Tally
import java.time.Instant

data class TallyDisplayItem(
    val tally: Tally,
    val abstainCount: Int,
    val totalCount: Int,
    val ratio: Float,
    val streakStart: Instant? = null,
    val lastYesAt: Instant? = null,
    val lapseStart: Instant? = null
)
