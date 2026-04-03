package com.habit.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.habit.HabitApp
import com.habit.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null
    private var chimePlayer: ChimePlayer? = null

    private var habitName: String = ""
    private var startEpochMs: Long = 0
    private var chimeIntervalMs: Long = 0
    private var thresholdMs: Long = 0
    private var lastIntervalChimeMs: Long = -1
    private var thresholdChimeFired: Boolean = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        chimePlayer = ChimePlayer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                habitName = intent.getStringExtra(EXTRA_HABIT_NAME) ?: "Timer"
                startEpochMs = intent.getLongExtra(EXTRA_START_EPOCH_MS, System.currentTimeMillis())
                chimeIntervalMs = intent.getLongExtra(EXTRA_CHIME_INTERVAL_MS, 0)
                thresholdMs = intent.getLongExtra(EXTRA_THRESHOLD_MS, 0)
                lastIntervalChimeMs = 0
                thresholdChimeFired = false

                startForeground(NOTIFICATION_ID, buildNotification(habitName, "0:00"))
                startTicking()
            }
            ACTION_STOP -> {
                stopTicking()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive) {
                delay(1000)
                val elapsed = System.currentTimeMillis() - startEpochMs

                val manager = getSystemService(NOTIFICATION_SERVICE)
                    as android.app.NotificationManager
                manager.notify(NOTIFICATION_ID, buildNotification(habitName, formatElapsed(elapsed)))

                if (chimeIntervalMs > 0) {
                    val prevCount = lastIntervalChimeMs / chimeIntervalMs
                    val currCount = elapsed / chimeIntervalMs
                    if (currCount > prevCount) {
                        chimePlayer?.playIntervalChime()
                        lastIntervalChimeMs = elapsed
                    }
                }

                if (thresholdMs > 0 && !thresholdChimeFired && elapsed >= thresholdMs) {
                    chimePlayer?.playThresholdChime()
                    thresholdChimeFired = true
                }
            }
        }
    }

    private fun stopTicking() {
        tickJob?.cancel()
        tickJob = null
    }

    override fun onDestroy() {
        stopTicking()
        chimePlayer?.release()
        chimePlayer = null
        super.onDestroy()
    }

    private fun buildNotification(habitName: String, elapsed: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, HabitApp.TIMER_CHANNEL_ID)
            .setContentTitle(habitName)
            .setContentText(elapsed)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.habit.timer.START"
        const val ACTION_STOP = "com.habit.timer.STOP"
        const val EXTRA_HABIT_NAME = "habit_name"
        const val EXTRA_START_EPOCH_MS = "start_epoch_ms"
        const val EXTRA_CHIME_INTERVAL_MS = "chime_interval_ms"
        const val EXTRA_THRESHOLD_MS = "threshold_ms"

        fun startIntent(
            context: Context,
            habitName: String,
            startEpochMs: Long,
            chimeIntervalMs: Long,
            thresholdMs: Long
        ): Intent = Intent(context, TimerService::class.java).apply {
            action = ACTION_START
            putExtra(EXTRA_HABIT_NAME, habitName)
            putExtra(EXTRA_START_EPOCH_MS, startEpochMs)
            putExtra(EXTRA_CHIME_INTERVAL_MS, chimeIntervalMs)
            putExtra(EXTRA_THRESHOLD_MS, thresholdMs)
        }

        fun stopIntent(context: Context): Intent =
            Intent(context, TimerService::class.java).apply {
                action = ACTION_STOP
            }

        private fun formatElapsed(ms: Long): String {
            val totalSeconds = ms / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                "%d:%02d:%02d".format(hours, minutes, seconds)
            } else {
                "%d:%02d".format(minutes, seconds)
            }
        }
    }
}
