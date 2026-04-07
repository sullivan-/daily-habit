package com.habit.viewmodel

import com.google.common.truth.Truth.assertThat
import com.habit.data.Priority
import com.habit.data.Tally
import com.habit.data.TallyRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
class TallyEditorViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val tallyRepo = mockk<TallyRepository>(relaxed = true)

    private val existingTally = Tally(
        id = "1",
        name = "Sweets",
        priority = Priority.HIGH
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { tallyRepo.getById("1") } returns existingTally
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = TallyEditorViewModel(tallyRepo)

    @Test
    fun `initial state is new with default priority`() {
        val vm = createViewModel()
        val state = vm.state.value
        assertThat(state.isNew).isTrue()
        assertThat(state.priority).isEqualTo(Priority.MEDIUM)
        assertThat(state.dirty).isFalse()
        assertThat(state.isValid).isFalse()
    }

    @Test
    fun `setName updates name and marks dirty`() {
        val vm = createViewModel()
        vm.setName("Nicotine")
        assertThat(vm.state.value.name).isEqualTo("Nicotine")
        assertThat(vm.state.value.dirty).isTrue()
    }

    @Test
    fun `setPriority updates priority and marks dirty`() {
        val vm = createViewModel()
        vm.setPriority(Priority.LOW)
        assertThat(vm.state.value.priority).isEqualTo(Priority.LOW)
        assertThat(vm.state.value.dirty).isTrue()
    }

    @Test
    fun `isValid is false with blank name`() {
        val vm = createViewModel()
        vm.setName("   ")
        assertThat(vm.state.value.isValid).isFalse()
    }

    @Test
    fun `isValid is true with non-blank name`() {
        val vm = createViewModel()
        vm.setName("Sweets")
        assertThat(vm.state.value.isValid).isTrue()
    }

    @Test
    fun `save inserts new tally`() = runTest {
        val vm = createViewModel()
        vm.setName("Nicotine")
        vm.setPriority(Priority.HIGH)
        vm.save()

        coVerify {
            tallyRepo.insert(match {
                it.name == "Nicotine" && it.priority == Priority.HIGH
            })
        }
        assertThat(vm.state.value.saved).isTrue()
        assertThat(vm.state.value.dirty).isFalse()
    }

    @Test
    fun `save does nothing when invalid`() = runTest {
        val vm = createViewModel()
        vm.save()

        coVerify(exactly = 0) { tallyRepo.insert(any()) }
        coVerify(exactly = 0) { tallyRepo.update(any()) }
        assertThat(vm.state.value.saved).isFalse()
    }

    @Test
    fun `loadTally populates state from repository`() = runTest {
        val vm = createViewModel()
        vm.loadTally("1")

        val state = vm.state.value
        assertThat(state.isNew).isFalse()
        assertThat(state.id).isEqualTo("1")
        assertThat(state.name).isEqualTo("Sweets")
        assertThat(state.priority).isEqualTo(Priority.HIGH)
        assertThat(state.dirty).isFalse()
    }

    @Test
    fun `loadTally with unknown id does nothing`() = runTest {
        coEvery { tallyRepo.getById("99") } returns null
        val vm = createViewModel()
        vm.loadTally("99")
        assertThat(vm.state.value.isNew).isTrue()
    }

    @Test
    fun `save existing tally updates`() = runTest {
        val vm = createViewModel()
        vm.loadTally("1")
        vm.setName("Sweets Updated")
        vm.save()

        coVerify {
            tallyRepo.update(match {
                it.id == "1" && it.name == "Sweets Updated"
            })
        }
        assertThat(vm.state.value.saved).isTrue()
    }

    @Test
    fun `delete removes tally and sets deleted flag`() = runTest {
        val vm = createViewModel()
        vm.loadTally("1")
        vm.delete()

        coVerify { tallyRepo.deleteById("1") }
        assertThat(vm.state.value.deleted).isTrue()
    }

    @Test
    fun `delete does nothing for new tally`() = runTest {
        val vm = createViewModel()
        vm.delete()

        coVerify(exactly = 0) { tallyRepo.deleteById(any()) }
        assertThat(vm.state.value.deleted).isFalse()
    }
}
