package com.habit.data

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EasyDayRepository(private val dao: EasyDayDao) {
    fun flowForDate(date: LocalDate): Flow<EasyDayLevel> =
        dao.flowForDate(date).map { it?.level ?: EasyDayLevel.OFF }

    suspend fun levelForDate(date: LocalDate): EasyDayLevel =
        dao.getForDate(date)?.level ?: EasyDayLevel.OFF

    suspend fun setLevel(date: LocalDate, level: EasyDayLevel) {
        if (level == EasyDayLevel.OFF) {
            dao.deleteForDate(date)
        } else {
            dao.upsert(EasyDaySettingEntity(date = date, level = level))
        }
    }

    suspend fun history(): List<EasyDaySettingEntity> = dao.getAll()
}
