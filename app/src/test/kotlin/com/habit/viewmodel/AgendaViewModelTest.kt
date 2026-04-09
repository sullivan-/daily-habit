package com.habit.viewmodel

import com.google.common.truth.Truth.assertThat
import com.habit.data.Activity
import com.habit.data.ActivityRepository
import com.habit.data.DayBoundary
import com.habit.data.Habit
import com.habit.data.HabitRepository
import com.habit.data.Milestone
import com.habit.data.Priority
import com.habit.data.TargetMode
import com.habit.data.Track
import com.habit.data.TrackRepository

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
    private val trackRepo = mockk<TrackRepository>(relaxed = true)

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

    private val habitsFlow = MutableStateFlow(listOf(qigong, vitamins))
    private val activitiesFlow = MutableStateFlow<List<Activity>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        every { dayBoundary.today() } returns today
        every { dayBoundary.attributedDate(any()) } returns today
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
        habitRepo, activityRepo, dayBoundary,
        tickDispatcher = tickDispatcher
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
    fun `updateActivityStartTime on active activity updates activeActivity`() = runTest {
        coEvery { activityRepo.completedHistoryForHabit("qigong") } returns emptyList()
        val vm = createViewModel()
        vm.selectHabit("qigong")
        vm.startTimer()
        vm.expandActivity()

        val activity = vm.uiState.value.activeActivity!!
        val newStart = Instant.now().minusSeconds(600)
        vm.updateActivityStartTime(activity.id, newStart)

        assertThat(vm.uiState.value.activeActivity!!.startTime).isEqualTo(newStart)
        coVerify { activityRepo.update(match { it.startTime == newStart }) }
    }

    @Test
    fun `updateActivityStartTime on history activity updates history list`() = runTest {
        val completed = Activity(
            id = 10, habitId = "qigong", attributedDate = today,
            startTime = Instant.now().minusSeconds(3600),
            note = "", completedAt = Instant.now()
        )
        coEvery { activityRepo.completedHistoryForHabit("qigong") } returns listOf(completed)

        val vm = createViewModel()
        vm.selectHabit("qigong")
        vm.expandActivity()

        val newStart = Instant.now().minusSeconds(1800)
        vm.updateActivityStartTime(10, newStart)

        val updated = vm.uiState.value.historyActivities.first { it.id == 10L }
        assertThat(updated.startTime).isEqualTo(newStart)
        coVerify { activityRepo.update(match { it.id == 10L && it.startTime == newStart }) }
    }

    @Test
    fun `updateActivityCompletedAt on completed activity updates history and attributed date`() = runTest {
        val completed = Activity(
            id = 10, habitId = "qigong", attributedDate = today,
            startTime = Instant.now().minusSeconds(3600),
            note = "", completedAt = Instant.now()
        )
        coEvery { activityRepo.completedHistoryForHabit("qigong") } returns listOf(completed)

        val vm = createViewModel()
        vm.selectHabit("qigong")
        vm.expandActivity()

        val newCompleted = Instant.now().minusSeconds(600)
        vm.updateActivityCompletedAt(10, newCompleted)

        val updated = vm.uiState.value.historyActivities.first { it.id == 10L }
        assertThat(updated.completedAt).isEqualTo(newCompleted)
        coVerify { activityRepo.update(match { it.id == 10L && it.completedAt == newCompleted }) }
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

    // --- track-related tests ---

    private fun createViewModelWithTracks() = AgendaViewModel(
        habitRepo, activityRepo, dayBoundary,
        trackRepo = trackRepo,
        tickDispatcher = tickDispatcher
    )

    @Test
    fun `loadTracksForHabit populates availableTracks sorted by day default then priority`() = runTest {
        // today is Monday
        val mondayTrack = Track(
            id = "monday-form", habitId = "qigong", name = "Monday Form",
            priority = Priority.MEDIUM, dayOfWeek = DayOfWeek.MONDAY
        )
        val highPriorityTrack = Track(
            id = "standing", habitId = "qigong", name = "Standing",
            priority = Priority.HIGH
        )
        val lowPriorityTrack = Track(
            id = "seated", habitId = "qigong", name = "Seated",
            priority = Priority.LOW
        )
        coEvery { trackRepo.activeTracksForHabit("qigong") } returns
            listOf(lowPriorityTrack, highPriorityTrack, mondayTrack)

        val vm = createViewModelWithTracks()
        vm.loadTracksForHabit("qigong")

        val tracks = vm.uiState.value.availableTracks
        assertThat(tracks).hasSize(3)
        // monday track first (day default match), then high priority, then low
        assertThat(tracks[0].id).isEqualTo("monday-form")
        assertThat(tracks[1].id).isEqualTo("standing")
        assertThat(tracks[2].id).isEqualTo("seated")
    }

    @Test
    fun `selectTrack updates activity trackId and loads milestone`() = runTest {
        val track = Track(
            id = "standing", habitId = "qigong", name = "Standing",
            priority = Priority.HIGH
        )
        val milestone = Milestone(
            id = 1, trackId = "standing", name = "Lesson 1",
            sortOrder = 1, completed = false
        )
        val milestone2 = Milestone(
            id = 2, trackId = "standing", name = "Lesson 2",
            sortOrder = 2, completed = false
        )
        coEvery { trackRepo.getById("standing") } returns track
        coEvery { trackRepo.defaultMilestone("standing") } returns milestone
        coEvery { trackRepo.incompleteMilestones("standing") } returns
            listOf(milestone, milestone2)

        val vm = createViewModelWithTracks()
        vm.selectHabit("qigong")
        vm.selectTrack("standing")

        val state = vm.uiState.value
        assertThat(state.activeActivity?.trackId).isEqualTo("standing")
        assertThat(state.activeActivity?.milestoneId).isEqualTo(1)
        assertThat(state.selectedTrack).isEqualTo(track)
        assertThat(state.selectedMilestone).isEqualTo(milestone)
        assertThat(state.incompleteMilestones).hasSize(2)
    }

    @Test
    fun `selectTrack null clears track and milestone`() = runTest {
        val track = Track(
            id = "standing", habitId = "qigong", name = "Standing",
            priority = Priority.HIGH
        )
        coEvery { trackRepo.getById("standing") } returns track
        coEvery { trackRepo.defaultMilestone("standing") } returns null
        coEvery { trackRepo.incompleteMilestones("standing") } returns emptyList()

        val vm = createViewModelWithTracks()
        vm.selectHabit("qigong")
        vm.selectTrack("standing")
        vm.selectTrack(null)

        val state = vm.uiState.value
        assertThat(state.activeActivity?.trackId).isNull()
        assertThat(state.activeActivity?.milestoneId).isNull()
        assertThat(state.selectedTrack).isNull()
        assertThat(state.selectedMilestone).isNull()
        assertThat(state.incompleteMilestones).isEmpty()
    }

    @Test
    fun `habits with no tracks have empty availableTracks`() = runTest {
        coEvery { trackRepo.activeTracksForHabit("vitamins") } returns emptyList()

        val vm = createViewModelWithTracks()
        vm.loadTracksForHabit("vitamins")

        assertThat(vm.uiState.value.availableTracks).isEmpty()
    }

    @Test
    fun `selectMilestone allows picking any incomplete milestone`() = runTest {
        val track = Track(
            id = "standing", habitId = "qigong", name = "Standing",
            priority = Priority.HIGH
        )
        val milestone1 = Milestone(
            id = 1, trackId = "standing", name = "Lesson 1",
            sortOrder = 1, completed = false
        )
        val milestone2 = Milestone(
            id = 2, trackId = "standing", name = "Lesson 2",
            sortOrder = 2, completed = false
        )
        coEvery { trackRepo.getById("standing") } returns track
        coEvery { trackRepo.defaultMilestone("standing") } returns milestone1
        coEvery { trackRepo.incompleteMilestones("standing") } returns
            listOf(milestone1, milestone2)
        coEvery { trackRepo.getMilestoneById(2) } returns milestone2

        val vm = createViewModelWithTracks()
        vm.selectHabit("qigong")
        vm.selectTrack("standing")
        vm.selectMilestone(2)

        val state = vm.uiState.value
        assertThat(state.selectedMilestone).isEqualTo(milestone2)
        assertThat(state.activeActivity?.milestoneId).isEqualTo(2)
    }

    @Test
    fun `completeMilestone marks done and advances to next`() = runTest {
        val track = Track(
            id = "standing", habitId = "qigong", name = "Standing",
            priority = Priority.HIGH
        )
        val milestone1 = Milestone(
            id = 1, trackId = "standing", name = "Lesson 1",
            sortOrder = 1, completed = false
        )
        val milestone2 = Milestone(
            id = 2, trackId = "standing", name = "Lesson 2",
            sortOrder = 2, completed = false
        )
        coEvery { trackRepo.getById("standing") } returns track
        // first call from selectTrack returns milestone1; second call from completeMilestone
        // returns milestone2
        coEvery { trackRepo.defaultMilestone("standing") } returnsMany
            listOf(milestone1, milestone2)
        coEvery { trackRepo.incompleteMilestones("standing") } returnsMany
            listOf(listOf(milestone1, milestone2), listOf(milestone2))

        val vm = createViewModelWithTracks()
        vm.selectHabit("qigong")
        vm.selectTrack("standing")
        vm.completeMilestone()

        coVerify {
            trackRepo.updateMilestone(milestone1.copy(completed = true))
        }
        val state = vm.uiState.value
        assertThat(state.selectedMilestone).isEqualTo(milestone2)
        assertThat(state.activeActivity?.milestoneId).isEqualTo(2)
        assertThat(state.incompleteMilestones).hasSize(1)
    }

    @Test
    fun `completeMilestone with last milestone leaves selectedMilestone null`() = runTest {
        val track = Track(
            id = "standing", habitId = "qigong", name = "Standing",
            priority = Priority.HIGH
        )
        val milestone = Milestone(
            id = 1, trackId = "standing", name = "Lesson 1",
            sortOrder = 1, completed = false
        )
        coEvery { trackRepo.getById("standing") } returns track
        // first call from selectTrack returns the milestone; second call from
        // completeMilestone returns null (all done)
        coEvery { trackRepo.defaultMilestone("standing") } returnsMany
            listOf(milestone, null)
        coEvery { trackRepo.incompleteMilestones("standing") } returnsMany
            listOf(listOf(milestone), emptyList())

        val vm = createViewModelWithTracks()
        vm.selectHabit("qigong")
        vm.selectTrack("standing")
        vm.completeMilestone()

        coVerify {
            trackRepo.updateMilestone(milestone.copy(completed = true))
        }
        val state = vm.uiState.value
        assertThat(state.selectedMilestone).isNull()
        assertThat(state.activeActivity?.milestoneId).isNull()
        assertThat(state.incompleteMilestones).isEmpty()
    }
}
