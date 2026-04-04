package com.habit.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
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

    private val sweets = Tally(id = 1L, name = "Sweets", priority = Priority.HIGH)
    private val nicotine = Tally(id = 2L, name = "Nicotine", priority = Priority.LOW)

    private var editedTallyId: Long? = null
    private var newTallyRequested = false
    private var backRequested = false

    @Before
    fun setUp() {
        every { tallyRepo.allTallies() } returns talliesFlow
        every { dayBoundary.today() } returns LocalDate.of(2026, 4, 3)
        coEvery { choiceRepo.choiceCountsSince(any()) } returns emptyList()
        coEvery { choiceRepo.recentChoices(any(), any()) } returns emptyList()
        coEvery { choiceRepo.choicesToday(any(), any(), any()) } returns emptyList()

        editedTallyId = null
        newTallyRequested = false
        backRequested = false
    }

    private fun setScreen(tallies: List<Tally> = emptyList()) {
        talliesFlow.value = tallies
        val vm = ChoicesViewModel(tallyRepo, choiceRepo, dayBoundary)
        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                ChoicesScreen(
                    viewModel = vm,
                    onEditTally = { editedTallyId = it },
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
            choiceRepo.record(match { it.tallyId == 1L && it.abstained })
        }
    }

    @Test
    fun tapYesRecordsIndulgeChoice() {
        setScreen(listOf(sweets))
        composeTestRule.onAllNodesWithText("Yes")[0].performClick()
        composeTestRule.waitForIdle()

        coVerify {
            choiceRepo.record(match { it.tallyId == 1L && !it.abstained })
        }
    }

    @Test
    fun showsIndicatorWhenChoicesExist() {
        val now = Instant.now()
        coEvery { choiceRepo.recentChoices(1L, 10) } returns listOf(
            Choice(1, 1L, now, abstained = true),
            Choice(2, 1L, now.minusSeconds(60), abstained = false),
            Choice(3, 1L, now.minusSeconds(120), abstained = true)
        )

        setScreen(listOf(sweets))
        composeTestRule.onNodeWithText("2/3").assertIsDisplayed()
    }

    @Test
    fun tapEditNavigatesToEditor() {
        setScreen(listOf(sweets))
        composeTestRule.onNodeWithContentDescription("edit").performClick()
        composeTestRule.waitForIdle()
        assert(editedTallyId == 1L)
    }

    @Test
    fun tapBackNavigatesBack() {
        setScreen()
        composeTestRule.onNodeWithContentDescription("back").performClick()
        composeTestRule.waitForIdle()
        assert(backRequested)
    }
}
