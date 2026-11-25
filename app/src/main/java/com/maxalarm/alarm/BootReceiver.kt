package com.maxalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.maxalarm.data.AlarmStore

/** AlarmManager forgets everything on reboot, so put the alarms back. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val store = AlarmStore.get(context)
        // An alarm that was still ringing when the phone switched off rings again right away:
        // turning the phone off is not a way to stop it.
        val wasRinging = store.ringing()
        if (wasRinging.isNotEmpty()) {
            store.clearRinging()
            wasRinging.forEach { store.addSnooze(it, System.currentTimeMillis() + 3_000) }
        }
        AlarmScheduler.rescheduleAll(context)
    }
}
