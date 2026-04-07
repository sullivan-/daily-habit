package com.habit.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.habit.data.Priority
import com.habit.data.Tally
import com.habit.data.TallyRepository
import com.habit.viewmodel.TallyEditorViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TallyEditorScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val tallyRepo = mockk<TallyRepository>(relaxed = true)
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
    }

    private fun setScreen(tallyId: String? = null) {
        val vm = TallyEditorViewModel(tallyRepo)
        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                TallyEditorScreen(
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
    fun editTallyShowsCorrectTitle() {
        setScreen(tallyId = "1")
        composeTestRule.onNodeWithText("Edit Tally").assertIsDisplayed()
    }

    @Test
    fun editTallyPopulatesName() {
        setScreen(tallyId = "1")
        composeTestRule.onNodeWithText("Sweets").assertIsDisplayed()
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
        composeTestRule.onNodeWithText("Sweets").performTextReplacement("Candy")
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
        // tap the "Delete" button in the dialog
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
}
