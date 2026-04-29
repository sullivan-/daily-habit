package com.habit.viewmodel

import com.google.common.truth.Truth.assertThat
import com.habit.data.Choice
import com.habit.data.ChoiceRepository
import io.mockk.coEvery
import io.mockk.mockk
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class StreakCalculatorTest {

    private val choiceRepo = mockk<ChoiceRepository>()
    private val calculator = StreakCalculator(choiceRepo)
    private val tallyId = "test"

    private val now = Instant.now()
    private val oneHourAgo = now.minus(1, ChronoUnit.HOURS)
    private val twoDaysAgo = now.minus(2, ChronoUnit.DAYS)
    private val threeDaysAgo = now.minus(3, ChronoUnit.DAYS)
    private val fourDaysAgo = now.minus(4, ChronoUnit.DAYS)
    private val fiveDaysAgo = now.minus(5, ChronoUnit.DAYS)

    @Before
    fun setUp() {
        coEvery { choiceRepo.mostRecentChoice(tallyId) } returns null
        coEvery { choiceRepo.mostRecentIndulgence(tallyId) } returns null
        coEvery { choiceRepo.firstAbstentionAfter(tallyId, any()) } returns null
        coEvery { choiceRepo.firstAbstention(tallyId) } returns null
    }

    @Test
    fun `no choices returns null`() = runTest {
        assertThat(calculator.currentStreakStart(tallyId)).isNull()
    }

    @Test
    fun `only No choices returns timestamp of the first No`() = runTest {
        coEvery { choiceRepo.mostRecentChoice(tallyId) } returns
            Choice(1, tallyId, now, abstained = true)
        coEvery { choiceRepo.firstAbstention(tallyId) } returns
            Choice(3, tallyId, threeDaysAgo, abstained = true)

        assertThat(calculator.currentStreakStart(tallyId)).isEqualTo(threeDaysAgo)
    }

    @Test
    fun `only Yes choices returns null`() = runTest {
        coEvery { choiceRepo.mostRecentChoice(tallyId) } returns
            Choice(1, tallyId, now, abstained = false)

        assertThat(calculator.currentStreakStart(tallyId)).isNull()
    }

    @Test
    fun `Yes then No returns timestamp of the No`() = runTest {
        coEvery { choiceRepo.mostRecentChoice(tallyId) } returns
            Choice(2, tallyId, now, abstained = true)
        coEvery { choiceRepo.mostRecentIndulgence(tallyId) } returns
            Choice(1, tallyId, twoDaysAgo, abstained = false)
        coEvery { choiceRepo.firstAbstentionAfter(tallyId, twoDaysAgo) } returns
            Choice(2, tallyId, oneHourAgo, abstained = true)

        assertThat(calculator.currentStreakStart(tallyId)).isEqualTo(oneHourAgo)
    }

    @Test
    fun `Yes No No No returns timestamp of first No after Yes`() = runTest {
        coEvery { choiceRepo.mostRecentChoice(tallyId) } returns
            Choice(4, tallyId, now, abstained = true)
        coEvery { choiceRepo.mostRecentIndulgence(tallyId) } returns
            Choice(1, tallyId, fourDaysAgo, abstained = false)
        coEvery { choiceRepo.firstAbstentionAfter(tallyId, fourDaysAgo) } returns
            Choice(2, tallyId, threeDaysAgo, abstained = true)

        assertThat(calculator.currentStreakStart(tallyId)).isEqualTo(threeDaysAgo)
    }

    @Test
    fun `No No Yes No No returns timestamp of first No after last Yes`() = runTest {
        coEvery { choiceRepo.mostRecentChoice(tallyId) } returns
            Choice(5, tallyId, now, abstained = true)
        coEvery { choiceRepo.mostRecentIndulgence(tallyId) } returns
            Choice(3, tallyId, threeDaysAgo, abstained = false)
        coEvery { choiceRepo.firstAbstentionAfter(tallyId, threeDaysAgo) } returns
            Choice(4, tallyId, twoDaysAgo, abstained = true)

        assertThat(calculator.currentStreakStart(tallyId)).isEqualTo(twoDaysAgo)
    }

    @Test
    fun `most recent choice is Yes returns null even with earlier No runs`() = runTest {
        coEvery { choiceRepo.mostRecentChoice(tallyId) } returns
            Choice(3, tallyId, now, abstained = false)

        assertThat(calculator.currentStreakStart(tallyId)).isNull()
    }

    @Test
    fun `backdated No with no Yes returns backdated timestamp`() = runTest {
        val longAgo = now.minus(365 * 4, ChronoUnit.DAYS)
        coEvery { choiceRepo.mostRecentChoice(tallyId) } returns
            Choice(2, tallyId, now, abstained = true)
        coEvery { choiceRepo.firstAbstention(tallyId) } returns
            Choice(1, tallyId, longAgo, abstained = true)

        assertThat(calculator.currentStreakStart(tallyId)).isEqualTo(longAgo)
    }
}
