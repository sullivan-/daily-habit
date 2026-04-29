package com.habit.viewmodel

import java.time.Duration
import java.time.Instant
import java.time.Period
import java.time.ZoneId

fun formatStreakDuration(start: Instant, now: Instant): String? {
    val duration = Duration.between(start, now)
    val totalDays = duration.toDays()
    if (totalDays < 1) return null

    val startDate = start.atZone(ZoneId.systemDefault()).toLocalDate()
    val nowDate = now.atZone(ZoneId.systemDefault()).toLocalDate()
    val period = Period.between(startDate, nowDate)

    val years = period.years
    val months = period.years * 12 + period.months

    return when {
        totalDays < 60 -> "$totalDays day streak"
        months >= 12 -> "$years year streak"
        months >= 2 -> "$months month streak"
        else -> "$totalDays day streak"
    }
}
