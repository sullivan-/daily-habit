package com.habit.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
class TallyDaoTest {

    private lateinit var database: HabitDatabase
    private lateinit var dao: TallyDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.tallyDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndQueryAll() = runTest {
        val tally = Tally(id = "sweets", name = "Sweets", priority = Priority.HIGH)
        dao.insert(tally)
        val all = dao.allTallies().first()
        assertEquals(1, all.size)
        assertEquals("Sweets", all[0].name)
        assertEquals(Priority.HIGH, all[0].priority)
    }

    @Test
    fun queryById() = runTest {
        dao.insert(Tally(id = "sweets", name = "Sweets", priority = Priority.HIGH))
        val found = dao.getById("sweets")
        assertNotNull(found)
        assertEquals("Sweets", found!!.name)
    }

    @Test
    fun queryByIdNotFound() = runTest {
        val found = dao.getById("nonexistent")
        assertNull(found)
    }

    @Test
    fun update() = runTest {
        dao.insert(Tally(id = "sweets", name = "Sweets", priority = Priority.HIGH))
        dao.update(Tally(id = "sweets", name = "Candy", priority = Priority.LOW))
        val found = dao.getById("sweets")
        assertEquals("Candy", found!!.name)
        assertEquals(Priority.LOW, found.priority)
    }

    @Test
    fun delete() = runTest {
        dao.insert(Tally(id = "sweets", name = "Sweets", priority = Priority.HIGH))
        dao.deleteById("sweets")
        val all = dao.allTallies().first()
        assertEquals(0, all.size)
    }

    @Test
    fun cascadeDeleteRemovesChoices() = runTest {
        dao.insert(Tally(id = "sweets", name = "Sweets", priority = Priority.HIGH))
        val choiceDao = database.choiceDao()
        choiceDao.insert(
            Choice(
                tallyId = "sweets",
                timestamp = java.time.Instant.now(),
                abstained = true
            )
        )
        val before = choiceDao.recentChoices("sweets", 10)
        assertEquals(1, before.size)

        dao.deleteById("sweets")
        val after = choiceDao.recentChoices("sweets", 10)
        assertEquals(0, after.size)
    }
}
