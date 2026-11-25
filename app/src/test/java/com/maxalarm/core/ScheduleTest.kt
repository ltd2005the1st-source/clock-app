package com.maxalarm.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime

class ScheduleTest {
    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")
    // Tuesday 24 Nov 2026, 20:00
    private val now = ZonedDateTime.of(2026, 11, 24, 20, 0, 0, 0, zone)

    @Test fun oneShotLaterToday() {
        val next = Schedule.nextOccurrence(Alarm(1, 21, 30), now)
        assertEquals(ZonedDateTime.of(2026, 11, 24, 21, 30, 0, 0, zone), next)
    }

    @Test fun oneShotEarlierRollsToTomorrow() {
        val next = Schedule.nextOccurrence(Alarm(1, 6, 0), now)
        assertEquals(ZonedDateTime.of(2026, 11, 25, 6, 0, 0, 0, zone), next)
    }

    @Test fun sameMinuteAsNowRollsToTomorrow() {
        val next = Schedule.nextOccurrence(Alarm(1, 20, 0), now)
        assertEquals(25, next.dayOfMonth)
    }

    @Test fun repeatingSkipsToNextSelectedDay() {
        val alarm = Alarm(1, 7, 0, days = setOf(DayOfWeek.MONDAY))
        val next = Schedule.nextOccurrence(alarm, now)
        assertEquals(ZonedDateTime.of(2026, 11, 30, 7, 0, 0, 0, zone), next)
    }

    @Test fun repeatingSameDayStillAhead() {
        val alarm = Alarm(1, 22, 0, days = setOf(DayOfWeek.TUESDAY))
        assertEquals(24, Schedule.nextOccurrence(alarm, now).dayOfMonth)
    }

    @Test fun repeatingSameDayPassedWaitsAWeek() {
        val alarm = Alarm(1, 19, 0, days = setOf(DayOfWeek.TUESDAY))
        assertEquals(ZonedDateTime.of(2026, 12, 1, 19, 0, 0, 0, zone), Schedule.nextOccurrence(alarm, now))
    }

    @Test fun dstGapMovesForward() {
        val ny = ZoneId.of("America/New_York")
        val before = ZonedDateTime.of(2026, 3, 7, 23, 0, 0, 0, ny) // 8 Mar 2026 02:30 does not exist
        val next = Schedule.nextOccurrence(Alarm(1, 2, 30), before)
        assertEquals(3, next.hour)
        assertEquals(8, next.dayOfMonth)
    }

    @Test fun describesDays() {
        assertEquals("Once", Schedule.describeDays(emptySet()))
        assertEquals("Every day", Schedule.describeDays(DayOfWeek.entries.toSet()))
        assertEquals("Weekdays", Schedule.describeDays(DayOfWeek.entries.take(5).toSet()))
        assertEquals("Weekends", Schedule.describeDays(setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)))
        assertEquals("Mon, Wed", Schedule.describeDays(setOf(DayOfWeek.WEDNESDAY, DayOfWeek.MONDAY)))
    }

    @Test fun describesUntil() {
        assertEquals("in 1 h 30 min", Schedule.describeUntil(now, now.plusMinutes(90)))
        assertEquals("in 45 min", Schedule.describeUntil(now, now.plusMinutes(45)))
        assertEquals("in less than a minute", Schedule.describeUntil(now, now.plusSeconds(20)))
        assertEquals("in 2 d 3 h", Schedule.describeUntil(now, now.plusHours(51).plusMinutes(10)))
    }
}
