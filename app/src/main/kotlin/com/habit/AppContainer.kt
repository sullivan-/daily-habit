package com.habit

import android.content.Context
import androidx.room.Room
import com.habit.data.ActivityRepository
import com.habit.data.AppConfigEntity
import com.habit.data.ChoiceRepository
import com.habit.data.ConfigLoader
import com.habit.data.DayBoundary
import com.habit.data.HabitDatabase
import com.habit.data.HabitRepository
import com.habit.data.TallyRepository
import com.habit.data.TrackRepository

class AppContainer(context: Context) {
    val config = ConfigLoader(context).load()

    val database = Room.databaseBuilder(
        context,
        HabitDatabase::class.java,
        "habit.db"
    ).addMigrations(
        HabitDatabase.MIGRATION_2_3,
        HabitDatabase.MIGRATION_3_4,
        HabitDatabase.MIGRATION_4_5,
        HabitDatabase.MIGRATION_5_6,
        HabitDatabase.MIGRATION_6_7,
        HabitDatabase.MIGRATION_7_8,
        HabitDatabase.MIGRATION_8_9,
        HabitDatabase.MIGRATION_9_10
    ).build()

    val habitRepo = HabitRepository(database.habitDao())
    val activityRepo = ActivityRepository(database.activityDao())
    val tallyRepo = TallyRepository(database.tallyDao())
    val choiceRepo = ChoiceRepository(database.choiceDao())
    val trackRepo = TrackRepository(database.trackDao(), database.milestoneDao())
    private val appConfigDao = database.appConfigDao()
    val dayBoundary = DayBoundary(config.dayBoundaryHour)

    suspend fun seedIfEmpty() {
        if (appConfigDao.get() != null) return
        appConfigDao.insert(AppConfigEntity(dayBoundaryHour = config.dayBoundaryHour))
        habitRepo.loadFromConfig(config.habits)
        tallyRepo.loadFromConfig(config.tallies)
        trackRepo.loadFromConfig(config.tracks, config.milestones)
    }
}
