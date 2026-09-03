package com.habit.viewmodel

import java.time.Instant

data class TrackHistoryItem(
    val activityId: Long,
    val completedAt: Instant,
    val trackName: String?,
    val milestoneNames: List<String>,
    val note: String
)
