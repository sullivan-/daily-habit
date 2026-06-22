package com.habit.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.habit.data.Choice
import com.habit.data.ChoiceRepository
import com.habit.data.DayBoundary
import com.habit.data.Priority
import com.habit.data.Tally
import com.habit.data.TallyRepository
import com.habit.viewmodel.ChoicesViewModel
import com.habit.viewmodel.StreakCalculator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ChoicesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val tallyRepo = mockk<TallyRepository>()
    private val choiceRepo = mockk<ChoiceRepository>(relaxed = true)
    private val dayBoundary = mockk<DayBoundary>()

    private val talliesFlow = MutableStateFlow<List<Tally>>(emptyList())

    private val sweets = Tally(id = "1", name = "Sweets", priority = Priority.HIGH)
    private val nicotine = Tally(id = "2", name = "Nicotine", priority = Priority.LOW)

    private var detailsTallyId: String? = null
    private var newTallyRequested = false
    private var backRequested = false

    @Before
    fun setUp() {
        every { tallyRepo.allTallies() } returns talliesFlow
        every { choiceRepo.choiceChanges() } returns flowOf(0)
        every { dayBoundary.today() } returns LocalDate.of(2026, 4, 3)
        coEvery { choiceRepo.choiceCountsSince(any()) } returns emptyList()
        coEvery { choiceRepo.recentChoices(any(), any()) } returns emptyList()
        coEvery { choiceRepo.choicesToday(any(), any(), any()) } returns emptyList()
        coEvery { choiceRepo.mostRecentChoice(any()) } returns null
        coEvery { choiceRepo.mostRecentIndulgence(any()) } returns null
        coEvery { choiceRepo.firstAbstentionAfter(any(), any()) } returns null
        coEvery { choiceRepo.firstAbstention(any()) } returns null

        detailsTallyId = null
        newTallyRequested = false
        backRequested = false
    }

    private fun setScreen(tallies: List<Tally> = emptyList()) {
        talliesFlow.value = tallies
        val streakCalculator = StreakCalculator(choiceRepo)
        val vm = ChoicesViewModel(tallyRepo, choiceRepo, dayBoundary, streakCalculator)
        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                ChoicesScreen(
                    viewModel = vm,
                    onDetails = { detailsTallyId = it },
                    onNewTally = { newTallyRequested = true },
                    onBack = { backRequested = true }
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun emptyStateShowsTitle() {
        setScreen()
        composeTestRule.onNodeWithText("Choices").assertIsDisplayed()
    }

    @Test
    fun emptyStateShowsNoTallyRows() {
        setScreen()
        composeTestRule.onAllNodesWithText("No").assertCountEquals(0)
    }

    @Test
    fun showsTallyNames() {
        setScreen(listOf(sweets, nicotine))
        composeTestRule.onNodeWithText("Sweets").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nicotine").assertIsDisplayed()
    }

    @Test
    fun noIndicatorWhenNoChoices() {
        setScreen(listOf(sweets))
        composeTestRule.onAllNodesWithText("0/0").assertCountEquals(0)
    }

    @Test
    fun tapNoRecordsAbstainChoice() {
        setScreen(listOf(sweets))
        // there are two "No" buttons if two tallies, one if one
        composeTestRule.onAllNodesWithText("No")[0].performClick()
        composeTestRule.waitForIdle()

        coVerify {
            choiceRepo.record(match { it.tallyId == "1" && it.abstained })
        }
    }

    @Test
    fun tapYesRecordsIndulgeChoice() {
        setScreen(listOf(sweets))
        composeTestRule.onAllNodesWithText("Yes")[0].performClick()
        composeTestRule.waitForIdle()

        coVerify {
            choiceRepo.record(match { it.tallyId == "1" && !it.abstained })
        }
    }

    @Test
    fun tapNameNavigatesToDetails() {
        setScreen(listOf(sweets))
        composeTestRule.onNodeWithText("Sweets").performClick()
        composeTestRule.waitForIdle()
        assert(detailsTallyId == "1")
    }

    @Test
    fun noEditIconInTallyRow() {
        setScreen(listOf(sweets))
        composeTestRule.onAllNodesWithContentDescription("edit")
            .fetchSemanticsNodes().let {
                assert(it.isEmpty())
            }
    }

    @Test
    fun showsStreakTextWhenStreakExists() {
        val now = Instant.now()
        val fiveDaysAgo = now.minusSeconds(5 * 24 * 3600)
        coEvery { choiceRepo.mostRecentChoice("1") } returns
            Choice(1, "1", now, abstained = true)
        coEvery { choiceRepo.firstAbstention("1") } returns
            Choice(1, "1", fiveDaysAgo, abstained = true)

        setScreen(listOf(sweets))
        // the row renders "5 day streak (<count>)"; assert the duration label
        composeTestRule.onNodeWithText("5 day streak", substring = true).assertIsDisplayed()
    }

    @Test
    fun noStreakTextWhenNoStreak() {
        setScreen(listOf(sweets))
        composeTestRule.onAllNodesWithText("streak")
            .fetchSemanticsNodes().let {
                assert(it.isEmpty())
            }
    }

}
