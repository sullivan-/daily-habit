package com.habit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.habit.AppContainer

class TallyDetailsViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TallyDetailsViewModel(
            container.tallyRepo,
            container.choiceRepo,
            container.dayBoundary,
            StreakCalculator(container.choiceRepo)
        ) as T
    }
}
