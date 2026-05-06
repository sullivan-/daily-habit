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
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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

    private val streakCalculator = StreakCalculator(choiceRepo)

    private val sweets = Tally(id = "1", name = "Sweets", priority = Priority.HIGH)
    private val nicotine = Tally(id = "2", name = "Nicotine", priority = Priority.LOW)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { choiceRepo.choiceChanges() } returns flowOf(0)
        coEvery { choiceRepo.choiceCountsSince(any()) } returns emptyList()
        coEvery { choiceRepo.recentChoices(any(), any()) } returns emptyList()
        coEvery { choiceRepo.choicesToday(any(), any(), any()) } returns emptyList()
        coEvery { choiceRepo.mostRecentChoice(any()) } returns null
        coEvery { choiceRepo.mostRecentIndulgence(any()) } returns null
        coEvery { choiceRepo.firstAbstentionAfter(any(), any()) } returns null
        coEvery { choiceRepo.firstAbstention(any()) } returns null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(tallies: List<Tally> = listOf(sweets, nicotine)): ChoicesViewModel {
        coEvery { tallyRepo.allTallies() } returns flowOf(tallies)
        return ChoicesViewModel(tallyRepo, choiceRepo, dayBoundary, streakCalculator)
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
        vm.recordChoice("1", abstained = true)

        coVerify {
            choiceRepo.record(match { it.tallyId == "1" && it.abstained })
        }
    }

    @Test
    fun `recordChoice with indulge inserts choice`() = runTest {
        val vm = createViewModel()
        vm.recordChoice("1", abstained = false)

        coVerify {
            choiceRepo.record(match { it.tallyId == "1" && !it.abstained })
        }
    }

    @Test
    fun `indicator shows recent choices`() = runTest {
        val now = Instant.now()
        coEvery { choiceRepo.recentChoices("1", 10) } returns listOf(
            Choice(1, "1", now, abstained = true),
            Choice(2, "1", now.minusSeconds(60), abstained = true),
            Choice(3, "1", now.minusSeconds(120), abstained = false)
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
        coEvery { choiceRepo.recentChoices("1", 10) } returns listOf(
            Choice(1, "1", now, abstained = true),
            Choice(2, "1", now.minusSeconds(60), abstained = true)
        )

        val vm = createViewModel(listOf(sweets))
        assertThat(vm.uiState.value.tallies.first().ratio).isEqualTo(1f)
    }

    @Test
    fun `ratio is 0 when all indulge`() = runTest {
        val now = Instant.now()
        coEvery { choiceRepo.recentChoices("1", 10) } returns listOf(
            Choice(1, "1", now, abstained = false),
            Choice(2, "1", now.minusSeconds(60), abstained = false)
        )

        val vm = createViewModel(listOf(sweets))
        assertThat(vm.uiState.value.tallies.first().ratio).isEqualTo(0f)
    }

    @Test
    fun `sort order is shortest streak first longest streak last`() = runTest {
        val now = Instant.now()
        val sweetsStart = now.minus(2, ChronoUnit.DAYS)
        val nicotineStart = now.minus(10, ChronoUnit.DAYS)

        coEvery { choiceRepo.mostRecentChoice("1") } returns
            Choice(1, "1", now, abstained = true)
        coEvery { choiceRepo.firstAbstention("1") } returns
            Choice(2, "1", sweetsStart, abstained = true)

        coEvery { choiceRepo.mostRecentChoice("2") } returns
            Choice(3, "2", now, abstained = true)
        coEvery { choiceRepo.firstAbstention("2") } returns
            Choice(4, "2", nicotineStart, abstained = true)

        val vm = createViewModel()
        val items = vm.uiState.value.tallies
        assertThat(items[0].tally.name).isEqualTo("Sweets")
        assertThat(items[1].tally.name).isEqualTo("Nicotine")
    }

    @Test
    fun `clicking Yes on a tally with a streak moves it to the top`() = runTest {
        val now = Instant.now()
        val sweetsStart = now.minus(2, ChronoUnit.DAYS)
        val nicotineStart = now.minus(10, ChronoUnit.DAYS)

        coEvery { choiceRepo.mostRecentChoice("1") } returns
            Choice(1, "1", now, abstained = true)
        coEvery { choiceRepo.firstAbstention("1") } returns
            Choice(2, "1", sweetsStart, abstained = true)
        coEvery { choiceRepo.mostRecentChoice("2") } returns
            Choice(3, "2", now, abstained = true)
        coEvery { choiceRepo.firstAbstention("2") } returns
            Choice(4, "2", nicotineStart, abstained = true)

        val changes = MutableStateFlow(0)
        every { choiceRepo.choiceChanges() } returns changes

        val vm = createViewModel()
        assertThat(vm.uiState.value.tallies[0].tally.name).isEqualTo("Sweets")
        assertThat(vm.uiState.value.tallies[1].tally.name).isEqualTo("Nicotine")

        coEvery { choiceRepo.mostRecentChoice("2") } returns
            Choice(5, "2", now.plusSeconds(1), abstained = false)
        vm.recordChoice("2", abstained = false)
        changes.value = changes.value + 1

        val after = vm.uiState.value.tallies
        assertThat(after[0].tally.name).isEqualTo("Nicotine")
        assertThat(after[0].streakStart).isNull()
    }

    @Test
    fun `among broken streaks most recent Yes appears first`() = runTest {
        val now = Instant.now()
        val olderYes = now.minus(5, ChronoUnit.HOURS)

        coEvery { choiceRepo.mostRecentChoice("1") } returns
            Choice(1, "1", olderYes, abstained = false)
        coEvery { choiceRepo.mostRecentChoice("2") } returns
            Choice(2, "2", now, abstained = false)

        val vm = createViewModel()
        val items = vm.uiState.value.tallies
        assertThat(items[0].tally.name).isEqualTo("Nicotine")
        assertThat(items[1].tally.name).isEqualTo("Sweets")
    }

    @Test
    fun `order is broken-streaks then no-choices then active-streaks`() = runTest {
        val brokenA = Tally(id = "a", name = "A", priority = Priority.LOW)
        val brokenB = Tally(id = "b", name = "B", priority = Priority.LOW)
        val noChoices = Tally(id = "c", name = "C", priority = Priority.LOW)
        val streakShort = Tally(id = "d", name = "D", priority = Priority.LOW)
        val streakLong = Tally(id = "e", name = "E", priority = Priority.LOW)

        val now = Instant.now()
        coEvery { choiceRepo.mostRecentChoice("a") } returns
            Choice(1, "a", now, abstained = false)
        coEvery { choiceRepo.mostRecentChoice("b") } returns
            Choice(2, "b", now.minus(1, ChronoUnit.HOURS), abstained = false)
        coEvery { choiceRepo.mostRecentChoice("c") } returns null
        coEvery { choiceRepo.mostRecentChoice("d") } returns
            Choice(3, "d", now, abstained = true)
        coEvery { choiceRepo.firstAbstention("d") } returns
            Choice(4, "d", now.minus(2, ChronoUnit.DAYS), abstained = true)
        coEvery { choiceRepo.mostRecentChoice("e") } returns
            Choice(5, "e", now, abstained = true)
        coEvery { choiceRepo.firstAbstention("e") } returns
            Choice(6, "e", now.minus(20, ChronoUnit.DAYS), abstained = true)

        val vm = createViewModel(
            listOf(brokenA, brokenB, noChoices, streakShort, streakLong)
        )
        val names = vm.uiState.value.tallies.map { it.tally.name }
        assertThat(names).containsExactly("A", "B", "C", "D", "E").inOrder()
    }

    @Test
    fun `tallies with no streak appear above tallies with a streak`() = runTest {
        val now = Instant.now()
        val sweetsStart = now.minus(3, ChronoUnit.DAYS)

        coEvery { choiceRepo.mostRecentChoice("1") } returns
            Choice(1, "1", now, abstained = true)
        coEvery { choiceRepo.firstAbstention("1") } returns
            Choice(2, "1", sweetsStart, abstained = true)

        coEvery { choiceRepo.mostRecentChoice("2") } returns
            Choice(3, "2", now, abstained = false)

        val vm = createViewModel()
        val items = vm.uiState.value.tallies
        assertThat(items[0].tally.name).isEqualTo("Nicotine")
        assertThat(items[0].streakStart).isNull()
        assertThat(items[1].tally.name).isEqualTo("Sweets")
    }

    @Test
    fun `switches to daily counts when more than 10 choices today`() = runTest {
        val now = Instant.now()
        val todayChoices = (1..12).map { i ->
            Choice(i.toLong(), "1", now.minusSeconds(i * 60L), abstained = i % 2 == 0)
        }
        coEvery { choiceRepo.choicesToday("1", any(), any()) } returns todayChoices
        coEvery { choiceRepo.recentChoices("1", 10) } returns todayChoices.take(10)

        val vm = createViewModel(listOf(sweets))
        val item = vm.uiState.value.tallies.first()
        // daily counts: 12 total, 6 abstained (even indices)
        assertThat(item.totalCount).isEqualTo(12)
        assertThat(item.abstainCount).isEqualTo(6)
    }

    @Test
    fun `streakStart is populated when streak exists`() = runTest {
        val now = Instant.now()
        val twoDaysAgo = now.minus(2, ChronoUnit.DAYS)
        coEvery { choiceRepo.mostRecentChoice("1") } returns
            Choice(1, "1", now, abstained = true)
        coEvery { choiceRepo.firstAbstention("1") } returns
            Choice(1, "1", twoDaysAgo, abstained = true)

        val vm = createViewModel(listOf(sweets))
        val item = vm.uiState.value.tallies.first()
        assertThat(item.streakStart).isEqualTo(twoDaysAgo)
    }

    @Test
    fun `streakStart is null when most recent choice is Yes`() = runTest {
        val now = Instant.now()
        coEvery { choiceRepo.mostRecentChoice("1") } returns
            Choice(1, "1", now, abstained = false)

        val vm = createViewModel(listOf(sweets))
        val item = vm.uiState.value.tallies.first()
        assertThat(item.streakStart).isNull()
    }

    @Test
    fun `streakStart is null when tally has no choices`() = runTest {
        val vm = createViewModel(listOf(sweets))
        val item = vm.uiState.value.tallies.first()
        assertThat(item.streakStart).isNull()
    }
}
