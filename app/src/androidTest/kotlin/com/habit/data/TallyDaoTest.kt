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
        val tally = Tally(name = "Sweets", priority = Priority.HIGH)
        dao.insert(tally)
        val all = dao.allTallies().first()
        assertEquals(1, all.size)
        assertEquals("Sweets", all[0].name)
        assertEquals(Priority.HIGH, all[0].priority)
    }

    @Test
    fun queryById() = runTest {
        val id = dao.insert(Tally(name = "Sweets", priority = Priority.HIGH))
        val found = dao.getById(id)
        assertNotNull(found)
        assertEquals("Sweets", found!!.name)
    }

    @Test
    fun queryByIdNotFound() = runTest {
        val found = dao.getById(999L)
        assertNull(found)
    }

    @Test
    fun update() = runTest {
        val id = dao.insert(Tally(name = "Sweets", priority = Priority.HIGH))
        dao.update(Tally(id = id, name = "Candy", priority = Priority.LOW))
        val found = dao.getById(id)
        assertEquals("Candy", found!!.name)
        assertEquals(Priority.LOW, found.priority)
    }

    @Test
    fun delete() = runTest {
        val id = dao.insert(Tally(name = "Sweets", priority = Priority.HIGH))
        dao.deleteById(id)
        val all = dao.allTallies().first()
        assertEquals(0, all.size)
    }

    @Test
    fun cascadeDeleteRemovesChoices() = runTest {
        val tallyId = dao.insert(Tally(name = "Sweets", priority = Priority.HIGH))
        val choiceDao = database.choiceDao()
        choiceDao.insert(
            Choice(
                tallyId = tallyId,
                timestamp = java.time.Instant.now(),
                abstained = true
            )
        )
        val before = choiceDao.recentChoices(tallyId, 10)
        assertEquals(1, before.size)

        dao.deleteById(tallyId)
        val after = choiceDao.recentChoices(tallyId, 10)
        assertEquals(0, after.size)
    }
}
