package com.habit.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.isToggleable
import com.habit.data.Habit
import com.habit.data.HabitRepository
import com.habit.data.Priority
import com.habit.data.TargetMode
import com.habit.data.ThresholdType
import com.habit.viewmodel.HabitEditorViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.DayOfWeek
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HabitEditorScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val habitRepo = mockk<HabitRepository>(relaxed = true)
    private var backRequested = false

    private val existingHabit = Habit(
        id = "qigong",
        name = "Qigong",
        timesOfDay = listOf(7),
        sortOrder = 1,
        daysActive = DayOfWeek.entries.toSet(),
        dailyTarget = 2,
        dailyTargetMode = TargetMode.AT_LEAST,
        timed = true,
        thresholdMinutes = 30,
        thresholdType = ThresholdType.GOAL,
        priority = Priority.HIGH,
        dailyTexts = mapOf(DayOfWeek.MONDAY to "morning session")
    )

    @Before
    fun setUp() {
        backRequested = false
        coEvery { habitRepo.getById("qigong") } returns existingHabit
        coEvery { habitRepo.allIds() } returns listOf("qigong")
    }

    private fun setScreen(habitId: String? = null) {
        val vm = HabitEditorViewModel(habitRepo)
        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                HabitEditorScreen(
                    viewModel = vm,
                    habitId = habitId,
                    onBack = { backRequested = true }
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    // --- title ---

    @Test
    fun newHabitShowsCorrectTitle() {
        setScreen()
        composeTestRule.onNodeWithText("New Habit").assertIsDisplayed()
    }

    @Test
    fun editHabitShowsCorrectTitle() {
        setScreen(habitId = "qigong")
        composeTestRule.onNodeWithText("Edit Habit").assertIsDisplayed()
    }

    // --- name field ---

    @Test
    fun editHabitPopulatesName() {
        setScreen(habitId = "qigong")
        composeTestRule.onNodeWithText("Qigong").assertIsDisplayed()
    }

    @Test
    fun saveDisabledWhenNameEmpty() {
        setScreen()
        composeTestRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun saveEnabledWhenNameFilled() {
        setScreen()
        composeTestRule.onNodeWithText("Name").performTextInput("Reading")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Save").assertIsEnabled()
    }

    // --- save ---

    @Test
    fun saveNewHabitInsertsAndNavigatesBack() {
        setScreen()
        composeTestRule.onNodeWithText("Name").performTextInput("Reading")
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()

        coVerify { habitRepo.insert(match { it.name == "Reading" }) }
        assert(backRequested)
    }

    @Test
    fun saveExistingHabitUpdates() {
        setScreen(habitId = "qigong")
        composeTestRule.onNodeWithText("Qigong").performTextReplacement("Tai Chi")
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()

        coVerify { habitRepo.update(match { it.id == "qigong" && it.name == "Tai Chi" }) }
    }

    // --- times of day ---

    @Test
    fun editHabitShowsTimeChip() {
        setScreen(habitId = "qigong")
        composeTestRule.onNodeWithText("7:00").assertIsDisplayed()
    }

    @Test
    fun newHabitShowsDefaultTimeChip() {
        setScreen()
        composeTestRule.onNodeWithText("8:00").assertIsDisplayed()
    }

    @Test
    fun addTimeOpensDialogAndAddsChip() {
        setScreen()
        composeTestRule.onNodeWithText("+").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Add time").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hour (0-23)").performTextInput("14")
        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("14:00").assertIsDisplayed()
    }

    @Test
    fun removeTimeChip() {
        setScreen()
        // add a second time first so removal is allowed
        composeTestRule.onNodeWithText("+").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Hour (0-23)").performTextInput("14")
        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.waitForIdle()
        // now remove the original 8:00
        composeTestRule.onNodeWithText("8:00").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("8:00").fetchSemanticsNodes().let {
            assert(it.isEmpty())
        }
    }

    // --- daily target mode ---

    @Test
    fun editHabitShowsAtLeastSelected() {
        setScreen(habitId = "qigong")
        composeTestRule.onNodeWithText("At least").assertIsSelected()
    }

    @Test
    fun canSwitchToExactly() {
        setScreen(habitId = "qigong")
        composeTestRule.onNodeWithText("Exactly").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Exactly").assertIsSelected()
    }

    // --- timed checkbox ---

    @Test
    fun editTimedHabitShowsCheckboxOn() {
        setScreen(habitId = "qigong")
        composeTestRule.onNodeWithText("Timed habit").assertIsDisplayed()
    }

    @Test
    fun timedFieldsVisibleWhenTimedChecked() {
        setScreen(habitId = "qigong")
        composeTestRule.onNodeWithText("Threshold (minutes)")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun timedFieldsHiddenWhenUntimed() {
        setScreen()
        composeTestRule.onAllNodesWithText("Threshold (minutes)").fetchSemanticsNodes().let {
            assert(it.isEmpty())
        }
    }

    @Test
    fun checkingTimedShowsThresholdField() {
        setScreen()
        composeTestRule.onNode(isToggleable()).performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Threshold (minutes)").assertExists()
    }

    // --- threshold type ---

    @Test
    fun thresholdTypeVisibleWhenThresholdSet() {
        setScreen(habitId = "qigong")
        composeTestRule.onNodeWithText("Goal").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Time to stop").performScrollTo().assertIsDisplayed()
    }

    // --- daily texts ---

    @Test
    fun editHabitShowsDailyText() {
        setScreen(habitId = "qigong")
        composeTestRule.onNodeWithText("morning session").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun dailyTextsFieldsVisible() {
        setScreen()
        composeTestRule.onNodeWithText("Mon").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Sat").performScrollTo().assertIsDisplayed()
    }

    // --- delete ---

    @Test
    fun deleteButtonOnlyShownForExisting() {
        setScreen()
        composeTestRule.onAllNodesWithText("Delete Habit").fetchSemanticsNodes().let {
            assert(it.isEmpty())
        }
    }

    @Test
    fun deleteShowsConfirmation() {
        setScreen(habitId = "qigong")
        composeTestRule.onNodeWithText("Delete Habit").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Delete Qigong?").assertIsDisplayed()
    }

    @Test
    fun confirmDeleteRemovesHabit() {
        setScreen(habitId = "qigong")
        composeTestRule.onNodeWithText("Delete Habit").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("Delete")[0].performClick()
        composeTestRule.waitForIdle()

        coVerify { habitRepo.deleteById("qigong") }
        assert(backRequested)
    }

    // --- discard dialog ---

    @Test
    fun backWithDirtyStateShowsDiscardDialog() {
        setScreen()
        composeTestRule.onNodeWithText("Name").performTextInput("test")
        composeTestRule.onNodeWithContentDescription("back").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Discard changes?").assertIsDisplayed()
    }

    @Test
    fun backWithCleanStateNavigatesDirectly() {
        setScreen()
        composeTestRule.onNodeWithContentDescription("back").performClick()
        composeTestRule.waitForIdle()
        assert(backRequested)
    }
}
