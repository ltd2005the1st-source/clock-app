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
    /** How many different tasks silence it: 10 or 20. */
    val taskCount: Int = DEFAULT_TASK_COUNT,
) {
    val repeats: Boolean get() = days.isNotEmpty()

    companion object {
        const val NEW_ID = 0
        const val DEFAULT_SNOOZE_MINUTES = 5
        val SNOOZE_CHOICES = listOf(1, 5, 10, 15, 30)
        const val DEFAULT_TASK_COUNT = 20
        val TASK_COUNT_CHOICES = listOf(10, 20)
        /** Id used by countdowns, which aren't stored in the alarm list. */
        const val COUNTDOWN_ID = -1
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
    /**
     * True for a running countdown that hasn't rung yet. Unlike a snooze, a countdown can be
     * cancelled before it goes off. Once it rings it behaves exactly like an alarm.
     */
    val isCountdown: Boolean = false,
    val startedAtMillis: Long = 0,
)
