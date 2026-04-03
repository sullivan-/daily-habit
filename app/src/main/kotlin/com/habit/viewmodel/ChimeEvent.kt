package com.habit.viewmodel

sealed class ChimeEvent {
    data object Threshold : ChimeEvent()
}
