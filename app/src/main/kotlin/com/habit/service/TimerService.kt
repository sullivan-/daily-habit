package com.habit.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
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
    private var wakeLock: PowerManager.WakeLock? = null

    private var habitName: String = ""
    private var startEpochMs: Long = 0
    private var goalMs: Long = 0
    private var stopMs: Long = 0
    private var goalChimeFired: Boolean = false
    private var stopChimeFired: Boolean = false
    private var intervalMs: Long = 0
    private var nextIntervalAtMs: Long = 0
    private var isSecondsInterval: Boolean = true

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        chimePlayer = ChimePlayer(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                habitName = intent.getStringExtra(EXTRA_HABIT_NAME) ?: "Timer"
                startEpochMs = intent.getLongExtra(EXTRA_START_EPOCH_MS, System.currentTimeMillis())
                goalMs = intent.getLongExtra(EXTRA_GOAL_MS, 0)
                stopMs = intent.getLongExtra(EXTRA_STOP_MS, 0)
                val currentElapsed = System.currentTimeMillis() - startEpochMs
                goalChimeFired = goalMs > 0 && currentElapsed >= goalMs
                stopChimeFired = stopMs > 0 && currentElapsed >= stopMs

                startForeground(NOTIFICATION_ID, buildNotification(habitName, "0:00"))
                startTicking()
            }
            ACTION_START_INTERVAL -> {
                intervalMs = intent.getLongExtra(EXTRA_INTERVAL_MS, 0)
                val currentElapsed = intent.getLongExtra(EXTRA_CURRENT_ELAPSED_MS, 0)
                isSecondsInterval = intent.getBooleanExtra(EXTRA_IS_SECONDS, true)
                nextIntervalAtMs = currentElapsed + intervalMs
            }
            ACTION_STOP_INTERVAL -> {
                intervalMs = 0
                nextIntervalAtMs = 0
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
        acquireWakeLock()
        tickJob = scope.launch {
            while (isActive) {
                delay(1000)
                val elapsed = System.currentTimeMillis() - startEpochMs

                val manager = getSystemService(NOTIFICATION_SERVICE)
                    as android.app.NotificationManager
                manager.notify(NOTIFICATION_ID, buildNotification(habitName, formatElapsed(elapsed)))

                if (goalMs > 0 && !goalChimeFired && elapsed >= goalMs) {
                    chimePlayer?.playGoalChime()
                    goalChimeFired = true
                }
                if (stopMs > 0 && !stopChimeFired && elapsed >= stopMs) {
                    chimePlayer?.playStopChime()
                    stopChimeFired = true
                }

                if (intervalMs > 0 && elapsed >= nextIntervalAtMs) {
                    chimePlayer?.playIntervalChime(isSecondsInterval)
                    nextIntervalAtMs += intervalMs
                }
            }
        }
    }

    private fun stopTicking() {
        tickJob?.cancel()
        tickJob = null
        releaseWakeLock()
    }

    // held for the full user-initiated session, not a fixed window, so no timeout —
    // a timeout would silently kill chimes mid-session. released explicitly on stop.
    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
                .apply { setReferenceCounted(false) }
        }
        wakeLock?.let { if (!it.isHeld) it.acquire() }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
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
        private const val WAKE_LOCK_TAG = "habit:timer"
        const val ACTION_START = "com.habit.timer.START"
        const val ACTION_STOP = "com.habit.timer.STOP"
        const val ACTION_START_INTERVAL = "com.habit.timer.START_INTERVAL"
        const val ACTION_STOP_INTERVAL = "com.habit.timer.STOP_INTERVAL"
        const val EXTRA_HABIT_NAME = "habit_name"
        const val EXTRA_START_EPOCH_MS = "start_epoch_ms"
        const val EXTRA_GOAL_MS = "goal_ms"
        const val EXTRA_STOP_MS = "stop_ms"
        const val EXTRA_INTERVAL_MS = "interval_ms"
        const val EXTRA_CURRENT_ELAPSED_MS = "current_elapsed_ms"
        const val EXTRA_IS_SECONDS = "is_seconds"

        fun startIntent(
            context: Context,
            habitName: String,
            startEpochMs: Long,
            goalMs: Long,
            stopMs: Long
        ): Intent = Intent(context, TimerService::class.java).apply {
            action = ACTION_START
            putExtra(EXTRA_HABIT_NAME, habitName)
            putExtra(EXTRA_START_EPOCH_MS, startEpochMs)
            putExtra(EXTRA_GOAL_MS, goalMs)
            putExtra(EXTRA_STOP_MS, stopMs)
        }

        fun stopIntent(context: Context): Intent =
            Intent(context, TimerService::class.java).apply {
                action = ACTION_STOP
            }

        fun startIntervalIntent(
            context: Context,
            intervalMs: Long,
            currentElapsedMs: Long,
            isSeconds: Boolean
        ): Intent = Intent(context, TimerService::class.java).apply {
            action = ACTION_START_INTERVAL
            putExtra(EXTRA_INTERVAL_MS, intervalMs)
            putExtra(EXTRA_CURRENT_ELAPSED_MS, currentElapsedMs)
            putExtra(EXTRA_IS_SECONDS, isSeconds)
        }

        fun stopIntervalIntent(context: Context): Intent =
            Intent(context, TimerService::class.java).apply {
                action = ACTION_STOP_INTERVAL
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
