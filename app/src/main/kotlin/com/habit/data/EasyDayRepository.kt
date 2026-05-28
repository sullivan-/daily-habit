package com.habit.data

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class EasyDayRepository(private val dao: EasyDayDao) {
    fun flowForDate(date: LocalDate): Flow<EasyDayLevel> =
        combine(dao.flowForDate(date), dao.flowCarryOver()) { perDate, carry ->
            if (carry?.enabled == true) carry.level
            else perDate?.level ?: EasyDayLevel.OFF
        }

    fun carryOverFlow(): Flow<Boolean> =
        dao.flowCarryOver().map { it?.enabled ?: false }

    suspend fun levelForDate(date: LocalDate): EasyDayLevel {
        val carry = dao.getCarryOver()
        if (carry?.enabled == true) return carry.level
        return dao.getForDate(date)?.level ?: EasyDayLevel.OFF
    }

    suspend fun setLevel(date: LocalDate, level: EasyDayLevel) {
        if (level == EasyDayLevel.OFF) {
            dao.deleteForDate(date)
        } else {
            dao.upsert(EasyDaySettingEntity(date = date, level = level))
        }
    }

    suspend fun setCarryOver(enabled: Boolean, level: EasyDayLevel) {
        dao.upsertCarryOver(EasyDayCarryOverEntity(enabled = enabled, level = level))
    }

    suspend fun history(): List<EasyDaySettingEntity> = dao.getAll()
}
