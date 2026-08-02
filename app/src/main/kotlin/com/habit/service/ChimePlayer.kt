package com.habit.service

import android.content.Context
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class ChimePlayer(private val context: Context) {

    private val handler = Handler(Looper.getMainLooper())
    private var activeRingtone: Ringtone? = null
    private var toneGenerator: ToneGenerator? = null

    fun playGoalChime() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        RingtoneManager.getRingtone(context, uri)?.play()
        vibrate(200)
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

    private fun vibrate(durationMs: Long = 500) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
                as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(
            VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
        )
    }

    fun playIntervalChime(count: Int) {
        if (toneGenerator == null) {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        }
        repeat(count) { i ->
            handler.postDelayed({
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 200)
            }, i * BEEP_SPACING_MS)
        }
    }

    fun release() {
        stopRingtone()
        toneGenerator?.release()
        toneGenerator = null
        handler.removeCallbacksAndMessages(null)
    }

    companion object {
        private const val BEEP_SPACING_MS = 400L
    }
}
