package com.habit.service

import android.content.Context
import android.media.RingtoneManager

class ChimePlayer(private val context: Context) {

    fun playThresholdChime() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        RingtoneManager.getRingtone(context, uri)?.play()
    }

    fun release() {}
}
