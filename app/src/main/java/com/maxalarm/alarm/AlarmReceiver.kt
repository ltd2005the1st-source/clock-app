package com.maxalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.maxalarm.data.AlarmStore

/** Fired by AlarmManager when an alarm or a snooze is due. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_RING) return
        val store = AlarmStore.get(context)
        val code = intent.getIntExtra(EXTRA_CODE, -1)
        val alarm = when (intent.getStringExtra(EXTRA_KIND)) {
            KIND_ALARM -> {
                val alarm = store.find(code) ?: return
                if (!alarm.enabled) return
                if (alarm.repeats) {
                    AlarmScheduler.schedule(context, alarm)
                } else {
                    store.upsert(alarm.copy(enabled = false))
                }
                alarm
            }
            KIND_SNOOZE -> store.takeSnooze(code)?.alarm ?: return
            else -> return
        }
        AlarmService.ring(context, alarm)
    }

    companion object {
        const val ACTION_RING = "com.maxalarm.action.RING"
        const val EXTRA_KIND = "kind"
        const val EXTRA_CODE = "code"
        const val KIND_ALARM = "alarm"
        const val KIND_SNOOZE = "snooze"
    }
}
