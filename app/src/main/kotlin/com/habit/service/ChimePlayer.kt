package com.habit.service

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri

class ChimePlayer(private val context: Context) {

    private val notificationUri: Uri =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    private val alarmUri: Uri =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

    fun playIntervalChime() {
        RingtoneManager.getRingtone(context, notificationUri)?.play()
    }

    fun playThresholdChime() {
        RingtoneManager.getRingtone(context, alarmUri)?.play()
    }

    fun release() {
        // nothing to release with RingtoneManager
    }
}
