package com.maxalarm.data

import android.content.Context
import com.maxalarm.core.Alarm
import com.maxalarm.core.PendingSnooze
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek

/**
 * Alarms, pending snoozes and the "currently ringing" set, kept in device-protected
 * storage so alarms still work after a reboot before the phone is unlocked.
 */
class AlarmStore private constructor(context: Context) {

    private val prefs = context.applicationContext
        .createDeviceProtectedStorageContext()
        .getSharedPreferences("max_alarm", Context.MODE_PRIVATE)

    private val _alarms = MutableStateFlow(readAlarms(KEY_ALARMS))
    val alarms: StateFlow<List<Alarm>> = _alarms.asStateFlow()

    private val _snoozes = MutableStateFlow(readSnoozes())
    val snoozes: StateFlow<List<PendingSnooze>> = _snoozes.asStateFlow()

    fun find(id: Int): Alarm? = _alarms.value.firstOrNull { it.id == id }

    @Synchronized
    fun upsert(alarm: Alarm): Alarm {
        val saved = if (alarm.id == Alarm.NEW_ID) alarm.copy(id = nextId("next_alarm_id", 1)) else alarm
        val list = _alarms.value.filterNot { it.id == saved.id } + saved
        writeAlarms(KEY_ALARMS, list)
        _alarms.value = list
        return saved
    }

    @Synchronized
    fun delete(id: Int) {
        val list = _alarms.value.filterNot { it.id == id }
        writeAlarms(KEY_ALARMS, list)
        _alarms.value = list
    }

    @Synchronized
    fun addSnooze(alarm: Alarm, triggerAtMillis: Long, isCountdown: Boolean = false): PendingSnooze {
        val snooze = PendingSnooze(
            nextId("next_snooze_code", SNOOZE_CODE_BASE), alarm, triggerAtMillis,
            isCountdown = isCountdown, startedAtMillis = System.currentTimeMillis(),
        )
        val list = _snoozes.value + snooze
        writeSnoozes(list)
        _snoozes.value = list
        return snooze
    }

    /** Removes and returns the snooze, so it can only ring once. */
    @Synchronized
    fun takeSnooze(requestCode: Int): PendingSnooze? {
        val snooze = _snoozes.value.firstOrNull { it.requestCode == requestCode } ?: return null
        val list = _snoozes.value - snooze
        writeSnoozes(list)
        _snoozes.value = list
        return snooze
    }

    /** Cancels a countdown that hasn't rung yet. Real snoozes can't be removed this way. */
    @Synchronized
    fun cancelCountdown(requestCode: Int): Boolean {
        val countdown = _snoozes.value.firstOrNull { it.requestCode == requestCode && it.isCountdown } ?: return false
        val list = _snoozes.value - countdown
        writeSnoozes(list)
        _snoozes.value = list
        return true
    }

    @Synchronized
    fun ringing(): List<Alarm> = readAlarms(KEY_RINGING)

    @Synchronized
    fun addRinging(alarm: Alarm) {
        val list = ringing().filterNot { it.id == alarm.id } + alarm
        writeAlarms(KEY_RINGING, list)
    }

    @Synchronized
    fun clearRinging() {
        prefs.edit().remove(KEY_RINGING).commit()
    }

    private fun nextId(key: String, start: Int): Int {
        val id = prefs.getInt(key, start)
        prefs.edit().putInt(key, id + 1).commit()
        return id
    }

    private fun readAlarms(key: String): List<Alarm> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { alarmFromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    private fun writeAlarms(key: String, list: List<Alarm>) {
        val arr = JSONArray()
        list.forEach { arr.put(alarmToJson(it)) }
        prefs.edit().putString(key, arr.toString()).commit()
    }

    private fun readSnoozes(): List<PendingSnooze> {
        val raw = prefs.getString(KEY_SNOOZES, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                PendingSnooze(
                    o.getInt("code"), alarmFromJson(o.getJSONObject("alarm")), o.getLong("at"),
                    isCountdown = o.optBoolean("countdown", false),
                    startedAtMillis = o.optLong("started", 0),
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun writeSnoozes(list: List<PendingSnooze>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject().put("code", it.requestCode).put("alarm", alarmToJson(it.alarm)).put("at", it.triggerAtMillis)
                    .put("countdown", it.isCountdown).put("started", it.startedAtMillis),
            )
        }
        prefs.edit().putString(KEY_SNOOZES, arr.toString()).commit()
    }

    private fun alarmToJson(a: Alarm) = JSONObject()
        .put("id", a.id)
        .put("hour", a.hour)
        .put("minute", a.minute)
        .put("label", a.label)
        .put("enabled", a.enabled)
        .put("days", JSONArray(a.days.map { it.value }))
        .put("snooze", a.snoozeMinutes)
        .put("vibrate", a.vibrate)
        .put("ringtone", a.ringtoneUri ?: JSONObject.NULL)
        .put("tasks", a.taskCount)

    private fun alarmFromJson(o: JSONObject): Alarm {
        val days = o.optJSONArray("days")
        return Alarm(
            id = o.getInt("id"),
            hour = o.getInt("hour"),
            minute = o.getInt("minute"),
            label = o.optString("label", ""),
            enabled = o.optBoolean("enabled", true),
            days = if (days == null) emptySet() else (0 until days.length()).map { DayOfWeek.of(days.getInt(it)) }.toSet(),
            snoozeMinutes = o.optInt("snooze", Alarm.DEFAULT_SNOOZE_MINUTES),
            vibrate = o.optBoolean("vibrate", true),
            ringtoneUri = if (o.isNull("ringtone")) null else o.optString("ringtone"),
            taskCount = o.optInt("tasks", Alarm.DEFAULT_TASK_COUNT),
        )
    }

    companion object {
        private const val KEY_ALARMS = "alarms"
        private const val KEY_SNOOZES = "snoozes"
        private const val KEY_RINGING = "ringing"
        /** Alarm request codes are their ids; snoozes use codes from here up so they never collide. */
        const val SNOOZE_CODE_BASE = 1_000_000

        @Volatile private var instance: AlarmStore? = null

        fun get(context: Context): AlarmStore =
            instance ?: synchronized(this) { instance ?: AlarmStore(context).also { instance = it } }
    }
}
