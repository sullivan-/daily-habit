package com.habit.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackDaoTest {

    private lateinit var database: HabitDatabase
    private lateinit var habitDao: HabitDao
    private lateinit var trackDao: TrackDao
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

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        habitDao = database.habitDao()
        trackDao = database.trackDao()
        activityDao = database.activityDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndQueryTracksForHabit() = runTest {
        habitDao.insert(habit)
        trackDao.insert(
            Track(id = "standing", habitId = "qigong", name = "Standing", priority = Priority.HIGH)
        )
        trackDao.insert(
            Track(id = "seated", habitId = "qigong", name = "Seated", priority = Priority.MEDIUM)
        )

        val tracks = trackDao.tracksForHabit("qigong").first()
        assertEquals(2, tracks.size)
        // ordered by archived then name
        assertEquals("Seated", tracks[0].name)
        assertEquals("Standing", tracks[1].name)
    }

    @Test
    fun activeTracksExcludesArchived() = runTest {
        habitDao.insert(habit)
        trackDao.insert(
            Track(id = "standing", habitId = "qigong", name = "Standing", priority = Priority.HIGH)
        )
        trackDao.insert(
            Track(
                id = "seated", habitId = "qigong", name = "Seated",
                priority = Priority.MEDIUM, archived = true
            )
        )

        val active = trackDao.activeTracksForHabit("qigong")
        assertEquals(1, active.size)
        assertEquals("Standing", active[0].name)
    }

    @Test
    fun updateTrack() = runTest {
        habitDao.insert(habit)
        trackDao.insert(
            Track(id = "standing", habitId = "qigong", name = "Standing", priority = Priority.HIGH)
        )
        trackDao.update(
            Track(
                id = "standing", habitId = "qigong", name = "Standing Form",
                priority = Priority.MEDIUM
            )
        )

        val found = trackDao.getById("standing")
        assertEquals("Standing Form", found!!.name)
        assertEquals(Priority.MEDIUM, found.priority)
    }

    @Test
    fun deleteTrack() = runTest {
        habitDao.insert(habit)
        trackDao.insert(
            Track(id = "standing", habitId = "qigong", name = "Standing", priority = Priority.HIGH)
        )
        trackDao.deleteById("standing")

        val found = trackDao.getById("standing")
        assertNull(found)
    }

    @Test
    fun cascadeDeleteWhenHabitDeleted() = runTest {
        habitDao.insert(habit)
        trackDao.insert(
            Track(id = "standing", habitId = "qigong", name = "Standing", priority = Priority.HIGH)
        )
        trackDao.insert(
            Track(id = "seated", habitId = "qigong", name = "Seated", priority = Priority.MEDIUM)
        )

        habitDao.deleteById("qigong")

        val tracks = trackDao.tracksForHabit("qigong").first()
        assertEquals(0, tracks.size)
    }

    @Test
    fun activityCountReturnsCorrectCount() = runTest {
        habitDao.insert(habit)
        trackDao.insert(
            Track(id = "standing", habitId = "qigong", name = "Standing", priority = Priority.HIGH)
        )

        assertEquals(0, trackDao.activityCount("standing"))

        activityDao.insert(
            Activity(
                habitId = "qigong",
                attributedDate = LocalDate.of(2026, 3, 30),
                startTime = null,
                note = "",
                completedAt = Instant.now(),
                trackId = "standing"
            )
        )
        activityDao.insert(
            Activity(
                habitId = "qigong",
                attributedDate = LocalDate.of(2026, 3, 30),
                startTime = null,
                note = "",
                completedAt = Instant.now(),
                trackId = "standing"
            )
        )

        assertEquals(2, trackDao.activityCount("standing"))
    }
}
