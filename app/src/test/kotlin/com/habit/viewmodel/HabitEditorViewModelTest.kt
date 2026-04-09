package com.habit.viewmodel

import com.google.common.truth.Truth.assertThat
import com.habit.data.Habit
import com.habit.data.HabitRepository
import com.habit.data.Milestone
import com.habit.data.Priority
import com.habit.data.TargetMode
import com.habit.data.TrackRepository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.DayOfWeek
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HabitEditorViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val habitRepo = mockk<HabitRepository>(relaxed = true)
    private val trackRepo = mockk<TrackRepository>(relaxed = true)

    private val existingHabit = Habit(
        id = "qigong",
        name = "Qigong",
        timesOfDay = listOf(7, 15),
        sortOrder = 1,
        daysActive = DayOfWeek.entries.toSet(),
        dailyTarget = 2,
        dailyTargetMode = TargetMode.AT_LEAST,
        timed = true,
        goalMinutes = 30,
        stopMinutes = null,
        priority = Priority.HIGH
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { habitRepo.getById("qigong") } returns existingHabit
        coEvery { habitRepo.allIds() } returns listOf("qigong", "vitamins")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = HabitEditorViewModel(habitRepo)

    @Test
    fun `initial state is new and valid defaults`() {
        val vm = createViewModel()
        val state = vm.state.value
        assertThat(state.isNew).isTrue()
        assertThat(state.dirty).isFalse()
        assertThat(state.timesOfDay).isEqualTo(listOf(8))
        assertThat(state.daysActive).isEqualTo(DayOfWeek.entries.toSet())
    }

    @Test
    fun `loadHabit populates state from repository`() = runTest {
        val vm = createViewModel()
        vm.loadHabit("qigong")

        val state = vm.state.value
        assertThat(state.isNew).isFalse()
        assertThat(state.id).isEqualTo("qigong")
        assertThat(state.name).isEqualTo("Qigong")
        assertThat(state.timesOfDay).isEqualTo(listOf(7, 15))
        assertThat(state.priority).isEqualTo(Priority.HIGH)
        assertThat(state.goalMinutes).isEqualTo(30)
        assertThat(state.dirty).isFalse()
    }

    @Test
    fun `loadHabit with unknown id does nothing`() = runTest {
        coEvery { habitRepo.getById("unknown") } returns null
        val vm = createViewModel()
        vm.loadHabit("unknown")
        assertThat(vm.state.value.isNew).isTrue()
    }

    @Test
    fun `setName updates name and marks dirty`() {
        val vm = createViewModel()
        vm.setName("Reading")
        assertThat(vm.state.value.name).isEqualTo("Reading")
        assertThat(vm.state.value.dirty).isTrue()
    }

    @Test
    fun `addTimeOfDay adds and sorts`() {
        val vm = createViewModel()
        vm.addTimeOfDay(14)
        assertThat(vm.state.value.timesOfDay).isEqualTo(listOf(8, 14))
        vm.addTimeOfDay(6)
        assertThat(vm.state.value.timesOfDay).isEqualTo(listOf(6, 8, 14))
    }

    @Test
    fun `addTimeOfDay rejects duplicates`() {
        val vm = createViewModel()
        vm.addTimeOfDay(8)
        assertThat(vm.state.value.timesOfDay).isEqualTo(listOf(8))
    }

    @Test
    fun `removeTimeOfDay removes but keeps at least one`() {
        val vm = createViewModel()
        vm.addTimeOfDay(14)
        assertThat(vm.state.value.timesOfDay).hasSize(2)

        vm.removeTimeOfDay(8)
        assertThat(vm.state.value.timesOfDay).isEqualTo(listOf(14))

        vm.removeTimeOfDay(14)
        assertThat(vm.state.value.timesOfDay).isEqualTo(listOf(14))
    }

    @Test
    fun `toggleDayActive toggles and keeps at least one`() {
        val vm = createViewModel()
        vm.toggleDayActive(DayOfWeek.MONDAY)
        assertThat(vm.state.value.daysActive).doesNotContain(DayOfWeek.MONDAY)

        vm.toggleDayActive(DayOfWeek.MONDAY)
        assertThat(vm.state.value.daysActive).contains(DayOfWeek.MONDAY)
    }

    @Test
    fun `toggleDayActive cannot remove last day`() {
        val vm = createViewModel()
        // remove all but one
        DayOfWeek.entries.drop(1).forEach { vm.toggleDayActive(it) }
        assertThat(vm.state.value.daysActive).hasSize(1)

        // try to remove the last one
        val lastDay = vm.state.value.daysActive.first()
        vm.toggleDayActive(lastDay)
        assertThat(vm.state.value.daysActive).hasSize(1)
    }

    @Test
    fun `setDailyTarget rejects values below 1`() {
        val vm = createViewModel()
        vm.setDailyTarget(3)
        assertThat(vm.state.value.dailyTarget).isEqualTo(3)

        vm.setDailyTarget(0)
        assertThat(vm.state.value.dailyTarget).isEqualTo(3)

        vm.setDailyTarget(-1)
        assertThat(vm.state.value.dailyTarget).isEqualTo(3)
    }

    @Test
    fun `setTimed clears goal and stop fields when false`() {
        val vm = createViewModel()
        vm.setTimed(true)
        vm.setGoalMinutes(30)
        assertThat(vm.state.value.goalMinutes).isEqualTo(30)

        vm.setTimed(false)
        assertThat(vm.state.value.goalMinutes).isNull()
        assertThat(vm.state.value.stopMinutes).isNull()
    }

    @Test
    fun `setGoalMinutes sets goal`() {
        val vm = createViewModel()
        vm.setTimed(true)

        vm.setGoalMinutes(15)
        assertThat(vm.state.value.goalMinutes).isEqualTo(15)
    }

    @Test
    fun `setGoalMinutes null clears goal`() {
        val vm = createViewModel()
        vm.setTimed(true)
        vm.setGoalMinutes(15)
        vm.setGoalMinutes(null)
        assertThat(vm.state.value.goalMinutes).isNull()
    }

    @Test
    fun `setStopMinutes sets stop`() {
        val vm = createViewModel()
        vm.setTimed(true)

        vm.setStopMinutes(45)
        assertThat(vm.state.value.stopMinutes).isEqualTo(45)
    }

    @Test
    fun `setStopMinutes null clears stop`() {
        val vm = createViewModel()
        vm.setTimed(true)
        vm.setStopMinutes(45)
        vm.setStopMinutes(null)
        assertThat(vm.state.value.stopMinutes).isNull()
    }

    @Test
    fun `isValid requires name timesOfDay daysActive and dailyTarget`() {
        val vm = createViewModel()
        // default state has timesOfDay=[8], daysActive=all, dailyTarget=1 but name=""
        assertThat(vm.state.value.isValid).isFalse()

        vm.setName("Test")
        assertThat(vm.state.value.isValid).isTrue()
    }

    @Test
    fun `isValid false with blank name`() {
        val vm = createViewModel()
        vm.setName("   ")
        assertThat(vm.state.value.isValid).isFalse()
    }

    @Test
    fun `save inserts new habit with generated id`() = runTest {
        val vm = createViewModel()
        vm.setName("Meditation")
        vm.save()

        coVerify { habitRepo.insert(match { it.id == "meditation" && it.name == "Meditation" }) }
        assertThat(vm.state.value.saved).isTrue()
        assertThat(vm.state.value.dirty).isFalse()
    }

    @Test
    fun `save generates unique id on conflict`() = runTest {
        val vm = createViewModel()
        vm.setName("Qigong") // "qigong" already exists
        vm.save()

        coVerify { habitRepo.insert(match { it.id == "qigong-2" }) }
    }

    @Test
    fun `save updates existing habit`() = runTest {
        val vm = createViewModel()
        vm.loadHabit("qigong")
        vm.setName("Qigong Updated")
        vm.save()

        coVerify { habitRepo.update(match { it.id == "qigong" && it.name == "Qigong Updated" }) }
        assertThat(vm.state.value.saved).isTrue()
    }

    @Test
    fun `save does nothing when invalid`() = runTest {
        val vm = createViewModel()
        // name is empty, so invalid
        vm.save()

        coVerify(exactly = 0) { habitRepo.insert(any()) }
        coVerify(exactly = 0) { habitRepo.update(any()) }
        assertThat(vm.state.value.saved).isFalse()
    }

    @Test
    fun `delete marks as deleted for existing habit`() = runTest {
        val vm = createViewModel()
        vm.loadHabit("qigong")
        vm.delete()

        coVerify { habitRepo.deleteById("qigong") }
        assertThat(vm.state.value.deleted).isTrue()
    }

    @Test
    fun `delete does nothing for new habit`() = runTest {
        val vm = createViewModel()
        vm.delete()

        coVerify(exactly = 0) { habitRepo.deleteById(any()) }
        assertThat(vm.state.value.deleted).isFalse()
    }

    @Test
    fun `dirty flag set on any field change`() {
        val vm = createViewModel()
        assertThat(vm.state.value.dirty).isFalse()

        vm.setName("X")
        assertThat(vm.state.value.dirty).isTrue()
    }

    @Test
    fun `dirty flag cleared on save`() = runTest {
        val vm = createViewModel()
        vm.setName("Test")
        assertThat(vm.state.value.dirty).isTrue()

        vm.save()
        assertThat(vm.state.value.dirty).isFalse()
    }

    // --- track-related tests ---

    private fun createViewModelWithTracks() = HabitEditorViewModel(
        habitRepo, trackRepo = trackRepo
    )

    @Test
    fun `addTrack adds an expanded new item`() {
        val vm = createViewModelWithTracks()
        vm.addTrack()

        val tracks = vm.state.value.tracks
        assertThat(tracks).hasSize(1)
        assertThat(tracks[0].isNew).isTrue()
        assertThat(tracks[0].expanded).isTrue()
        assertThat(tracks[0].name).isEmpty()
        assertThat(tracks[0].priority).isEqualTo(Priority.MEDIUM)
        assertThat(vm.state.value.dirty).isTrue()
    }

    @Test
    fun `archiveTrack sets archived and collapses`() {
        val vm = createViewModelWithTracks()
        vm.addTrack()
        vm.archiveTrack(0)

        val track = vm.state.value.tracks[0]
        assertThat(track.archived).isTrue()
        assertThat(track.expanded).isFalse()
    }

    @Test
    fun `unarchiveTrack clears archived flag`() {
        val vm = createViewModelWithTracks()
        vm.addTrack()
        vm.archiveTrack(0)
        assertThat(vm.state.value.tracks[0].archived).isTrue()

        vm.unarchiveTrack(0)
        assertThat(vm.state.value.tracks[0].archived).isFalse()
    }

    @Test
    fun `deleteTrack removes from list`() {
        val vm = createViewModelWithTracks()
        vm.addTrack()
        vm.addTrack()
        assertThat(vm.state.value.tracks).hasSize(2)

        vm.deleteTrack(0)
        assertThat(vm.state.value.tracks).hasSize(1)
        assertThat(vm.state.value.dirty).isTrue()
    }

    @Test
    fun `addMilestone appends with correct sort order`() {
        val vm = createViewModelWithTracks()
        vm.addTrack()
        vm.addMilestone(0, "Lesson 1")
        vm.addMilestone(0, "Lesson 2")

        val milestones = vm.state.value.tracks[0].milestones
        assertThat(milestones).hasSize(2)
        assertThat(milestones[0].name).isEqualTo("Lesson 1")
        assertThat(milestones[0].sortOrder).isEqualTo(1)
        assertThat(milestones[1].name).isEqualTo("Lesson 2")
        assertThat(milestones[1].sortOrder).isEqualTo(2)
        assertThat(vm.state.value.dirty).isTrue()
    }

    @Test
    fun `deleteMilestone removes from list`() {
        val vm = createViewModelWithTracks()
        vm.addTrack()
        vm.addMilestone(0, "Lesson 1")
        vm.addMilestone(0, "Lesson 2")
        assertThat(vm.state.value.tracks[0].milestones).hasSize(2)

        vm.deleteMilestone(0, 0)

        val milestones = vm.state.value.tracks[0].milestones
        assertThat(milestones).hasSize(1)
        assertThat(milestones[0].name).isEqualTo("Lesson 2")
        assertThat(vm.state.value.dirty).isTrue()
    }
}
