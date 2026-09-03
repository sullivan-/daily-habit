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
import org.junit.Assert.assertNotNull
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
        tieBreaker = 4,
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

    private suspend fun insertActivity(completedAt: Instant?): Long =
        activityDao.insert(
            Activity(
                habitId = "qigong", attributedDate = LocalDate.now(), startTime = null,
                note = "", completedAt = completedAt, trackId = "standing"
            )
        )

    private suspend fun insertTwoLessons(): Pair<Long, Long> {
        habitDao.insert(habit)
        trackDao.insert(track)
        val m1 = milestoneDao.insert(Milestone(trackId = "standing", name = "Lesson 1", sortOrder = 1))
        val m2 = milestoneDao.insert(Milestone(trackId = "standing", name = "Lesson 2", sortOrder = 2))
        return m1 to m2
    }

    @Test
    fun claimedMilestonesStayOpenUntilTheActivityCompletesThem() = runTest {
        val (m1, m2) = insertTwoLessons()
        val activityId = insertActivity(completedAt = null)

        milestoneDao.claim(m2, activityId, completed = false)
        milestoneDao.claim(m1, activityId, completed = false)

        val pending = milestoneDao.claimedBy(activityId)
        assertEquals(listOf("Lesson 1", "Lesson 2"), pending.map { it.name })
        assertEquals(false, pending[0].completed)
        assertEquals(2, milestoneDao.incompleteMilestones("standing").size)

        milestoneDao.completeClaimedBy(activityId)
        assertEquals(0, milestoneDao.incompleteMilestones("standing").size)
        assertEquals(activityId, milestoneDao.getById(m1)!!.activityId)
    }

    @Test
    fun claimOnAFinishedActivityCompletesAtOnce() = runTest {
        val (m1, _) = insertTwoLessons()
        val activityId = insertActivity(completedAt = Instant.now())

        milestoneDao.claim(m1, activityId, completed = true)

        val milestone = milestoneDao.getById(m1)!!
        assertEquals(true, milestone.completed)
        assertEquals(activityId, milestone.activityId)
    }

    @Test
    fun releaseClearsTheClaimAndTheCompletion() = runTest {
        val (m1, _) = insertTwoLessons()
        val activityId = insertActivity(completedAt = Instant.now())
        milestoneDao.claim(m1, activityId, completed = true)

        milestoneDao.release(m1)

        val milestone = milestoneDao.getById(m1)!!
        assertEquals(false, milestone.completed)
        assertNull(milestone.activityId)
    }

    @Test
    fun releaseClaimedByKeepsCompletedMilestonesCompleted() = runTest {
        val (m1, m2) = insertTwoLessons()
        val activityId = insertActivity(completedAt = null)
        milestoneDao.claim(m1, activityId, completed = true)
        milestoneDao.claim(m2, activityId, completed = false)

        milestoneDao.releaseClaimedBy(activityId)

        assertEquals(0, milestoneDao.claimedBy(activityId).size)
        assertEquals(true, milestoneDao.getById(m1)!!.completed)
        assertEquals(false, milestoneDao.getById(m2)!!.completed)
    }

    @Test
    fun deletingAnActivityDropsItsClaims() = runTest {
        val (m1, _) = insertTwoLessons()
        val activityId = insertActivity(completedAt = null)
        milestoneDao.claim(m1, activityId, completed = false)

        activityDao.deleteById(activityId)

        assertNull(milestoneDao.getById(m1)!!.activityId)
        assertEquals(0, milestoneDao.claimedBy(activityId).size)
    }

    @Test
    fun milestoneIdsWithHistoryIncludesMilestonesCompletedByAnActivity() = runTest {
        val (m1, m2) = insertTwoLessons()
        val activityId = insertActivity(completedAt = Instant.now())
        milestoneDao.claim(m1, activityId, completed = true)

        val withHistory = milestoneDao.milestoneIdsWithHistory(listOf(m1, m2))

        assertEquals(listOf(m1), withHistory)
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
            Milestone(trackId = "standing", name = "Lesson 1", sortOrder = 4, completed = true)
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
            Milestone(trackId = "standing", name = "Lesson 1", sortOrder = 4, completed = true)
        )

        val default = milestoneDao.defaultMilestone("standing")
        assertNull(default)
    }

    @Test
    fun incompleteMilestonesReturnsAllIncomplete() = runTest {
        habitDao.insert(habit)
        trackDao.insert(track)
        milestoneDao.insert(
            Milestone(trackId = "standing", name = "Lesson 1", sortOrder = 4, completed = true)
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

    @Test
    fun milestoneIdsWithHistoryReportsOnlyRecordedMilestones() = runTest {
        habitDao.insert(habit)
        trackDao.insert(track)
        val recorded = milestoneDao.insert(
            Milestone(trackId = "standing", name = "Lesson 1", sortOrder = 1)
        )
        val untouched = milestoneDao.insert(
            Milestone(trackId = "standing", name = "Lesson 2", sortOrder = 2)
        )
        activityDao.insert(
            activity(milestoneId = recorded, completedAt = Instant.now())
        )

        val withHistory = milestoneDao.milestoneIdsWithHistory(listOf(recorded, untouched))

        assertEquals(listOf(recorded), withHistory)
    }

    @Test
    fun detachPendingActivitiesKeepsUnfinishedActivityWhenMilestoneDeleted() = runTest {
        habitDao.insert(habit)
        trackDao.insert(track)
        val milestoneId = milestoneDao.insert(
            Milestone(trackId = "standing", name = "Lesson 1", sortOrder = 1)
        )
        val activityId = activityDao.insert(
            activity(milestoneId = milestoneId, completedAt = null)
        )

        milestoneDao.detachPendingActivities(milestoneId)
        milestoneDao.deleteById(milestoneId)

        val survivor = activityDao.getById(activityId)
        assertNotNull(survivor)
        assertNull(survivor!!.milestoneId)
    }

    private fun activity(milestoneId: Long, completedAt: Instant?) = Activity(
        habitId = "qigong",
        attributedDate = LocalDate.of(2026, 3, 30),
        startTime = null,
        note = "",
        completedAt = completedAt,
        trackId = "standing",
        milestoneId = milestoneId
    )
}
