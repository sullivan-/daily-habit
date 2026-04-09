package com.habit.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MilestoneDaoTest {

    private lateinit var database: HabitDatabase
    private lateinit var habitDao: HabitDao
    private lateinit var trackDao: TrackDao
    private lateinit var milestoneDao: MilestoneDao
    private lateinit var activityDao: ActivityDao

    private val habit = Habit(
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

    private val track = Track(
        id = "standing",
        habitId = "qigong",
        name = "Standing",
        priority = Priority.HIGH
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        habitDao = database.habitDao()
        trackDao = database.trackDao()
        milestoneDao = database.milestoneDao()
        activityDao = database.activityDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndQueryMilestonesForTrack() = runTest {
        habitDao.insert(habit)
        trackDao.insert(track)
        milestoneDao.insert(
            Milestone(trackId = "standing", name = "Lesson 1", sortOrder = 1)
        )
        milestoneDao.insert(
            Milestone(trackId = "standing", name = "Lesson 2", sortOrder = 2)
        )

        val milestones = milestoneDao.milestonesForTrack("standing")
        assertEquals(2, milestones.size)
        assertEquals("Lesson 1", milestones[0].name)
        assertEquals("Lesson 2", milestones[1].name)
    }

    @Test
    fun defaultMilestoneReturnsFirstIncomplete() = runTest {
        habitDao.insert(habit)
        trackDao.insert(track)
        milestoneDao.insert(
            Milestone(trackId = "standing", name = "Lesson 1", sortOrder = 1, completed = true)
        )
        milestoneDao.insert(
            Milestone(trackId = "standing", name = "Lesson 2", sortOrder = 2)
        )
        milestoneDao.insert(
            Milestone(trackId = "standing", name = "Lesson 3", sortOrder = 3)
        )

        val default = milestoneDao.defaultMilestone("standing")
        assertEquals("Lesson 2", default!!.name)
    }

    @Test
    fun defaultMilestoneReturnsNullWhenAllComplete() = runTest {
        habitDao.insert(habit)
        trackDao.insert(track)
        milestoneDao.insert(
            Milestone(trackId = "standing", name = "Lesson 1", sortOrder = 1, completed = true)
        )

        val default = milestoneDao.defaultMilestone("standing")
        assertNull(default)
    }

    @Test
    fun incompleteMilestonesReturnsAllIncomplete() = runTest {
        habitDao.insert(habit)
        trackDao.insert(track)
        milestoneDao.insert(
            Milestone(trackId = "standing", name = "Lesson 1", sortOrder = 1, completed = true)
        )
        milestoneDao.insert(
            Milestone(trackId = "standing", name = "Lesson 2", sortOrder = 2)
        )
        milestoneDao.insert(
            Milestone(trackId = "standing", name = "Lesson 3", sortOrder = 3)
        )

        val incomplete = milestoneDao.incompleteMilestones("standing")
        assertEquals(2, incomplete.size)
        assertEquals("Lesson 2", incomplete[0].name)
        assertEquals("Lesson 3", incomplete[1].name)
    }

    @Test
    fun maxSortOrder() = runTest {
        habitDao.insert(habit)
        trackDao.insert(track)
        milestoneDao.insert(
            Milestone(trackId = "standing", name = "Lesson 1", sortOrder = 1)
        )
        milestoneDao.insert(
            Milestone(trackId = "standing", name = "Lesson 2", sortOrder = 5)
        )
        milestoneDao.insert(
            Milestone(trackId = "standing", name = "Lesson 3", sortOrder = 3)
        )

        assertEquals(5, milestoneDao.maxSortOrder("standing"))
    }

    @Test
    fun maxSortOrderReturnsNullWhenEmpty() = runTest {
        habitDao.insert(habit)
        trackDao.insert(track)

        assertNull(milestoneDao.maxSortOrder("standing"))
    }

    @Test
    fun cascadeDeleteWhenTrackDeleted() = runTest {
        habitDao.insert(habit)
        trackDao.insert(track)
        milestoneDao.insert(
            Milestone(trackId = "standing", name = "Lesson 1", sortOrder = 1)
        )
        milestoneDao.insert(
            Milestone(trackId = "standing", name = "Lesson 2", sortOrder = 2)
        )

        trackDao.deleteById("standing")

        val milestones = milestoneDao.milestonesForTrack("standing")
        assertEquals(0, milestones.size)
    }

    @Test
    fun activityCountForMilestone() = runTest {
        habitDao.insert(habit)
        trackDao.insert(track)
        val milestoneId = milestoneDao.insert(
            Milestone(trackId = "standing", name = "Lesson 1", sortOrder = 1)
        )

        assertEquals(0, milestoneDao.activityCount(milestoneId))

        activityDao.insert(
            Activity(
                habitId = "qigong",
                attributedDate = LocalDate.of(2026, 3, 30),
                startTime = null,
                note = "",
                completedAt = Instant.now(),
                trackId = "standing",
                milestoneId = milestoneId
            )
        )

        assertEquals(1, milestoneDao.activityCount(milestoneId))
    }
}
