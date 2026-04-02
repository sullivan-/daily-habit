package com.habit.service

import android.media.AudioManager
import android.media.ToneGenerator

class ChimePlayer {

    private val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
    private val loudToneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)

    fun playIntervalChime() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
    }

    fun playThresholdChime() {
        // three ascending tones for a cheerful feel
        loudToneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 200)
        Thread.sleep(250)
        loudToneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 200)
        Thread.sleep(250)
        loudToneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 300)
    }

    fun release() {
        toneGenerator.release()
        loudToneGenerator.release()
    }
}
