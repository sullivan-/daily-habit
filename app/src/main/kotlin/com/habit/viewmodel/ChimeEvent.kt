package com.habit.viewmodel

sealed class ChimeEvent {
    data object Goal : ChimeEvent()
    data object Stop : ChimeEvent()
}
