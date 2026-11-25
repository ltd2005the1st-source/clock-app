package com.maxalarm.core

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime

object Schedule {

    /** The next moment strictly after [now] at which [alarm] should ring. */
    fun nextOccurrence(alarm: Alarm, now: ZonedDateTime): ZonedDateTime {
        val time = LocalTime.of(alarm.hour, alarm.minute)
        for (offset in 0L..7L) {
            val date = now.toLocalDate().plusDays(offset)
            // ZonedDateTime.of moves a time inside a DST gap forward, which is what an alarm should do.
            val candidate = ZonedDateTime.of(date, time, now.zone)
            if (candidate.isAfter(now) && (alarm.days.isEmpty() || candidate.dayOfWeek in alarm.days)) {
                return candidate
            }
        }
        error("No occurrence found for $alarm")
    }

    fun describeDays(days: Set<DayOfWeek>): String {
        val weekdays = setOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
        )
        val weekend = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        return when {
            days.isEmpty() -> "Once"
            days.size == 7 -> "Every day"
            days == weekdays -> "Weekdays"
            days == weekend -> "Weekends"
            else -> DayOfWeek.entries.filter { it in days }.joinToString(", ") { shortName(it) }
        }
    }

    fun shortName(day: DayOfWeek): String =
        day.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)

    /** "in 7 h 12 min", "in 45 min", "in less than a minute". */
    fun describeUntil(now: ZonedDateTime, target: ZonedDateTime): String {
        val totalMinutes = Duration.between(now, target).toMinutes()
        if (totalMinutes < 1) return "in less than a minute"
        val days = totalMinutes / (60 * 24)
        val hours = (totalMinutes / 60) % 24
        val minutes = totalMinutes % 60
        val parts = buildList {
            if (days > 0) add("$days d")
            if (hours > 0) add("$hours h")
            if (minutes > 0 && days == 0L) add("$minutes min")
        }
        return "in " + parts.joinToString(" ")
    }
}
