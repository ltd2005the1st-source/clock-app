@file:OptIn(ExperimentalMaterial3Api::class)

package com.maxalarm.ui

import com.maxalarm.service
import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AlarmOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.maxalarm.core.Alarm
import com.maxalarm.core.PendingSnooze
import com.maxalarm.core.Schedule
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@Composable
fun AlarmListScreen(
    alarms: List<Alarm>,
    snoozes: List<PendingSnooze>,
    onAdd: () -> Unit,
    onOpen: (Alarm) -> Unit,
    onToggle: (Alarm, Boolean) -> Unit,
    bottomBar: @Composable () -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val now by produceState(ZonedDateTime.now()) {
        while (true) {
            delay(15_000)
            value = ZonedDateTime.now()
        }
    }
    val sorted = remember(alarms) { alarms.sortedWith(compareBy({ it.hour }, { it.minute })) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Alarms") },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = bottomBar,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add alarm") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { NextAlarmLine(alarms, snoozes, now) }
            item { SetupCard() }
            items(snoozes.filterNot { it.isCountdown }, key = { "snooze-${it.requestCode}" }) { SnoozeCard(it) }
            if (alarms.isEmpty()) {
                item { EmptyState() }
            }
            items(sorted, key = { it.id }) { alarm ->
                AlarmCard(alarm, onClick = { onOpen(alarm) }, onToggle = { onToggle(alarm, it) })
            }
            item { VolumeNote() }
        }
    }
}

@Composable
private fun NextAlarmLine(alarms: List<Alarm>, snoozes: List<PendingSnooze>, now: ZonedDateTime) {
    val zone = ZoneId.systemDefault()
    val next = (
        alarms.filter { it.enabled }.map { Schedule.nextOccurrence(it, now) } +
            snoozes.filterNot { it.isCountdown }.map { Instant.ofEpochMilli(it.triggerAtMillis).atZone(zone) }
        ).minOrNull()
    Text(
        text = if (next == null) "No alarms set" else "Next alarm ${Schedule.describeUntil(now, next)}",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
    )
}

@Composable
private fun AlarmCard(alarm: Alarm, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val context = LocalContext.current
    val parts = TimeText.parts(context, alarm.hour, alarm.minute)
    val colors = if (alarm.enabled) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    } else {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Card(onClick = onClick, colors = colors, shape = MaterialTheme.shapes.extraLarge) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(parts.time, style = MaterialTheme.typography.displayMedium)
                    if (parts.amPm != null) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            parts.amPm,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                }
                val subtitle = listOf(alarm.label.trim(), Schedule.describeDays(alarm.days))
                    .filter { it.isNotEmpty() }
                    .joinToString(" · ")
                Text(subtitle, style = MaterialTheme.typography.bodyLarge)
            }
            Switch(checked = alarm.enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun SnoozeCard(snooze: PendingSnooze) {
    val context = LocalContext.current
    val at = Instant.ofEpochMilli(snooze.triggerAtMillis).atZone(ZoneId.systemDefault())
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Snooze, contentDescription = null)
            Spacer(Modifier.width(16.dp))
            Column {
                val name = snooze.alarm.label.ifBlank {
                    if (snooze.alarm.id == Alarm.COUNTDOWN_ID) "Countdown" else "Alarm"
                }
                Text("$name snoozed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Rings again at ${TimeText.full(context, at)}. A snoozed alarm can't be cancelled.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.AlarmOff,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.padding(6.dp))
        Text("No alarms yet", style = MaterialTheme.typography.titleLarge)
        Text(
            "Tap Add alarm to set one.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun VolumeNote() {
    OutlinedCard(shape = MaterialTheme.shapes.extraLarge) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                Icon(Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = null)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Always at full volume", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Every alarm rings at your phone's maximum alarm volume, and the volume keys can't " +
                        "lower it. A ringing alarm has no stop button: finish all of its tasks (10 or 20), " +
                        "then choose Snooze or Turn off for today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Outlined.Lock, contentDescription = "Locked")
        }
    }
}

/** Lists the permissions that keep alarms reliable, each with a button that fixes it. */
@Composable
private fun SetupCard() {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { refresh++ }

    val items = remember(refresh) { missingSetup(context) }
    if (items.isEmpty()) return

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Finish setting up", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            items.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.titleSmall)
                        Text(item.why, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.width(12.dp))
                    FilledTonalButton(onClick = {
                        if (item.kind == SetupKind.NOTIFICATIONS && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            runCatching { context.startActivity(settingsIntent(context, item.kind)) }
                        }
                    }) { Text("Allow") }
                }
            }
        }
    }
}

private enum class SetupKind { NOTIFICATIONS, FULL_SCREEN, EXACT_ALARMS, BATTERY, DO_NOT_DISTURB }

private data class SetupItem(val kind: SetupKind, val title: String, val why: String)

private fun missingSetup(context: Context): List<SetupItem> = buildList {
    val nm = context.service<NotificationManager>()
    if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
        add(SetupItem(SetupKind.NOTIFICATIONS, "Notifications", "Needed to show a ringing alarm."))
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && !nm.canUseFullScreenIntent()) {
        add(SetupItem(SetupKind.FULL_SCREEN, "Full-screen alarms", "Lets the alarm take over the lock screen."))
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        !context.service<AlarmManager>().canScheduleExactAlarms()
    ) {
        add(SetupItem(SetupKind.EXACT_ALARMS, "Alarms & reminders", "Needed to ring on the exact minute."))
    }
    if (!context.service<PowerManager>().isIgnoringBatteryOptimizations(context.packageName)) {
        add(SetupItem(SetupKind.BATTERY, "Battery optimization", "Stops the phone from putting alarms to sleep."))
    }
    if (!nm.isNotificationPolicyAccessGranted) {
        add(SetupItem(SetupKind.DO_NOT_DISTURB, "Do Not Disturb access", "Lets alarms ring even in Total silence."))
    }
}

private fun settingsIntent(context: Context, kind: SetupKind): Intent {
    val pkg = Uri.parse("package:${context.packageName}")
    return when (kind) {
        SetupKind.NOTIFICATIONS -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        SetupKind.FULL_SCREEN -> Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkg)
        SetupKind.EXACT_ALARMS -> Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkg)
        SetupKind.BATTERY -> Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkg)
        SetupKind.DO_NOT_DISTURB -> Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
    }
}
