package com.habit.data

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

class ActivityRepository(private val activityDao: ActivityDao) {
    fun activitiesForDate(date: LocalDate): Flow<List<Activity>> =
        activityDao.activitiesForDate(date)

    fun activitiesForHabitOnDate(
        habitId: String,
        date: LocalDate
    ): Flow<List<Activity>> =
        activityDao.activitiesForHabitOnDate(habitId, date)

    suspend fun inProgressActivity(habitId: String, date: LocalDate): Activity? =
        activityDao.inProgressActivity(habitId, date)

    suspend fun completedHistoryForHabit(habitId: String): List<Activity> =
        activityDao.completedHistoryForHabit(habitId)

    suspend fun create(activity: Activity): Long =
        activityDao.insert(activity)

    suspend fun update(activity: Activity) =
        activityDao.update(activity)

    suspend fun delete(activity: Activity) =
        activityDao.deleteById(activity.id)
}
