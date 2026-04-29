package com.habit.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.habit.data.Choice
import com.habit.data.ChoiceRepository
import com.habit.data.DayBoundary
import com.habit.data.Priority
import com.habit.data.Tally
import com.habit.data.TallyRepository
import com.habit.viewmodel.StreakCalculator
import com.habit.viewmodel.TallyDetailsViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TallyDetailsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val tallyRepo = mockk<TallyRepository>(relaxed = true)
    private val choiceRepo = mockk<ChoiceRepository>(relaxed = true)
    private val dayBoundary = DayBoundary(2)
    private var backRequested = false

    private val existingTally = Tally(
        id = "1",
        name = "Sweets",
        priority = Priority.HIGH
    )

    @Before
    fun setUp() {
        backRequested = false
        coEvery { tallyRepo.getById("1") } returns existingTally
        coEvery { choiceRepo.recentChoices(any(), any()) } returns emptyList()
        coEvery { choiceRepo.choicesToday(any(), any(), any()) } returns emptyList()
        coEvery { choiceRepo.mostRecentChoice(any()) } returns null
        coEvery { choiceRepo.mostRecentIndulgence(any()) } returns null
        coEvery { choiceRepo.firstAbstentionAfter(any(), any()) } returns null
        coEvery { choiceRepo.firstAbstention(any()) } returns null
        coEvery { choiceRepo.earliestChoice(any()) } returns null
    }

    private fun setScreen(tallyId: String? = null) {
        val streakCalculator = StreakCalculator(choiceRepo)
        val vm = TallyDetailsViewModel(
            tallyRepo, choiceRepo, dayBoundary, streakCalculator
        )
        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                TallyDetailsScreen(
                    viewModel = vm,
                    tallyId = tallyId,
                    onBack = { backRequested = true }
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun newTallyShowsCorrectTitle() {
        setScreen()
        composeTestRule.onNodeWithText("New Tally").assertIsDisplayed()
    }

    @Test
    fun editTallyShowsNameAsTitle() {
        setScreen(tallyId = "1")
        // name appears in both the title and the text field
        composeTestRule.onAllNodesWithText("Sweets")[0].assertIsDisplayed()
    }

    @Test
    fun saveDisabledWhenNameEmpty() {
        setScreen()
        composeTestRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun saveEnabledWhenNameFilled() {
        setScreen()
        composeTestRule.onNodeWithText("Name").performTextInput("Nicotine")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Save").assertIsEnabled()
    }

    @Test
    fun saveNewTallyInsertsAndNavigatesBack() {
        setScreen()
        composeTestRule.onNodeWithText("Name").performTextInput("Nicotine")
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()

        coVerify { tallyRepo.insert(match { it.name == "Nicotine" }) }
        assert(backRequested)
    }

    @Test
    fun saveExistingTallyUpdates() {
        setScreen(tallyId = "1")
        composeTestRule.onNode(hasText("Sweets") and hasSetTextAction())
            .performTextReplacement("Candy")
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()

        coVerify { tallyRepo.update(match { it.id == "1" && it.name == "Candy" }) }
    }

    @Test
    fun deleteButtonOnlyShownForExisting() {
        setScreen()
        composeTestRule.onAllNodesWithText("Delete Tally")
            .fetchSemanticsNodes().let {
                assert(it.isEmpty())
            }
    }

    @Test
    fun deleteShowsConfirmation() {
        setScreen(tallyId = "1")
        composeTestRule.onNodeWithText("Delete Tally").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Delete Sweets?").assertIsDisplayed()
    }

    @Test
    fun confirmDeleteRemovesTally() {
        setScreen(tallyId = "1")
        composeTestRule.onNodeWithText("Delete Tally").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("Delete")[0].performClick()
        composeTestRule.waitForIdle()

        coVerify { tallyRepo.deleteById("1") }
        assert(backRequested)
    }

    @Test
    fun backWithDirtyStateShowsDiscardDialog() {
        setScreen()
        composeTestRule.onNodeWithText("Name").performTextInput("test")
        composeTestRule.onNodeWithContentDescription("back").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Discard changes?").assertIsDisplayed()
    }

    @Test
    fun hidesStreakAndChoicesSectionsForNewTally() {
        setScreen()
        composeTestRule.onAllNodesWithText("Streak")
            .fetchSemanticsNodes().let {
                assert(it.isEmpty())
            }
        composeTestRule.onAllNodesWithText("Choices")
            .fetchSemanticsNodes().let {
                assert(it.isEmpty())
            }
    }

    @Test
    fun showsStreakSectionWithDurationAndDate() {
        val now = Instant.now()
        val fiveDaysAgo = now.minus(5, ChronoUnit.DAYS)
        coEvery { choiceRepo.mostRecentChoice("1") } returns
            Choice(1, "1", now, abstained = true)
        coEvery { choiceRepo.firstAbstention("1") } returns
            Choice(1, "1", fiveDaysAgo, abstained = true)
        coEvery { choiceRepo.earliestChoice("1") } returns
            Choice(1, "1", fiveDaysAgo, abstained = true)

        setScreen(tallyId = "1")
        composeTestRule.onNodeWithText("5 day streak").assertIsDisplayed()
    }

    @Test
    fun showsNoCurrentStreakWhenMostRecentIsYes() {
        val now = Instant.now()
        coEvery { choiceRepo.mostRecentChoice("1") } returns
            Choice(1, "1", now, abstained = false)
        coEvery { choiceRepo.earliestChoice("1") } returns
            Choice(1, "1", now, abstained = false)

        setScreen(tallyId = "1")
        composeTestRule.onNodeWithText("no current streak").assertIsDisplayed()
    }

    @Test
    fun recordFirstNoButtonIsVisible() {
        setScreen(tallyId = "1")
        composeTestRule.onNodeWithText("Record First No").assertIsDisplayed()
    }

    @Test
    fun recordFirstNoOpensDatePicker() {
        setScreen(tallyId = "1")
        composeTestRule.onNodeWithText("Record First No").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("OK").assertIsDisplayed()
    }
}
