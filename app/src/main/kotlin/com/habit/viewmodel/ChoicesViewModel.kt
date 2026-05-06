package com.habit.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.habit.data.Choice
import com.habit.data.ChoiceRepository
import com.habit.data.DayBoundary
import com.habit.data.Tally
import com.habit.data.TallyRepository
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ChoicesViewModel(
    private val tallyRepo: TallyRepository,
    private val choiceRepo: ChoiceRepository,
    private val dayBoundary: DayBoundary,
    private val streakCalculator: StreakCalculator
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChoicesUiState())
    val uiState: StateFlow<ChoicesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            tallyRepo.allTallies()
                .combine(choiceRepo.choiceChanges()) { tallies, _ -> tallies }
                .collect { tallies ->
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
        }
    }

    private fun tallySortBucket(item: TallyDisplayItem): Int = when {
        item.lastYesAt != null -> 0  // broken streak (most recent choice was Yes)
        item.streakStart == null -> 1  // no choices ever
        else -> 2  // active streak
    }

    private fun tallySortTimestamp(item: TallyDisplayItem): Long {
        val timestamp = item.lastYesAt ?: item.streakStart
        // negate so larger timestamps sort earlier within bucket
        return -(timestamp?.toEpochMilli() ?: 0L)
    }

    private suspend fun refreshDisplay(tallies: List<Tally>) {
        val sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS)

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

            val mostRecent = choiceRepo.mostRecentChoice(tally.id)
            val streakStart = streakCalculator.currentStreakStart(tally.id)
            val lastYesAt = if (mostRecent?.abstained == false) mostRecent.timestamp else null

            TallyDisplayItem(
                tally = tally,
                abstainCount = abstainCount,
                totalCount = totalCount,
                ratio = if (totalCount > 0) abstainCount.toFloat() / totalCount else 1f,
                streakStart = streakStart,
                lastYesAt = lastYesAt
            )
        }.sortedWith(compareBy(::tallySortBucket, ::tallySortTimestamp))

        val weeklyTotal = choiceRepo.totalCountSince(sevenDaysAgo)
        val weeklyAbstain = choiceRepo.abstainCountSince(sevenDaysAgo)

        _uiState.value = ChoicesUiState(
            tallies = items,
            weeklyAbstainCount = weeklyAbstain,
            weeklyTotalCount = weeklyTotal
        )
    }
}
