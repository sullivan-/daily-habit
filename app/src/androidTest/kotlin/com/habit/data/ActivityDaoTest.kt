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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActivityDaoTest {

    private lateinit var database: HabitDatabase
    private lateinit var habitDao: HabitDao
    private lateinit var activityDao: ActivityDao

    private val today = LocalDate.of(2026, 3, 30)
    private val yesterday = LocalDate.of(2026, 3, 29)

    private val habit = Habit(
        id = "qigong",
        name = "Qigong",
        timesOfDay = listOf(7),
        sortOrder = 1,
        daysActive = DayOfWeek.entries.toSet(),
        dailyTarget = 2,
        dailyTargetMode = TargetMode.AT_LEAST,
        timed = true,
        chimeIntervalSeconds = 10,
        thresholdMinutes = 30,
        thresholdType = ThresholdType.GOAL,
        priority = Priority.HIGH,
        dailyTexts = emptyMap()
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        habitDao = database.habitDao()
        activityDao = database.activityDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun activity(
        habitId: String = "qigong",
        date: LocalDate = today,
        note: String = "",
        elapsedMs: Long = 0,
        completedAt: Instant? = null
    ) = Activity(
        habitId = habitId,
        attributedDate = date,
        startTime = if (completedAt != null) Instant.now() else null,
        endTime = completedAt,
        elapsedMs = elapsedMs,
        note = note,
        completedAt = completedAt
    )

    @Test
    fun insertAndQueryByDate() = runTest {
        habitDao.insertAll(listOf(habit))
        activityDao.insert(activity(note = "session 1"))
        activityDao.insert(activity(date = yesterday, note = "yesterday"))

        val todayActivities = activityDao.activitiesForDate(today).first()
        assertEquals(1, todayActivities.size)
        assertEquals("session 1", todayActivities[0].note)
    }

    @Test
    fun queryByHabitAndDate() = runTest {
        val other = habit.copy(id = "vitamins", name = "Vitamins")
        habitDao.insertAll(listOf(habit, other))
        activityDao.insert(activity(habitId = "qigong", note = "qi"))
        activityDao.insert(activity(habitId = "vitamins", note = "vit"))

        val result = activityDao.activitiesForHabitOnDate("qigong", today).first()
        assertEquals(1, result.size)
        assertEquals("qi", result[0].note)
    }

    @Test
    fun updateActivityNote() = runTest {
        habitDao.insertAll(listOf(habit))
        val id = activityDao.insert(activity(note = "original"))
        val saved = activityDao.activitiesForDate(today).first()[0]
        activityDao.update(saved.copy(note = "updated"))

        val result = activityDao.activitiesForDate(today).first()
        assertEquals("updated", result[0].note)
    }

    @Test
    fun updateElapsedTime() = runTest {
        habitDao.insertAll(listOf(habit))
        activityDao.insert(activity(elapsedMs = 1000))
        val saved = activityDao.activitiesForDate(today).first()[0]
        activityDao.update(saved.copy(elapsedMs = 5000))

        val result = activityDao.activitiesForDate(today).first()
        assertEquals(5000L, result[0].elapsedMs)
    }
}
