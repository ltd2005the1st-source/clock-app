@file:OptIn(ExperimentalMaterial3Api::class)

package com.maxalarm.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.maxalarm.core.Alarm
import com.maxalarm.core.PendingSnooze
import com.maxalarm.data.Alarms
import kotlinx.coroutines.delay

/**
 * Countdown mode: rings exactly like an alarm (max volume, the tasks, Snooze / Turn off),
 * but after a set duration instead of at a clock time.
 */
@Composable
fun CountdownScreen(
    countdowns: List<PendingSnooze>,
    bottomBar: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var hours by rememberSaveable { mutableIntStateOf(0) }
    var minutes by rememberSaveable { mutableIntStateOf(10) }
    var seconds by rememberSaveable { mutableIntStateOf(0) }
    var label by rememberSaveable { mutableStateOf("") }
    var tasks by rememberSaveable { mutableIntStateOf(Alarm.DEFAULT_TASK_COUNT) }
    var snooze by rememberSaveable { mutableIntStateOf(Alarm.DEFAULT_SNOOZE_MINUTES) }
    var vibrate by rememberSaveable { mutableStateOf(true) }
    val totalSeconds = hours * 3600L + minutes * 60L + seconds

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { LargeTopAppBar(title = { Text("Countdown") }, scrollBehavior = scrollBehavior) },
        bottomBar = bottomBar,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            countdowns.sortedBy { it.triggerAtMillis }.forEach { RunningCountdown(it) }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spinner(hours, "h", 23) { hours = it }
                Separator()
                Spinner(minutes, "min", 59) { minutes = it }
                Separator()
                Spinner(seconds, "s", 59) { seconds = it }
            }

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(1, 5, 10, 15, 20, 30, 45, 60, 90).forEach { m ->
                    AssistChip(
                        onClick = { hours = m / 60; minutes = m % 60; seconds = 0 },
                        label = { Text(if (m < 60) "$m min" else if (m % 60 == 0) "${m / 60} h" else "${m / 60} h ${m % 60}") },
                    )
                }
            }

            OutlinedTextField(
                value = label,
                onValueChange = { label = it.take(40) },
                label = { Text("Label") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            TaskCountPicker(tasks) { tasks = it }
            SnoozeLengthPicker(snooze) { snooze = it }
            ListItem(
                headlineContent = { Text("Vibrate") },
                leadingContent = { Icon(Icons.Outlined.Vibration, contentDescription = null) },
                trailingContent = { Switch(checked = vibrate, onCheckedChange = { vibrate = it }) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
            Text(
                "Rings at maximum volume, just like an alarm.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = {
                    val template = Alarm(
                        id = Alarm.COUNTDOWN_ID, hour = 0, minute = 0, label = label.trim(),
                        snoozeMinutes = snooze, vibrate = vibrate, taskCount = tasks,
                    )
                    Alarms.startCountdown(context, template, totalSeconds * 1000)
                    label = ""
                },
                enabled = totalSeconds > 0,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Start countdown")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RunningCountdown(countdown: PendingSnooze) {
    val context = LocalContext.current
    val now by produceState(System.currentTimeMillis(), countdown.requestCode) {
        while (true) {
            value = System.currentTimeMillis()
            delay(250)
        }
    }
    val remaining = (countdown.triggerAtMillis - now).coerceAtLeast(0)
    val total = (countdown.triggerAtMillis - countdown.startedAtMillis).coerceAtLeast(1)
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(formatDuration(remaining), style = MaterialTheme.typography.displaySmall)
                    Text(
                        countdown.alarm.label.ifBlank { "Countdown" } + " · ${countdown.alarm.taskCount} tasks",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                // A countdown can be cancelled before it rings; once ringing, only the tasks stop it.
                TextButton(onClick = { Alarms.cancelCountdown(context, countdown.requestCode) }) { Text("Cancel") }
            }
            LinearProgressIndicator(
                progress = { 1f - remaining.toFloat() / total },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun Spinner(value: Int, unit: String, max: Int, onChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(onClick = { onChange(if (value >= max) 0 else value + 1) }) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "More $unit")
        }
        Text(
            value.toString().padStart(2, '0'),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Medium,
        )
        Text(unit, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FilledTonalIconButton(onClick = { onChange(if (value <= 0) max else value - 1) }) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Less $unit")
        }
    }
}

@Composable
private fun Separator() {
    Text(
        ":",
        style = MaterialTheme.typography.displayMedium,
        modifier = Modifier.padding(horizontal = 8.dp).padding(bottom = 24.dp),
    )
}

private fun formatDuration(millis: Long): String {
    val s = (millis + 999) / 1000
    return "%d:%02d:%02d".format(s / 3600, (s / 60) % 60, s % 60)
}
