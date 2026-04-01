package com.habit.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.DayOfWeek
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HabitDaoTest {

    private lateinit var database: HabitDatabase
    private lateinit var dao: HabitDao

    private val habit = Habit(
        id = "qigong",
        name = "Qigong",
        timesOfDay = listOf(7),
        sortOrder = 1,
        daysActive = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
        dailyTarget = 2,
        dailyTargetMode = TargetMode.AT_LEAST,
        timed = true,
        chimeIntervalSeconds = 10,
        thresholdMinutes = 30,
        thresholdType = ThresholdType.GOAL,
        priority = Priority.HIGH,
        dailyTexts = mapOf(DayOfWeek.MONDAY to "standing form")
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.habitDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndQueryAll() = runTest {
        dao.insertAll(listOf(habit))
        val all = dao.allHabits().first()
        assertEquals(1, all.size)
        assertEquals("Qigong", all[0].name)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), all[0].daysActive)
        assertEquals(mapOf(DayOfWeek.MONDAY to "standing form"), all[0].dailyTexts)
    }

    @Test
    fun queryById() = runTest {
        dao.insertAll(listOf(habit))
        val found = dao.getById("qigong")
        assertNotNull(found)
        assertEquals("Qigong", found!!.name)
    }

    @Test
    fun queryByIdNotFound() = runTest {
        val found = dao.getById("nonexistent")
        assertNull(found)
    }

    @Test
    fun insertReplaces() = runTest {
        dao.insertAll(listOf(habit))
        val updated = habit.copy(name = "Qigong Updated")
        dao.insertAll(listOf(updated))
        val all = dao.allHabits().first()
        assertEquals(1, all.size)
        assertEquals("Qigong Updated", all[0].name)
    }
}
