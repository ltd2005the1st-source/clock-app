package com.maxalarm.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxalarm.alarm.AlarmScheduler
import com.maxalarm.alarm.RingSession
import com.maxalarm.core.Alarm
import com.maxalarm.core.Schedule
import com.maxalarm.data.AlarmStore
import com.maxalarm.data.Alarms
import com.maxalarm.ui.ringing.RingingActivity
import com.maxalarm.ui.theme.MaxAlarmTheme
import java.time.LocalTime
import java.time.ZonedDateTime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AlarmScheduler.rescheduleAll(this)
        setContent {
            MaxAlarmTheme { AppRoot() }
        }
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val store = remember { AlarmStore.get(context) }
    val alarms by store.alarms.collectAsStateWithLifecycle()
    val snoozes by store.snoozes.collectAsStateWithLifecycle()
    val ringing by RingSession.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Alarm?>(null) }

    // If an alarm goes off while the app is open, jump straight to the ringing screen.
    val isRinging = ringing != null
    LaunchedEffect(isRinging) {
        if (isRinging) context.startActivity(RingingActivity.intent(context))
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (RingSession.state.value != null) context.startActivity(RingingActivity.intent(context))
    }

    BackHandler(enabled = editing != null) { editing = null }

    val current = editing
    if (current == null) {
        AlarmListScreen(
            alarms = alarms,
            snoozes = snoozes,
            onAdd = {
                val now = LocalTime.now().plusMinutes(1)
                editing = Alarm(id = Alarm.NEW_ID, hour = now.hour, minute = now.minute)
            },
            onOpen = { editing = it },
            onToggle = { alarm, on ->
                val saved = Alarms.setEnabled(context, alarm, on)
                if (on) toastNext(context, saved)
            },
        )
    } else {
        EditAlarmScreen(
            initial = current,
            onSave = {
                val saved = Alarms.save(context, it.copy(enabled = true))
                toastNext(context, saved)
                editing = null
            },
            onDelete = {
                Alarms.delete(context, current.id)
                editing = null
            },
            onClose = { editing = null },
        )
    }
}

private fun toastNext(context: android.content.Context, alarm: Alarm) {
    val now = ZonedDateTime.now()
    val next = Schedule.nextOccurrence(alarm, now)
    Toast.makeText(context, "Alarm set for ${Schedule.describeUntil(now, next)}", Toast.LENGTH_SHORT).show()
}
