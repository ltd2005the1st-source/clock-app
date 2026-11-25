package com.maxalarm.core

import java.time.DayOfWeek

/**
 * One alarm as the user configured it. There is deliberately no volume field:
 * every alarm rings at the device's maximum alarm volume.
 */
data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val enabled: Boolean = true,
    /** Empty means "ring once". */
    val days: Set<DayOfWeek> = emptySet(),
    val snoozeMinutes: Int = DEFAULT_SNOOZE_MINUTES,
    val vibrate: Boolean = true,
    /** Null means the system default alarm sound. */
    val ringtoneUri: String? = null,
) {
    val repeats: Boolean get() = days.isNotEmpty()

    companion object {
        const val NEW_ID = 0
        const val DEFAULT_SNOOZE_MINUTES = 5
        val SNOOZE_CHOICES = listOf(1, 5, 10, 15, 30)
    }
}

/**
 * A snoozed alarm waiting to ring again. It keeps a copy of the alarm so it still
 * rings even if the original alarm is edited, switched off or deleted meanwhile.
 */
data class PendingSnooze(
    val requestCode: Int,
    val alarm: Alarm,
    val triggerAtMillis: Long,
)
