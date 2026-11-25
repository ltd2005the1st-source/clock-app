package com.maxalarm.data

import android.content.Context
import com.maxalarm.alarm.AlarmScheduler
import com.maxalarm.core.Alarm

/** What the UI calls to change alarms; keeps storage and AlarmManager in step. */
object Alarms {
    fun save(context: Context, alarm: Alarm): Alarm {
        val saved = AlarmStore.get(context).upsert(alarm)
        if (saved.enabled) AlarmScheduler.schedule(context, saved) else AlarmScheduler.cancel(context, saved.id)
        return saved
    }

    fun setEnabled(context: Context, alarm: Alarm, enabled: Boolean) = save(context, alarm.copy(enabled = enabled))

    /** Deleting an alarm never cancels a snooze that is already pending. */
    fun delete(context: Context, id: Int) {
        AlarmScheduler.cancel(context, id)
        AlarmStore.get(context).delete(id)
    }
}
