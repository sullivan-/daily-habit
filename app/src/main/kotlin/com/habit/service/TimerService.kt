package com.habit.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.habit.HabitApp
import com.habit.MainActivity

class TimerService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val habitName = intent?.getStringExtra(EXTRA_HABIT_NAME) ?: "Timer"
        startForeground(NOTIFICATION_ID, buildNotification(habitName, "0:00"))
        return START_STICKY
    }

    private fun buildNotification(habitName: String, elapsed: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, HabitApp.TIMER_CHANNEL_ID)
            .setContentTitle(habitName)
            .setContentText(elapsed)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1
        const val EXTRA_HABIT_NAME = "habit_name"
    }
}
