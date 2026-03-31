package com.habit.data

import kotlinx.coroutines.flow.Flow

class HabitRepository(private val habitDao: HabitDao) {
    fun allHabits(): Flow<List<Habit>> = habitDao.allHabits()

    suspend fun getById(id: String): Habit? = habitDao.getById(id)

    suspend fun loadFromConfig(habits: List<Habit>) {
        habitDao.insertAll(habits)
    }
}
