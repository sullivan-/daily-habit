package com.habit.data

import kotlinx.coroutines.flow.Flow

class TallyRepository(private val tallyDao: TallyDao) {
    fun allTallies(): Flow<List<Tally>> = tallyDao.allTallies()
    suspend fun loadFromConfig(tallies: List<Tally>) = tallyDao.insertAll(tallies)
    suspend fun getById(id: Long): Tally? = tallyDao.getById(id)
    suspend fun insert(tally: Tally): Long = tallyDao.insert(tally)
    suspend fun update(tally: Tally) = tallyDao.update(tally)
    suspend fun deleteById(id: Long) = tallyDao.deleteById(id)
}
