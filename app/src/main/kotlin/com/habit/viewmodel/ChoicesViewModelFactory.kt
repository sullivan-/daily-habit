package com.habit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.habit.AppContainer

class ChoicesViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ChoicesViewModel(
            container.tallyRepo,
            container.choiceRepo,
            container.dayBoundary
        ) as T
    }
}
