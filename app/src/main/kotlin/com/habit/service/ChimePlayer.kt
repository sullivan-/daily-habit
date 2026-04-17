package com.habit.service

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class ChimePlayer(private val context: Context) {

    private val handler = Handler(Looper.getMainLooper())
    private var activeRingtone: Ringtone? = null

    fun playGoalChime() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        RingtoneManager.getRingtone(context, uri)?.play()
        vibrate()
    }

    fun playStopChime() {
        playAlarmAndVibrate()
        handler.postDelayed({
            stopRingtone()
            playAlarmAndVibrate()
        }, 1500)
        handler.postDelayed({ stopRingtone() }, 3000)
    }

    private fun playAlarmAndVibrate() {
        stopRingtone()
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        activeRingtone = RingtoneManager.getRingtone(context, uri)?.apply { play() }
        vibrate()
    }

    private fun stopRingtone() {
        activeRingtone?.takeIf { it.isPlaying }?.stop()
        activeRingtone = null
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
                as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(
            VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    }

    fun release() {
        stopRingtone()
        handler.removeCallbacksAndMessages(null)
    }
}
