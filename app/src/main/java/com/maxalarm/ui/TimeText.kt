package com.maxalarm.ui

import android.content.Context
import android.text.format.DateFormat
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formats times following the phone's 12/24-hour setting. */
object TimeText {
    data class Parts(val time: String, val amPm: String?)

    fun parts(context: Context, hour: Int, minute: Int): Parts {
        val t = LocalTime.of(hour, minute)
        return if (DateFormat.is24HourFormat(context)) {
            Parts(t.format(DateTimeFormatter.ofPattern("HH:mm")), null)
        } else {
            Parts(t.format(DateTimeFormatter.ofPattern("h:mm")), t.format(DateTimeFormatter.ofPattern("a", Locale.getDefault())))
        }
    }

    fun full(context: Context, hour: Int, minute: Int): String =
        parts(context, hour, minute).let { if (it.amPm == null) it.time else "${it.time} ${it.amPm}" }

    fun full(context: Context, at: ZonedDateTime): String = full(context, at.hour, at.minute)
}
