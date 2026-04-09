package com.habit.data

import kotlinx.coroutines.flow.Flow

class HabitRepository(private val habitDao: HabitDao) {
    fun allHabits(): Flow<List<Habit>> = habitDao.allHabits()

    suspend fun count(): Int = habitDao.count()

    suspend fun getById(id: String): Habit? = habitDao.getById(id)

    suspend fun loadFromConfig(habits: List<Habit>) {
        habitDao.insertAll(habits)
    }

    suspend fun allIds(): List<String> = habitDao.allIds()

    suspend fun insert(habit: Habit) = habitDao.insert(habit)

    suspend fun update(habit: Habit) = habitDao.update(habit)

    suspend fun deleteById(id: String) = habitDao.deleteById(id)
}
