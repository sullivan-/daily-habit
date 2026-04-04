package com.habit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habit.data.Priority
import com.habit.data.Tally
import com.habit.data.TallyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TallyEditorViewModel(
    private val tallyRepo: TallyRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TallyEditorState())
    val state: StateFlow<TallyEditorState> = _state.asStateFlow()

    fun loadTally(tallyId: Long) {
        viewModelScope.launch {
            tallyRepo.getById(tallyId)?.let { tally ->
                _state.value = TallyEditorState(
                    id = tally.id,
                    name = tally.name,
                    priority = tally.priority,
                    isNew = false
                )
            }
        }
    }

    fun setName(name: String) {
        _state.value = _state.value.copy(name = name, dirty = true)
    }

    fun setPriority(priority: Priority) {
        _state.value = _state.value.copy(priority = priority, dirty = true)
    }

    fun save() {
        val s = _state.value
        if (!s.isValid) return
        viewModelScope.launch {
            if (s.isNew) {
                tallyRepo.insert(Tally(name = s.name, priority = s.priority))
            } else {
                tallyRepo.update(
                    Tally(id = s.id, name = s.name, priority = s.priority)
                )
            }
            _state.value = s.copy(saved = true, dirty = false)
        }
    }

    fun delete() {
        val s = _state.value
        if (s.isNew) return
        viewModelScope.launch {
            tallyRepo.deleteById(s.id)
            _state.value = s.copy(deleted = true)
        }
    }
}
