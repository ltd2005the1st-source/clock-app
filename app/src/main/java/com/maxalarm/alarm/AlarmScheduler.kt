package com.maxalarm.alarm

import com.maxalarm.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.maxalarm.core.Alarm
import com.maxalarm.core.PendingSnooze
import com.maxalarm.core.Schedule
import com.maxalarm.data.AlarmStore
import com.maxalarm.ui.MainActivity
import java.time.ZonedDateTime

object AlarmScheduler {

    fun schedule(context: Context, alarm: Alarm) {
        val at = Schedule.nextOccurrence(alarm, ZonedDateTime.now()).toInstant().toEpochMilli()
        setAlarmClock(context, at, ringIntent(context, alarm.id, AlarmReceiver.KIND_ALARM))
    }

    fun cancel(context: Context, alarmId: Int) {
        val pi = PendingIntent.getBroadcast(
            context, alarmId, baseIntent(context),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        context.service<AlarmManager>().cancel(pi)
        pi.cancel()
    }

    fun scheduleSnooze(context: Context, snooze: PendingSnooze) {
        val at = maxOf(snooze.triggerAtMillis, System.currentTimeMillis() + 1_000)
        setAlarmClock(context, at, ringIntent(context, snooze.requestCode, AlarmReceiver.KIND_SNOOZE))
    }

    /** Re-registers everything with AlarmManager (after boot, time change, app update or launch). */
    fun rescheduleAll(context: Context) {
        val store = AlarmStore.get(context)
        store.alarms.value.forEach { if (it.enabled) schedule(context, it) else cancel(context, it.id) }
        store.snoozes.value.forEach { scheduleSnooze(context, it) }
    }

    private fun baseIntent(context: Context) =
        Intent(context, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_RING)

    private fun ringIntent(context: Context, requestCode: Int, kind: String): PendingIntent {
        val intent = baseIntent(context)
            .putExtra(AlarmReceiver.EXTRA_KIND, kind)
            .putExtra(AlarmReceiver.EXTRA_CODE, requestCode)
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun setAlarmClock(context: Context, atMillis: Long, operation: PendingIntent) {
        val am = context.service<AlarmManager>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            // Only possible on Android 12/12L if the user revoked "Alarms & reminders".
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, operation)
            return
        }
        val show = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        am.setAlarmClock(AlarmManager.AlarmClockInfo(atMillis, show), operation)
    }
}
