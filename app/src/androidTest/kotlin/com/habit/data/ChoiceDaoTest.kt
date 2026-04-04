package com.habit.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChoiceDaoTest {

    private lateinit var database: HabitDatabase
    private lateinit var choiceDao: ChoiceDao
    private lateinit var tallyDao: TallyDao
    private var tallyId: Long = 0

    private val now = Instant.now()

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        choiceDao = database.choiceDao()
        tallyDao = database.tallyDao()
        tallyId = tallyDao.insert(Tally(name = "Sweets", priority = Priority.HIGH))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndQuery() = runTest {
        choiceDao.insert(Choice(tallyId = tallyId, timestamp = now, abstained = true))
        val choices = choiceDao.recentChoices(tallyId, 10)
        assertEquals(1, choices.size)
        assertEquals(true, choices[0].abstained)
    }

    @Test
    fun recentChoicesRespectsLimit() = runTest {
        repeat(5) { i ->
            choiceDao.insert(
                Choice(
                    tallyId = tallyId,
                    timestamp = now.minusSeconds(i * 60L),
                    abstained = true
                )
            )
        }
        val choices = choiceDao.recentChoices(tallyId, 3)
        assertEquals(3, choices.size)
    }

    @Test
    fun recentChoicesOrderedByTimestampDesc() = runTest {
        choiceDao.insert(
            Choice(tallyId = tallyId, timestamp = now.minusSeconds(120), abstained = true)
        )
        choiceDao.insert(
            Choice(tallyId = tallyId, timestamp = now, abstained = false)
        )
        choiceDao.insert(
            Choice(tallyId = tallyId, timestamp = now.minusSeconds(60), abstained = true)
        )

        val choices = choiceDao.recentChoices(tallyId, 10)
        assertEquals(3, choices.size)
        assertEquals(false, choices[0].abstained) // most recent (now)
        assertEquals(true, choices[2].abstained)  // oldest
    }

    @Test
    fun choiceCountsSinceGroupsByTally() = runTest {
        val tallyId2 = tallyDao.insert(
            Tally(name = "Nicotine", priority = Priority.LOW)
        )

        choiceDao.insert(Choice(tallyId = tallyId, timestamp = now, abstained = true))
        choiceDao.insert(
            Choice(tallyId = tallyId, timestamp = now.minusSeconds(60), abstained = false)
        )
        choiceDao.insert(
            Choice(tallyId = tallyId2, timestamp = now, abstained = true)
        )

        val since = now.minusSeconds(3600).toEpochMilli()
        val counts = choiceDao.choiceCountsSince(since)
        assertEquals(2, counts.size)

        val sweetsCount = counts.first { it.tallyId == tallyId }
        assertEquals(2, sweetsCount.count)

        val nicotineCount = counts.first { it.tallyId == tallyId2 }
        assertEquals(1, nicotineCount.count)
    }

    @Test
    fun choiceCountsSinceExcludesOldChoices() = runTest {
        val oneHourAgo = now.minusSeconds(3600)
        val twoHoursAgo = now.minusSeconds(7200)

        choiceDao.insert(
            Choice(tallyId = tallyId, timestamp = now, abstained = true)
        )
        choiceDao.insert(
            Choice(tallyId = tallyId, timestamp = twoHoursAgo, abstained = true)
        )

        val counts = choiceDao.choiceCountsSince(oneHourAgo.toEpochMilli())
        assertEquals(1, counts.size)
        assertEquals(1, counts[0].count)
    }

    @Test
    fun choicesInRangeFiltersCorrectly() = runTest {
        val start = now.minusSeconds(3600)
        val end = now.plusSeconds(1)

        choiceDao.insert(
            Choice(tallyId = tallyId, timestamp = now, abstained = true)
        )
        choiceDao.insert(
            Choice(
                tallyId = tallyId,
                timestamp = now.minusSeconds(7200),
                abstained = false
            )
        )

        val inRange = choiceDao.choicesInRange(
            tallyId,
            start.toEpochMilli(),
            end.toEpochMilli()
        )
        assertEquals(1, inRange.size)
        assertEquals(true, inRange[0].abstained)
    }
}
