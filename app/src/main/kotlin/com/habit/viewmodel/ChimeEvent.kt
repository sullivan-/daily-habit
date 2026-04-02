package com.habit.viewmodel

sealed class ChimeEvent {
    data object Interval : ChimeEvent()
    data object Threshold : ChimeEvent()
}
