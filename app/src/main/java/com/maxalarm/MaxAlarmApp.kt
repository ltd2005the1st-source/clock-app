package com.maxalarm

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class MaxAlarmApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val nm = service<NotificationManager>()
        val ringing = NotificationChannel(
            CHANNEL_RINGING, "Ringing alarms", NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Shown while an alarm is ringing"
            // The service plays the sound itself at full volume; the channel stays silent.
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
        }
        nm.createNotificationChannel(ringing)
    }

    companion object {
        const val CHANNEL_RINGING = "ringing"
    }
}
