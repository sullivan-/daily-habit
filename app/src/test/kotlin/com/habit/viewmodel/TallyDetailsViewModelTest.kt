package com.habit.viewmodel

import com.google.common.truth.Truth.assertThat
import com.habit.data.Choice
import com.habit.data.ChoiceRepository
import com.habit.data.DayBoundary
import com.habit.data.Priority
import com.habit.data.Tally
import com.habit.data.TallyRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TallyDetailsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val tallyRepo = mockk<TallyRepository>(relaxed = true)
    private val choiceRepo = mockk<ChoiceRepository>(relaxed = true)
    private val dayBoundary = DayBoundary(2)
    private val streakCalculator = StreakCalculator(choiceRepo)

    private val now = Instant.now()
    private val existingTally = Tally(id = "1", name = "Sweets", priority = Priority.HIGH)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { tallyRepo.getById("1") } returns existingTally
        coEvery { choiceRepo.recentChoices(any(), any()) } returns emptyList()
        coEvery { choiceRepo.choicesToday(any(), any(), any()) } returns emptyList()
        coEvery { choiceRepo.mostRecentChoice(any()) } returns null
        coEvery { choiceRepo.mostRecentIndulgence(any()) } returns null
        coEvery { choiceRepo.firstAbstentionAfter(any(), any()) } returns null
        coEvery { choiceRepo.firstAbstention(any()) } returns null
        coEvery { choiceRepo.earliestChoice(any()) } returns null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() =
        TallyDetailsViewModel(tallyRepo, choiceRepo, dayBoundary, streakCalculator)

    @Test
    fun `loadTally populates all fields including streak and stats`() = runTest {
        val twoDaysAgo = now.minus(2, ChronoUnit.DAYS)
        coEvery { choiceRepo.mostRecentChoice("1") } returns
            Choice(1, "1", now, abstained = true)
        coEvery { choiceRepo.firstAbstention("1") } returns
            Choice(1, "1", twoDaysAgo, abstained = true)
        coEvery { choiceRepo.earliestChoice("1") } returns
            Choice(1, "1", twoDaysAgo, abstained = true)
        coEvery { choiceRepo.recentChoices("1", 10) } returns listOf(
            Choice(1, "1", twoDaysAgo, abstained = true),
            Choice(2, "1", now, abstained = true)
        )

        val vm = createViewModel()
        vm.loadTally("1")

        val state = vm.state.value
        assertThat(state.id).isEqualTo("1")
        assertThat(state.name).isEqualTo("Sweets")
        assertThat(state.priority).isEqualTo(Priority.HIGH)
        assertThat(state.isNew).isFalse()
        assertThat(state.streakStart).isEqualTo(twoDaysAgo)
        assertThat(state.abstainCountLast10).isEqualTo(2)
        assertThat(state.totalCountLast10).isEqualTo(2)
        assertThat(state.hasChoices).isTrue()
    }

    @Test
    fun `recordFirstNo inserts a choice and refreshes stats`() = runTest {
        val vm = createViewModel()
        vm.loadTally("1")

        val date = LocalDate.of(2020, 6, 15)
        vm.recordFirstNo(date)

        coVerify {
            choiceRepo.record(match {
                it.tallyId == "1" && it.abstained
            })
        }
    }

    @Test
    fun `recordFirstNo on a new tally is a no-op`() = runTest {
        val vm = createViewModel()
        vm.recordFirstNo(LocalDate.of(2020, 1, 1))

        coVerify(exactly = 0) { choiceRepo.record(any()) }
    }

    @Test
    fun `streak display updates after recordFirstNo`() = runTest {
        val threeDaysAgo = now.minus(3, ChronoUnit.DAYS)
        coEvery { choiceRepo.mostRecentChoice("1") } returns
            Choice(1, "1", now, abstained = true)
        coEvery { choiceRepo.earliestChoice("1") } returns
            Choice(1, "1", threeDaysAgo, abstained = true)
        coEvery { choiceRepo.firstAbstention("1") } returns
            Choice(1, "1", threeDaysAgo, abstained = true)

        val vm = createViewModel()
        vm.loadTally("1")

        assertThat(vm.state.value.streakStart).isEqualTo(threeDaysAgo)

        // after recording a first no, the streak refreshes
        val longAgo = now.minus(100, ChronoUnit.DAYS)
        coEvery { choiceRepo.firstAbstention("1") } returns
            Choice(0, "1", longAgo, abstained = true)
        coEvery { choiceRepo.earliestChoice("1") } returns
            Choice(0, "1", longAgo, abstained = true)

        vm.recordFirstNo(LocalDate.of(2026, 1, 14))
        assertThat(vm.state.value.streakStart).isEqualTo(longAgo)
    }

    @Test
    fun `today stats hidden when fewer than 3 choices today`() = runTest {
        coEvery { choiceRepo.choicesToday("1", any(), any()) } returns listOf(
            Choice(1, "1", now, abstained = true),
            Choice(2, "1", now.minusSeconds(60), abstained = false)
        )

        val vm = createViewModel()
        vm.loadTally("1")

        assertThat(vm.state.value.showTodayStats).isFalse()
    }

    @Test
    fun `today stats shown when 3+ choices today`() = runTest {
        coEvery { choiceRepo.choicesToday("1", any(), any()) } returns listOf(
            Choice(1, "1", now, abstained = true),
            Choice(2, "1", now.minusSeconds(60), abstained = false),
            Choice(3, "1", now.minusSeconds(120), abstained = true)
        )

        val vm = createViewModel()
        vm.loadTally("1")

        assertThat(vm.state.value.showTodayStats).isTrue()
        assertThat(vm.state.value.abstainCountToday).isEqualTo(2)
        assertThat(vm.state.value.totalCountToday).isEqualTo(3)
    }
}
