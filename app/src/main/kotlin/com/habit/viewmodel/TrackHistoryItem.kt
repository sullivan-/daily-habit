package com.habit.viewmodel

import java.time.Instant

data class TrackHistoryItem(
    val activityId: Long,
    val completedAt: Instant,
    val trackName: String?,
    val milestoneName: String?,
    val note: String
)
