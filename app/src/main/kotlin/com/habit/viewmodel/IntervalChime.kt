package com.habit.viewmodel

enum class IntervalChimeState { IDLE, SELECTING, RUNNING }

object IntervalOptions {
    val seconds = listOf(6, 7, 8, 9, 10, 11, 12)
    val minutes = listOf(3, 4, 5, 8, 10)

    fun labelFor(ms: Long): String {
        val sec = ms / 1000
        return if (sec < 60) "${sec}s" else "${sec / 60}m"
    }

    fun isSecondsInterval(ms: Long): Boolean = ms < 60_000
}
