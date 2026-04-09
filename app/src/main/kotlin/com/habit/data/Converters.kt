package com.habit.data

import androidx.room.TypeConverter
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
class Converters {

    @TypeConverter
    fun fromDayOfWeekSet(days: Set<DayOfWeek>): String =
        days.joinToString(",") { it.name }

    @TypeConverter
    fun toDayOfWeekSet(value: String): Set<DayOfWeek> =
        if (value.isEmpty()) emptySet()
        else value.split(",").map { DayOfWeek.valueOf(it) }.toSet()

    @TypeConverter
    fun fromDayOfWeek(day: DayOfWeek?): String? = day?.name

    @TypeConverter
    fun toDayOfWeek(value: String?): DayOfWeek? = value?.let { DayOfWeek.valueOf(it) }

    @TypeConverter
    fun fromIntList(list: List<Int>): String =
        list.joinToString(",")

    @TypeConverter
    fun toIntList(value: String): List<Int> =
        if (value.isEmpty()) emptyList()
        else value.split(",").map { it.toInt() }

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
    fun fromPriority(priority: Priority): String = priority.name

    @TypeConverter
    fun toPriority(value: String): Priority = Priority.valueOf(value)
}
