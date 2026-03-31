package com.habit.viewmodel

import com.google.common.truth.Truth.assertThat
import com.habit.data.Activity
import com.habit.data.ActivityRepository
import com.habit.data.DayBoundary
import com.habit.data.Habit
import com.habit.data.HabitRepository
import com.habit.data.Priority
import com.habit.data.TargetMode
import com.habit.data.ThresholdType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AgendaViewModelTest {

    private val mainDispatcher = UnconfinedTestDispatcher()
    private val tickDispatcher = StandardTestDispatcher()
    private val today = LocalDate.of(2026, 3, 30) // Monday

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
        thresholdType = ThresholdType.GOAL_MET,
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
        Dispatchers.setMain(mainDispatcher)
        every { dayBoundary.today() } returns today
        every { habitRepo.allHabits() } returns habitsFlow
        every { activityRepo.activitiesForDate(today) } returns activitiesFlow
        coEvery { activityRepo.create(any()) } returns 1L
        coEvery { activityRepo.inProgressActivity(any(), any()) } returns null
        coEvery { habitRepo.getById("qigong") } returns qigong
        coEvery { habitRepo.getById("vitamins") } returns vitamins
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = AgendaViewModel(
        habitRepo, activityRepo, dayBoundary, tickDispatcher
    )

    @Test
    fun `initial state loads habits and activities`() = runTest {
        val vm = createViewModel()
        val state = vm.uiState.value
        assertThat(state.habits).hasSize(2)
        assertThat(state.todayActivities).isEmpty()
        assertThat(state.layout).isEqualTo(Layout.MAIN)
    }

    @Test
    fun `selectHabit updates selectedHabitId and creates activity`() = runTest {
        val vm = createViewModel()
        vm.selectHabit("qigong")
        assertThat(vm.uiState.value.selectedHabitId).isEqualTo("qigong")
        assertThat(vm.uiState.value.activeActivity).isNotNull()
        assertThat(vm.uiState.value.activeActivity?.habitId).isEqualTo("qigong")
        coVerify { activityRepo.create(any()) }
    }

    @Test
    fun `selectHabit resumes existing in-progress activity`() = runTest {
        val existing = Activity(
            id = 42,
            habitId = "qigong",
            attributedDate = today,
            startTime = null,
            endTime = null,
            elapsedMs = 0,
            note = "saved note",
            completedAt = null
        )
        coEvery { activityRepo.inProgressActivity("qigong", today) } returns existing

        val vm = createViewModel()
        vm.selectHabit("qigong")
        assertThat(vm.uiState.value.activeActivity?.id).isEqualTo(42)
        assertThat(vm.uiState.value.activeActivity?.note).isEqualTo("saved note")
    }

    @Test
    fun `clearSelection clears selectedHabitId`() = runTest {
        val vm = createViewModel()
        vm.selectHabit("qigong")
        vm.clearSelection()
        assertThat(vm.uiState.value.selectedHabitId).isNull()
        assertThat(vm.uiState.value.activeActivity).isNull()
    }

    @Test
    fun `switchToReview changes layout`() = runTest {
        val vm = createViewModel()
        vm.switchToReview()
        assertThat(vm.uiState.value.layout).isEqualTo(Layout.REVIEW)
    }

    @Test
    fun `switchToMain changes layout`() = runTest {
        val vm = createViewModel()
        vm.switchToReview()
        vm.switchToMain()
        assertThat(vm.uiState.value.layout).isEqualTo(Layout.MAIN)
    }

    @Test
    fun `expandActivity changes layout to activity focused`() = runTest {
        val vm = createViewModel()
        vm.expandActivity()
        assertThat(vm.uiState.value.layout).isEqualTo(Layout.ACTIVITY_FOCUSED)
    }

    @Test
    fun `startTimer sets timer running`() = runTest {
        val vm = createViewModel()
        vm.selectHabit("qigong")
        vm.startTimer()

        val state = vm.uiState.value
        assertThat(state.timerRunning).isTrue()
        assertThat(state.activeActivity?.startTime).isNotNull()
    }

    @Test
    fun `stopTimer pauses and persists elapsed time`() = runTest {
        val vm = createViewModel()
        vm.selectHabit("qigong")
        vm.startTimer()
        vm.stopTimer()

        val state = vm.uiState.value
        assertThat(state.timerRunning).isFalse()
        assertThat(state.activeActivity).isNotNull()
        coVerify { activityRepo.update(any()) }
    }

    @Test
    fun `completeActivity finalizes and advances to next habit`() = runTest {
        val vm = createViewModel()
        vm.selectHabit("qigong")
        vm.startTimer()
        vm.completeActivity("session notes")

        val state = vm.uiState.value
        assertThat(state.timerRunning).isFalse()
        assertThat(state.elapsedMs).isEqualTo(0)
        // should advance to next habit (vitamins)
        assertThat(state.selectedHabitId).isEqualTo("vitamins")
    }

    @Test
    fun `completeUntimed completes active activity`() = runTest {
        val vm = createViewModel()
        vm.selectHabit("vitamins")
        vm.completeUntimed("vitamins", "took them")

        coVerify {
            activityRepo.update(match {
                it.habitId == "vitamins" &&
                    it.note == "took them" &&
                    it.completedAt != null
            })
        }
    }

    @Test
    fun `updateNote persists to database`() = runTest {
        val vm = createViewModel()
        vm.selectHabit("qigong")
        vm.updateNote("standing form")

        coVerify {
            activityRepo.update(match { it.note == "standing form" })
        }
        assertThat(vm.uiState.value.activeActivity?.note).isEqualTo("standing form")
    }

    @Test
    fun `doAgain switches to main with habit selected for at-least habits`() = runTest {
        val vm = createViewModel()
        vm.switchToReview()
        vm.doAgain("qigong")

        val state = vm.uiState.value
        assertThat(state.layout).isEqualTo(Layout.MAIN)
        assertThat(state.selectedHabitId).isEqualTo("qigong")
    }

    @Test
    fun `doAgain does nothing for exactly habits`() = runTest {
        val vm = createViewModel()
        vm.switchToReview()
        vm.doAgain("vitamins")
        assertThat(vm.uiState.value.selectedHabitId).isNull()
    }

    @Test
    fun `progress count reflects completed activities`() = runTest {
        val vm = createViewModel()
        assertThat(vm.uiState.value.progressCount).isEqualTo(0)

        activitiesFlow.value = listOf(
            Activity(
                id = 1,
                habitId = "vitamins",
                attributedDate = today,
                startTime = null,
                endTime = null,
                elapsedMs = 0,
                note = "",
                completedAt = Instant.now()
            )
        )

        assertThat(vm.uiState.value.progressCount).isEqualTo(1)
    }

    @Test
    fun `selectHabit blocked when timer running on different habit`() = runTest {
        val vm = createViewModel()
        vm.selectHabit("qigong")
        vm.startTimer()

        vm.selectHabit("vitamins")
        assertThat(vm.uiState.value.selectedHabitId).isEqualTo("qigong")
    }

    @Test
    fun `forceSelectHabit stops timer and switches`() = runTest {
        val vm = createViewModel()
        vm.selectHabit("qigong")
        vm.startTimer()
        vm.forceSelectHabit("vitamins")

        val state = vm.uiState.value
        assertThat(state.selectedHabitId).isEqualTo("vitamins")
        assertThat(state.timerRunning).isFalse()
    }
}
