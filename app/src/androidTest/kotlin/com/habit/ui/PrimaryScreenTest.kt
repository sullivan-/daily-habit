package com.habit.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PrimaryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val today = LocalDate.of(2026, 3, 30)

    private val habitRepo = mockk<HabitRepository>()
    private val activityRepo = mockk<ActivityRepository>(relaxed = true)
    private val dayBoundary = mockk<DayBoundary>()

    private val qigong = Habit(
        id = "qigong",
        name = "Qigong",
        timeOfDay = 7,
        sortOrder = 1,
        daysActive = DayOfWeek.entries.toSet(),
        dailyTarget = 2,
        dailyTargetMode = TargetMode.AT_LEAST,
        timed = true,
        chimeIntervalSeconds = 10,
        thresholdMinutes = 30,
        thresholdType = null,
        priority = Priority.HIGH,
        dailyTexts = emptyMap()
    )

    private val vitamins = Habit(
        id = "vitamins",
        name = "Vitamins",
        timeOfDay = 7,
        sortOrder = 2,
        daysActive = DayOfWeek.entries.toSet(),
        dailyTarget = 1,
        dailyTargetMode = TargetMode.EXACTLY,
        timed = false,
        chimeIntervalSeconds = null,
        thresholdMinutes = null,
        thresholdType = null,
        priority = Priority.MEDIUM,
        dailyTexts = emptyMap()
    )

    private val habitsFlow = MutableStateFlow(listOf(qigong, vitamins))
    private val activitiesFlow = MutableStateFlow<List<Activity>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { dayBoundary.today() } returns today
        every { habitRepo.allHabits() } returns habitsFlow
        every { activityRepo.activitiesForDate(today) } returns activitiesFlow
        coEvery { activityRepo.create(any()) } returns 1L
        coEvery { habitRepo.getById(any()) } returns vitamins
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = AgendaViewModel(
        habitRepo, activityRepo, dayBoundary,
        tickDispatcher = testDispatcher
    )

    @Test
    fun mainLayoutShowsAgendaAndProgressBar() {
        val vm = createViewModel()
        composeTestRule.setContent {
            MaterialTheme { PrimaryScreen(vm) }
        }

        composeTestRule.onNodeWithText("Qigong").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vitamins").assertIsDisplayed()
        composeTestRule.onNodeWithText("0/3").assertIsDisplayed()
    }

    @Test
    fun tapAgendaItemShowsHabitInActivityView() {
        val vm = createViewModel()
        composeTestRule.setContent {
            MaterialTheme { PrimaryScreen(vm) }
        }

        composeTestRule.onNodeWithText("Qigong").performClick()
        composeTestRule.onNodeWithText("Start").assertIsDisplayed()
    }

    @Test
    fun tapProgressBarSwitchesToReview() {
        val vm = createViewModel()
        composeTestRule.setContent {
            MaterialTheme { PrimaryScreen(vm) }
        }

        composeTestRule.onNodeWithText("0/3").performClick()
        composeTestRule.onNodeWithText("remaining", substring = true).assertIsDisplayed()
    }

    @Test
    fun tapAgendaBarSwitchesBackToMain() {
        val vm = createViewModel()
        composeTestRule.setContent {
            MaterialTheme { PrimaryScreen(vm) }
        }

        // switch to review
        composeTestRule.onNodeWithText("0/3").performClick()
        // switch back to main
        composeTestRule.onNodeWithText("remaining", substring = true).performClick()
        // agenda should be visible again
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
                endTime = null,
                elapsedMs = 0,
                note = "took them",
                completedAt = Instant.now()
            )
        )
        val vm = createViewModel()
        composeTestRule.setContent {
            MaterialTheme { PrimaryScreen(vm) }
        }

        // switch to review
        composeTestRule.onNodeWithText("1/3").performClick()
        composeTestRule.onNodeWithText("took them").assertIsDisplayed()
    }
}
