package com.habit

import android.content.Context
import androidx.room.Room
import com.habit.data.ActivityRepository
import com.habit.data.ConfigLoader
import com.habit.data.DayBoundary
import com.habit.data.HabitDatabase
import com.habit.data.HabitRepository

class AppContainer(context: Context) {
    private val config = ConfigLoader(context).load()

    private val database = Room.databaseBuilder(
        context,
        HabitDatabase::class.java,
        "habit.db"
    ).addMigrations(HabitDatabase.MIGRATION_2_3, HabitDatabase.MIGRATION_3_4).build()

    val habitRepo = HabitRepository(database.habitDao())
    val activityRepo = ActivityRepository(database.activityDao())
    val dayBoundary = DayBoundary(config.dayBoundaryHour)
    val habits = config.habits
}
