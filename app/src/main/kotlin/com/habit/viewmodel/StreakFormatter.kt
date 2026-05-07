package com.habit.viewmodel

import java.time.Duration
import java.time.Instant
import java.time.Period
import java.time.ZoneId

fun formatStreakDuration(start: Instant, now: Instant, count: Int): String =
    formatDurationLabel(start, now, "streak", count)

fun formatLapseDuration(start: Instant, now: Instant, count: Int): String =
    formatDurationLabel(start, now, "lapse", count)

private fun formatDurationLabel(
    start: Instant,
    now: Instant,
    noun: String,
    count: Int
): String {
    val duration = Duration.between(start, now)
    val totalHours = duration.toHours()
    val totalDays = duration.toDays()

    val base = when {
        totalHours < 1 -> "0 hour $noun"
        totalDays < 1 -> "$totalHours hour $noun"
        totalDays < 60 -> "$totalDays day $noun"
        else -> {
            val startDate = start.atZone(ZoneId.systemDefault()).toLocalDate()
            val nowDate = now.atZone(ZoneId.systemDefault()).toLocalDate()
            val period = Period.between(startDate, nowDate)
            val months = period.years * 12 + period.months
            when {
                months >= 12 -> "${period.years} year $noun"
                months >= 2 -> "$months month $noun"
                else -> "$totalDays day $noun"
            }
        }
    }
    return "$base ($count)"
}
