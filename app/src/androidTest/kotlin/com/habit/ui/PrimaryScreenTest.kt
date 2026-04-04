package com.habit.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.habit.data.Activity
import com.habit.data.ActivityRepository
import com.habit.data.DayBoundary
import com.habit.data.Habit
import com.habit.data.HabitRepository
import com.habit.data.Priority
import com.habit.data.TargetMode
import com.habit.viewmodel.AgendaViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PrimaryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val today = LocalDate.of(2026, 3, 30)

    private val habitRepo = mockk<HabitRepository>()
    private val activityRepo = mockk<ActivityRepository>(relaxed = true)
    private val dayBoundary = mockk<DayBoundary>()

    private val qigong = Habit(
        id = "qigong",
        name = "Qigong",
        timesOfDay = listOf(7),
        sortOrder = 1,
        daysActive = DayOfWeek.entries.toSet(),
        dailyTarget = 2,
        dailyTargetMode = TargetMode.AT_LEAST,
        timed = true,
        thresholdMinutes = 30,
        thresholdType = null,
        priority = Priority.HIGH,
        dailyTexts = emptyMap()
    )

    private val vitamins = Habit(
        id = "vitamins",
        name = "Vitamins",
        timesOfDay = listOf(7),
        sortOrder = 2,
        daysActive = DayOfWeek.entries.toSet(),
        dailyTarget = 1,
        dailyTargetMode = TargetMode.EXACTLY,
        timed = false,
        thresholdMinutes = null,
        thresholdType = null,
        priority = Priority.MEDIUM,
        dailyTexts = emptyMap()
    )

    private val habitsFlow = MutableStateFlow(listOf(qigong, vitamins))
    private val activitiesFlow = MutableStateFlow<List<Activity>>(emptyList())

    @Before
    fun setUp() {
        every { dayBoundary.today() } returns today
        every { habitRepo.allHabits() } returns habitsFlow
        every { activityRepo.activitiesForDate(today) } returns activitiesFlow
        coEvery { activityRepo.create(any()) } returns 1L
        coEvery { activityRepo.inProgressActivity(any(), any()) } returns null
        coEvery { activityRepo.completedHistoryForHabit(any()) } returns emptyList()
        coEvery { habitRepo.getById("qigong") } returns qigong
        coEvery { habitRepo.getById("vitamins") } returns vitamins
    }

    private fun createViewModel() = AgendaViewModel(
        habitRepo, activityRepo, dayBoundary,
        tickDispatcher = StandardTestDispatcher()
    )

    private fun setScreen(vm: AgendaViewModel) {
        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                PrimaryScreen(vm)
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun mainLayoutShowsAgendaAndProgressBar() {
        setScreen(createViewModel())
        composeTestRule.onNodeWithText("Qigong").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vitamins").assertIsDisplayed()
        composeTestRule.onNodeWithText("0/3").assertIsDisplayed()
    }

    @Test
    fun tapAgendaItemShowsHabitInActivityView() {
        setScreen(createViewModel())
        composeTestRule.onNodeWithText("Qigong").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Start").assertIsDisplayed()
    }

    @Test
    fun tapProgressBarSwitchesToReview() {
        setScreen(createViewModel())
        composeTestRule.onNodeWithText("0/3").performClick()
        composeTestRule.waitForIdle()
        // agenda bar and collapsed summary both show "remaining" — check at least one exists
        composeTestRule.onAllNodesWithText("3 remaining")[0].assertIsDisplayed()
    }

    @Test
    fun tapAgendaBarSwitchesBackToMain() {
        setScreen(createViewModel())
        composeTestRule.onNodeWithText("0/3").performClick()
        composeTestRule.waitForIdle()
        // tap the agenda bar (last node with "3 remaining")
        val nodes = composeTestRule.onAllNodesWithText("3 remaining")
        nodes[nodes.fetchSemanticsNodes().lastIndex].performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Qigong").assertIsDisplayed()
    }

    @Test
    fun reviewLayoutShowsCompletedItems() {
        activitiesFlow.value = listOf(
            Activity(
                id = 1,
                habitId = "vitamins",
                attributedDate = today,
                startTime = null,
                note = "took them",
                completedAt = Instant.now()
            )
        )
        setScreen(createViewModel())
        composeTestRule.onNodeWithText("1/3").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("took them").assertIsDisplayed()
    }

    @Test
    fun tapTimedHabitShowsStartButton() {
        setScreen(createViewModel())
        composeTestRule.onNodeWithText("Qigong").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Start").assertIsDisplayed()
        composeTestRule.onNodeWithText("0:00").assertIsDisplayed()
    }

    @Test
    fun untimedHabitHasNoTimerControls() {
        setScreen(createViewModel())
        composeTestRule.onNodeWithText("Vitamins").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("Start").assertCountEquals(0)
    }
}
