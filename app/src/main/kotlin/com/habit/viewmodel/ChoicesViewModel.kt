package com.habit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habit.data.Choice
import com.habit.data.ChoiceRepository
import com.habit.data.DayBoundary
import com.habit.data.Tally
import com.habit.data.TallyRepository
import com.habit.data.priorityToScore
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ChoicesViewModel(
    private val tallyRepo: TallyRepository,
    private val choiceRepo: ChoiceRepository,
    private val dayBoundary: DayBoundary
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChoicesUiState())
    val uiState: StateFlow<ChoicesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            tallyRepo.allTallies().collect { tallies ->
                refreshDisplay(tallies)
            }
        }
    }

    fun recordChoice(tallyId: String, abstained: Boolean) {
        viewModelScope.launch {
            choiceRepo.record(
                Choice(
                    tallyId = tallyId,
                    timestamp = Instant.now(),
                    abstained = abstained
                )
            )
            refreshDisplay(tallyRepo.allTallies().first())
        }
    }

    private suspend fun refreshDisplay(tallies: List<Tally>) {
        val sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS)
        val weeklyCounts = choiceRepo.choiceCountsSince(sevenDaysAgo)
            .associateBy { it.tallyId }
        val maxWeeklyCount = weeklyCounts.values.maxOfOrNull { it.count } ?: 0

        val today = dayBoundary.today()
        val zone = ZoneId.systemDefault()
        val dayStart = today.atStartOfDay(zone).toInstant()
        val dayEnd = today.plusDays(1).atStartOfDay(zone).toInstant()

        val items = tallies.map { tally ->
            val recent = choiceRepo.recentChoices(tally.id, 10)
            val todayChoices = choiceRepo.choicesToday(tally.id, dayStart, dayEnd)

            val useDaily = todayChoices.size > 10
            val displayChoices = if (useDaily) todayChoices else recent
            val abstainCount = displayChoices.count { it.abstained }
            val totalCount = displayChoices.size

            val priorityScore = priorityToScore(tally.priority)
            val weeklyCount = weeklyCounts[tally.id]?.count ?: 0
            val recencyScore = if (maxWeeklyCount > 0) {
                weeklyCount.toFloat() / maxWeeklyCount
            } else 0f

            TallyDisplayItem(
                tally = tally,
                abstainCount = abstainCount,
                totalCount = totalCount,
                ratio = if (totalCount > 0) abstainCount.toFloat() / totalCount else 1f,
                sortScore = priorityScore + recencyScore
            )
        }.sortedByDescending { it.sortScore }

        val weeklyTotal = choiceRepo.totalCountSince(sevenDaysAgo)
        val weeklyAbstain = choiceRepo.abstainCountSince(sevenDaysAgo)

        _uiState.value = ChoicesUiState(
            tallies = items,
            weeklyAbstainCount = weeklyAbstain,
            weeklyTotalCount = weeklyTotal
        )
    }
}
