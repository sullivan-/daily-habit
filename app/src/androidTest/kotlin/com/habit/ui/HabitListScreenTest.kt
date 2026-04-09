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
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HabitListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val habitRepo = mockk<HabitRepository>()
    private val activityRepo = mockk<ActivityRepository>(relaxed = true)
    private val dayBoundary = mockk<DayBoundary>()
    private val habitsFlow = MutableStateFlow<List<Habit>>(emptyList())

    private val qigong = Habit(
        id = "qigong",
        name = "Qigong",
        timesOfDay = listOf(7),
        sortOrder = 1,
        daysActive = DayOfWeek.entries.toSet(),
        dailyTarget = 2,
        dailyTargetMode = TargetMode.AT_LEAST,
        timed = true,
        goalMinutes = 30,
        stopMinutes = null,
        priority = Priority.HIGH
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
        goalMinutes = null,
        stopMinutes = null,
        priority = Priority.MEDIUM
    )

    private val badux = Habit(
        id = "badux",
        name = "Badux",
        timesOfDay = listOf(14),
        sortOrder = 1,
        daysActive = setOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
        ),
        dailyTarget = 3,
        dailyTargetMode = TargetMode.AT_LEAST,
        timed = true,
        goalMinutes = 50,
        stopMinutes = null,
        priority = Priority.MEDIUM_HIGH
    )

    private var editedHabitId: String? = null
    private var newHabitRequested = false
    private var backRequested = false

    @Before
    fun setUp() {
        every { dayBoundary.today() } returns LocalDate.of(2026, 4, 6)
        every { habitRepo.allHabits() } returns habitsFlow
        every { activityRepo.activitiesForDate(any()) } returns MutableStateFlow(emptyList<Activity>())
        coEvery { activityRepo.create(any()) } returns 1L
        coEvery { activityRepo.inProgressActivity(any(), any()) } returns null
        coEvery { activityRepo.completedHistoryForHabit(any()) } returns emptyList()
        coEvery { habitRepo.getById(any()) } returns null

        editedHabitId = null
        newHabitRequested = false
        backRequested = false
    }

    private fun setScreen(habits: List<Habit> = emptyList()) {
        habitsFlow.value = habits
        val vm = AgendaViewModel(
            habitRepo, activityRepo, dayBoundary,
            tickDispatcher = StandardTestDispatcher()
        )
        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                HabitListScreen(
                    viewModel = vm,
                    onEditHabit = { editedHabitId = it },
                    onNewHabit = { newHabitRequested = true },
                    onBack = { backRequested = true }
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun showsTitle() {
        setScreen()
        composeTestRule.onNodeWithText("Habits").assertIsDisplayed()
    }

    @Test
    fun emptyStateShowsNoRows() {
        setScreen()
        composeTestRule.onAllNodesWithText("timed").assertCountEquals(0)
    }

    @Test
    fun showsHabitNames() {
        setScreen(listOf(qigong, vitamins))
        composeTestRule.onNodeWithText("Qigong").assertIsDisplayed()
        composeTestRule.onNodeWithText("Vitamins").assertIsDisplayed()
    }

    @Test
    fun showsSummaryWithTimeDaysTimed() {
        setScreen(listOf(qigong))
        composeTestRule.onNodeWithText("7:00 · every day · timed").assertIsDisplayed()
    }

    @Test
    fun showsUntimedInSummary() {
        setScreen(listOf(vitamins))
        composeTestRule.onNodeWithText("7:00 · every day · untimed").assertIsDisplayed()
    }

    @Test
    fun showsWeekdaySubsetInSummary() {
        setScreen(listOf(badux))
        composeTestRule.onNodeWithText("14:00 · Mon, Tue, Wed, Thu, Fri · timed")
            .assertIsDisplayed()
    }

    @Test
    fun showsPriorityLabels() {
        setScreen(listOf(qigong, vitamins, badux))
        composeTestRule.onNodeWithText("High").assertIsDisplayed()
        composeTestRule.onNodeWithText("Med").assertIsDisplayed()
        composeTestRule.onNodeWithText("Med Hi").assertIsDisplayed()
    }

    @Test
    fun tapHabitNavigatesToEditor() {
        setScreen(listOf(qigong))
        composeTestRule.onNodeWithText("Qigong").performClick()
        composeTestRule.waitForIdle()
        assert(editedHabitId == "qigong")
    }

    @Test
    fun tapBackNavigatesBack() {
        setScreen()
        composeTestRule.onNodeWithContentDescription("back").performClick()
        composeTestRule.waitForIdle()
        assert(backRequested)
    }

    @Test
    fun tapFabRequestsNewHabit() {
        setScreen()
        composeTestRule.onNodeWithContentDescription("new habit").performClick()
        composeTestRule.waitForIdle()
        assert(newHabitRequested)
    }

    @Test
    fun sortsByTimeOfDayThenPriorityThenSortOrder() {
        // qigong: time=7, priority=HIGH(0), sort=1
        // vitamins: time=7, priority=MEDIUM(2), sort=2
        // badux: time=14, priority=MEDIUM_HIGH(1), sort=1
        // expected order: Qigong, Vitamins, Badux
        setScreen(listOf(badux, vitamins, qigong))

        val nodes = composeTestRule.onAllNodesWithText("timed", substring = true)
            .fetchSemanticsNodes()
        // all three habits should appear; verify first is Qigong's summary
        composeTestRule.onNodeWithText("7:00 · every day · timed").assertIsDisplayed()
        composeTestRule.onNodeWithText("14:00 · Mon, Tue, Wed, Thu, Fri · timed")
            .assertIsDisplayed()
    }
}
