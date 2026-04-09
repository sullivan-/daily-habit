package com.habit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.habit.AppContainer

class AgendaViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AgendaViewModel(
            container.habitRepo,
            container.activityRepo,
            container.dayBoundary,
            container.trackRepo
        ) as T
    }
}
