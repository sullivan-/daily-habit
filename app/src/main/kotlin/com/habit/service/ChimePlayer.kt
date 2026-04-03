package com.habit.service

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri

class ChimePlayer(private val context: Context) {

    private val alarmUri: Uri =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

    fun playThresholdChime() {
        RingtoneManager.getRingtone(context, alarmUri)?.play()
    }

    fun release() {
        // nothing to release with RingtoneManager
    }
}
