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

    /** Starts a countdown that rings like an alarm after [durationMillis]. */
    fun startCountdown(context: Context, template: Alarm, durationMillis: Long) {
        val countdown = AlarmStore.get(context).addSnooze(
            template.copy(id = Alarm.COUNTDOWN_ID, enabled = true, days = emptySet()),
            System.currentTimeMillis() + durationMillis,
            isCountdown = true,
        )
        AlarmScheduler.scheduleSnooze(context, countdown)
    }

    /** Only possible before the countdown rings. */
    fun cancelCountdown(context: Context, requestCode: Int) {
        if (AlarmStore.get(context).cancelCountdown(requestCode)) AlarmScheduler.cancel(context, requestCode)
    }

    /** Deleting an alarm never cancels a snooze that is already pending. */
    fun delete(context: Context, id: Int) {
        AlarmScheduler.cancel(context, id)
        AlarmStore.get(context).delete(id)
    }
}
