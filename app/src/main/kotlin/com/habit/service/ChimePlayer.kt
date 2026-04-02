package com.habit.service

import android.media.AudioManager
import android.media.ToneGenerator

class ChimePlayer {

    private val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)

    fun playIntervalChime() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
    }

    fun playThresholdChime() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 500)
    }

    fun release() {
        toneGenerator.release()
    }
}
