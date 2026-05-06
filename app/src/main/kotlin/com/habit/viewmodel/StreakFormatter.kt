package com.habit.viewmodel

import java.time.Duration
import java.time.Instant
import java.time.Period
import java.time.ZoneId

fun formatStreakDuration(start: Instant, now: Instant): String? =
    formatDurationLabel(start, now, "streak")

fun formatLapseDuration(start: Instant, now: Instant): String? =
    formatDurationLabel(start, now, "lapse")

private fun formatDurationLabel(start: Instant, now: Instant, noun: String): String? {
    val duration = Duration.between(start, now)
    val totalHours = duration.toHours()
    if (totalHours < 1) return null

    val totalDays = duration.toDays()
    if (totalDays < 1) return "$totalHours hour $noun"

    val startDate = start.atZone(ZoneId.systemDefault()).toLocalDate()
    val nowDate = now.atZone(ZoneId.systemDefault()).toLocalDate()
    val period = Period.between(startDate, nowDate)

    val years = period.years
    val months = period.years * 12 + period.months

    return when {
        totalDays < 60 -> "$totalDays day $noun"
        months >= 12 -> "$years year $noun"
        months >= 2 -> "$months month $noun"
        else -> "$totalDays day $noun"
    }
}
