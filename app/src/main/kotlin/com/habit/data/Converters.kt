package com.habit.data

import androidx.room.TypeConverter
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class Converters {

    @TypeConverter
    fun fromDayOfWeekSet(days: Set<DayOfWeek>): String =
        days.joinToString(",") { it.name }

    @TypeConverter
    fun toDayOfWeekSet(value: String): Set<DayOfWeek> =
        if (value.isEmpty()) emptySet()
        else value.split(",").map { DayOfWeek.valueOf(it) }.toSet()

    @TypeConverter
    fun fromDailyTexts(texts: Map<DayOfWeek, String>): String {
        val stringMap = texts.mapKeys { it.key.name }
        return Json.encodeToString(stringMap)
    }

    @TypeConverter
    fun toDailyTexts(value: String): Map<DayOfWeek, String> {
        if (value.isEmpty() || value == "{}") return emptyMap()
        val stringMap: Map<String, String> = Json.decodeFromString(value)
        return stringMap.mapKeys { DayOfWeek.valueOf(it.key) }
    }

    @TypeConverter
    fun fromLocalDate(date: LocalDate): Long = date.toEpochDay()

    @TypeConverter
    fun toLocalDate(value: Long): LocalDate = LocalDate.ofEpochDay(value)

    @TypeConverter
    fun fromInstant(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun fromTargetMode(mode: TargetMode): String = mode.name

    @TypeConverter
    fun toTargetMode(value: String): TargetMode = TargetMode.valueOf(value)

    @TypeConverter
    fun fromThresholdType(type: ThresholdType?): String? = type?.name

    @TypeConverter
    fun toThresholdType(value: String?): ThresholdType? = value?.let { ThresholdType.valueOf(it) }

    @TypeConverter
    fun fromPriority(priority: Priority): String = priority.name

    @TypeConverter
    fun toPriority(value: String): Priority = Priority.valueOf(value)
}
