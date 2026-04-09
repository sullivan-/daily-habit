package com.habit

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HabitApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannel()
        loadHabits()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            TIMER_CHANNEL_ID,
            "Timer",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows elapsed time while a habit timer is running"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun loadHabits() {
        appScope.launch {
            if (container.habitRepo.count() == 0) {
                container.habitRepo.loadFromConfig(container.habits)
                container.tallyRepo.loadFromConfig(container.tallies)
                container.trackRepo.loadFromConfig(
                    container.tracks,
                    container.milestones
                )
            }
        }
    }

    companion object {
        const val TIMER_CHANNEL_ID = "timer"
    }
}
