package com.habit.viewmodel

import com.google.common.truth.Truth.assertThat
import com.habit.data.Choice
import com.habit.data.ChoiceRepository
import com.habit.data.DayBoundary
import com.habit.data.Priority
import com.habit.data.Tally
import com.habit.data.TallyChoiceCount
import com.habit.data.TallyRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChoicesViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val tallyRepo = mockk<TallyRepository>()
    private val choiceRepo = mockk<ChoiceRepository>(relaxed = true)
    private val dayBoundary = DayBoundary(2)

    private val sweets = Tally(id = 1L, name = "Sweets", priority = Priority.HIGH)
    private val nicotine = Tally(id = 2L, name = "Nicotine", priority = Priority.LOW)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { choiceRepo.choiceCountsSince(any()) } returns emptyList()
        coEvery { choiceRepo.recentChoices(any(), any()) } returns emptyList()
        coEvery { choiceRepo.choicesToday(any(), any(), any()) } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(tallies: List<Tally> = listOf(sweets, nicotine)): ChoicesViewModel {
        coEvery { tallyRepo.allTallies() } returns flowOf(tallies)
        return ChoicesViewModel(tallyRepo, choiceRepo, dayBoundary)
    }

    @Test
    fun `initial state loads tallies`() = runTest {
        val vm = createViewModel()
        assertThat(vm.uiState.value.tallies).hasSize(2)
    }

    @Test
    fun `tallies with no choices show zero counts`() = runTest {
        val vm = createViewModel()
        val items = vm.uiState.value.tallies
        items.forEach { item ->
            assertThat(item.totalCount).isEqualTo(0)
            assertThat(item.abstainCount).isEqualTo(0)
        }
    }

    @Test
    fun `ratio is 1 when no choices exist`() = runTest {
        val vm = createViewModel()
        vm.uiState.value.tallies.forEach { item ->
            assertThat(item.ratio).isEqualTo(1f)
        }
    }

    @Test
    fun `recordChoice with abstain inserts choice`() = runTest {
        val vm = createViewModel()
        vm.recordChoice(1L, abstained = true)

        coVerify {
            choiceRepo.record(match { it.tallyId == 1L && it.abstained })
        }
    }

    @Test
    fun `recordChoice with indulge inserts choice`() = runTest {
        val vm = createViewModel()
        vm.recordChoice(1L, abstained = false)

        coVerify {
            choiceRepo.record(match { it.tallyId == 1L && !it.abstained })
        }
    }

    @Test
    fun `indicator shows recent choices`() = runTest {
        val now = Instant.now()
        coEvery { choiceRepo.recentChoices(1L, 10) } returns listOf(
            Choice(1, 1L, now, abstained = true),
            Choice(2, 1L, now.minusSeconds(60), abstained = true),
            Choice(3, 1L, now.minusSeconds(120), abstained = false)
        )

        val vm = createViewModel(listOf(sweets))
        val item = vm.uiState.value.tallies.first()
        assertThat(item.abstainCount).isEqualTo(2)
        assertThat(item.totalCount).isEqualTo(3)
        assertThat(item.ratio).isWithin(0.01f).of(2f / 3f)
    }

    @Test
    fun `ratio is 1 when all abstain`() = runTest {
        val now = Instant.now()
        coEvery { choiceRepo.recentChoices(1L, 10) } returns listOf(
            Choice(1, 1L, now, abstained = true),
            Choice(2, 1L, now.minusSeconds(60), abstained = true)
        )

        val vm = createViewModel(listOf(sweets))
        assertThat(vm.uiState.value.tallies.first().ratio).isEqualTo(1f)
    }

    @Test
    fun `ratio is 0 when all indulge`() = runTest {
        val now = Instant.now()
        coEvery { choiceRepo.recentChoices(1L, 10) } returns listOf(
            Choice(1, 1L, now, abstained = false),
            Choice(2, 1L, now.minusSeconds(60), abstained = false)
        )

        val vm = createViewModel(listOf(sweets))
        assertThat(vm.uiState.value.tallies.first().ratio).isEqualTo(0f)
    }

    @Test
    fun `sort order reflects priority plus recency`() = runTest {
        // nicotine (LOW=0.2) has max weekly activity → recency 1.0 → score 1.2
        // sweets (HIGH=1.0) has no weekly activity → recency 0.0 → score 1.0
        coEvery { choiceRepo.choiceCountsSince(any()) } returns listOf(
            TallyChoiceCount(tallyId = 2L, count = 10)
        )

        val vm = createViewModel()
        val items = vm.uiState.value.tallies
        assertThat(items[0].tally.name).isEqualTo("Nicotine")
        assertThat(items[1].tally.name).isEqualTo("Sweets")
    }

    @Test
    fun `no weekly activity sorts by priority alone`() = runTest {
        val vm = createViewModel()
        val items = vm.uiState.value.tallies
        // HIGH (1.0) before LOW (0.2)
        assertThat(items[0].tally.name).isEqualTo("Sweets")
        assertThat(items[1].tally.name).isEqualTo("Nicotine")
    }

    @Test
    fun `switches to daily counts when more than 10 choices today`() = runTest {
        val now = Instant.now()
        val todayChoices = (1..12).map { i ->
            Choice(i.toLong(), 1L, now.minusSeconds(i * 60L), abstained = i % 2 == 0)
        }
        coEvery { choiceRepo.choicesToday(1L, any(), any()) } returns todayChoices
        coEvery { choiceRepo.recentChoices(1L, 10) } returns todayChoices.take(10)

        val vm = createViewModel(listOf(sweets))
        val item = vm.uiState.value.tallies.first()
        // daily counts: 12 total, 6 abstained (even indices)
        assertThat(item.totalCount).isEqualTo(12)
        assertThat(item.abstainCount).isEqualTo(6)
    }
}
