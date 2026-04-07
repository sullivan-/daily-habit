package com.habit.data

import kotlinx.coroutines.flow.Flow

class TallyRepository(private val tallyDao: TallyDao) {
    fun allTallies(): Flow<List<Tally>> = tallyDao.allTallies()
    suspend fun loadFromConfig(tallies: List<Tally>) = tallyDao.insertAll(tallies)
    suspend fun getById(id: String): Tally? = tallyDao.getById(id)
    suspend fun insert(tally: Tally) = tallyDao.insert(tally)
    suspend fun update(tally: Tally) = tallyDao.update(tally)
    suspend fun deleteById(id: String) = tallyDao.deleteById(id)
}
