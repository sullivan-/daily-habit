package com.habit.viewmodel

import com.google.common.truth.Truth.assertThat
import com.habit.data.Activity
import com.habit.data.ActivityRepository
import com.habit.data.DayBoundary
import com.habit.data.Habit
import com.habit.data.HabitRepository
import com.habit.data.Priority
import com.habit.data.TargetMode

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
        timesOfDay = listOf(7),
        sortOrder = 1,
        daysActive = DayOfWeek.entries.toSet(),
        dailyTarget = 2,
        dailyTargetMode = TargetMode.AT_LEAST,
        timed = true,


        goalMinutes = 30,
        stopMinutes = null,
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


        goalMinutes = null,
        stopMinutes = null,
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
        coEvery { activityRepo.activeActivity() } returns null
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
    fun `cancelTimer deletes activity and creates fresh one`() = runTest {
        coEvery { activityRepo.create(any()) } returns 1L
        val vm = createViewModel()
        vm.selectHabit("qigong")
        vm.startTimer()
        vm.cancelTimer()

        val state = vm.uiState.value
        assertThat(state.timerRunning).isFalse()
        assertThat(state.activeActivity).isNotNull()
        assertThat(state.activeActivity?.startTime).isNull()
    }

    @Test
    fun `completeActivity finalizes and advances to next habit`() = runTest {
        val vm = createViewModel()
        vm.selectHabit("qigong")
        vm.startTimer()
        vm.completeActivity("session notes")

        val state = vm.uiState.value
        assertThat(state.timerRunning).isFalse()
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
    

    

                note = "",
                completedAt = Instant.now()
            )
        )

        assertThat(vm.uiState.value.progressCount).isEqualTo(1)
    }

    @Test
    fun `selectHabit switches while timer keeps running`() = runTest {
        val vm = createViewModel()
        vm.selectHabit("qigong")
        vm.startTimer()

        vm.selectHabit("vitamins")
        assertThat(vm.uiState.value.selectedHabitId).isEqualTo("vitamins")
        assertThat(vm.uiState.value.timerRunning).isTrue()
        assertThat(vm.uiState.value.timedHabitId).isEqualTo("qigong")
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

    @Test
    fun `switchToReview preserves selectedHabitId and timer state`() = runTest {
        val vm = createViewModel()
        vm.selectHabit("qigong")
        vm.startTimer()
        vm.switchToReview()

        val state = vm.uiState.value
        assertThat(state.layout).isEqualTo(Layout.REVIEW)
        assertThat(state.selectedHabitId).isEqualTo("qigong")
        assertThat(state.timerRunning).isTrue()
    }

    @Test
    fun `selectHabit works after switchToReview and switchToMain`() = runTest {
        val vm = createViewModel()
        vm.selectHabit("qigong")
        vm.switchToReview()
        vm.switchToMain()
        vm.selectHabit("vitamins")

        assertThat(vm.uiState.value.selectedHabitId).isEqualTo("vitamins")
    }

    @Test
    fun `selectHabit allowed when timer running on same habit`() = runTest {
        val vm = createViewModel()
        vm.selectHabit("qigong")
        vm.startTimer()
        vm.selectHabit("qigong")

        assertThat(vm.uiState.value.selectedHabitId).isEqualTo("qigong")
    }

    @Test
    fun `expandActivity sets activity focused layout and loads history`() = runTest {
        coEvery { activityRepo.completedHistoryForHabit("qigong") } returns emptyList()
        val vm = createViewModel()
        vm.selectHabit("qigong")
        vm.expandActivity()

        val state = vm.uiState.value
        assertThat(state.layout).isEqualTo(Layout.ACTIVITY_FOCUSED)
        assertThat(state.historyActivities).isNotEmpty()
        assertThat(state.previousLayout).isEqualTo(Layout.MAIN)
    }

    @Test
    fun `collapseActivity returns to previous layout`() = runTest {
        coEvery { activityRepo.completedHistoryForHabit("qigong") } returns emptyList()
        val vm = createViewModel()
        vm.selectHabit("qigong")
        vm.switchToReview()
        vm.expandActivity()
        assertThat(vm.uiState.value.layout).isEqualTo(Layout.ACTIVITY_FOCUSED)

        vm.collapseActivity()
        assertThat(vm.uiState.value.layout).isEqualTo(Layout.REVIEW)
        assertThat(vm.uiState.value.historyActivities).isEmpty()
    }

    @Test
    fun `historyOlder and historyNewer navigate history`() = runTest {
        val completed1 = Activity(
            id = 10, habitId = "qigong", attributedDate = today,
            startTime = null, note = "first", completedAt = java.time.Instant.now()
        )
        val completed2 = Activity(
            id = 11, habitId = "qigong", attributedDate = today,
            startTime = null, note = "second", completedAt = java.time.Instant.now()
        )
        coEvery { activityRepo.completedHistoryForHabit("qigong") } returns
            listOf(completed1, completed2)

        val vm = createViewModel()
        vm.selectHabit("qigong")
        vm.expandActivity()

        // starts at the newest (in-progress activity at index 2)
        val initialIndex = vm.uiState.value.historyIndex
        assertThat(initialIndex).isEqualTo(vm.uiState.value.historyActivities.lastIndex)

        vm.historyOlder()
        assertThat(vm.uiState.value.historyIndex).isEqualTo(initialIndex - 1)

        vm.historyNewer()
        assertThat(vm.uiState.value.historyIndex).isEqualTo(initialIndex)
    }

    @Test
    fun `historyBackToAnchor returns to anchor index`() = runTest {
        val completed1 = Activity(
            id = 10, habitId = "qigong", attributedDate = today,
            startTime = null, note = "first", completedAt = java.time.Instant.now()
        )
        coEvery { activityRepo.completedHistoryForHabit("qigong") } returns
            listOf(completed1)

        val vm = createViewModel()
        vm.selectHabit("qigong")
        vm.expandActivity()

        val anchorIndex = vm.uiState.value.historyAnchorIndex
        vm.historyOlder()
        assertThat(vm.uiState.value.historyIndex).isNotEqualTo(anchorIndex)

        vm.historyBackToAnchor()
        assertThat(vm.uiState.value.historyIndex).isEqualTo(anchorIndex)
    }

    @Test
    fun `hasSwipedFromAnchor is true after navigation`() = runTest {
        val completed1 = Activity(
            id = 10, habitId = "qigong", attributedDate = today,
            startTime = null, note = "first", completedAt = java.time.Instant.now()
        )
        coEvery { activityRepo.completedHistoryForHabit("qigong") } returns
            listOf(completed1)

        val vm = createViewModel()
        vm.selectHabit("qigong")
        vm.expandActivity()

        assertThat(vm.uiState.value.hasSwipedFromAnchor).isFalse()
        vm.historyOlder()
        assertThat(vm.uiState.value.hasSwipedFromAnchor).isTrue()
    }

    @Test
    fun `selectCompletedActivity sets selectedActivityId`() = runTest {
        val completed = Activity(
            id = 99, habitId = "vitamins", attributedDate = today,
            startTime = null, note = "", completedAt = java.time.Instant.now()
        )
        activitiesFlow.value = listOf(completed)

        val vm = createViewModel()
        vm.selectCompletedActivity(99)

        assertThat(vm.uiState.value.selectedActivityId).isEqualTo(99)
        assertThat(vm.uiState.value.selectedHabitId).isEqualTo("vitamins")
    }

    @Test
    fun `skipActivity deletes in-progress activity and clears selection`() = runTest {
        val vm = createViewModel()
        vm.selectHabit("qigong")
        assertThat(vm.uiState.value.activeActivity).isNotNull()

        vm.skipActivity()

        assertThat(vm.uiState.value.selectedHabitId).isNull()
        assertThat(vm.uiState.value.activeActivity).isNull()
        assertThat(vm.uiState.value.layout).isEqualTo(Layout.MAIN)
        coVerify { activityRepo.delete(any()) }
    }

    @Test
    fun `skipActivity does nothing when no active activity`() = runTest {
        val vm = createViewModel()
        // no habit selected — no activeActivity
        vm.skipActivity()
        coVerify(exactly = 0) { activityRepo.delete(any()) }
    }

    @Test
    fun `today field uses dayBoundary not system clock`() = runTest {
        val vm = createViewModel()
        assertThat(vm.uiState.value.today).isEqualTo(today)
    }

    @Test
    fun `totalTarget uses dayBoundary today for day filtering`() = runTest {
        // today is Monday — both habits are active on Monday
        val vm = createViewModel()
        // qigong target=2, vitamins target=1 → 3
        assertThat(vm.uiState.value.totalTarget).isEqualTo(3)
    }

    @Test
    fun `otherHabits excludes habits on agenda`() = runTest {
        val vm = createViewModel()
        val otherIds = vm.uiState.value.otherHabits.map { it.id }
        val agendaIds = vm.uiState.value.agendaItems.map { it.habit.id }
        // no overlap
        assertThat(otherIds).containsNoneIn(agendaIds)
    }

    @Test
    fun `otherHabits excludes exactly habits that met target`() = runTest {
        // complete vitamins (EXACTLY, target=1)
        activitiesFlow.value = listOf(
            Activity(
                id = 1, habitId = "vitamins", attributedDate = today,
                startTime = null, note = "", completedAt = Instant.now()
            )
        )

        val vm = createViewModel()
        val otherIds = vm.uiState.value.otherHabits.map { it.id }
        assertThat(otherIds).doesNotContain("vitamins")
    }

    @Test
    fun `otherHabits includes at-least habits that met target`() = runTest {
        // complete qigong twice (AT_LEAST, target=2) — target met
        activitiesFlow.value = listOf(
            Activity(
                id = 1, habitId = "qigong", attributedDate = today,
                startTime = null, note = "", completedAt = Instant.now()
            ),
            Activity(
                id = 2, habitId = "qigong", attributedDate = today,
                startTime = null, note = "", completedAt = Instant.now()
            )
        )

        val vm = createViewModel()
        val otherIds = vm.uiState.value.otherHabits.map { it.id }
        assertThat(otherIds).contains("qigong")
    }
}
