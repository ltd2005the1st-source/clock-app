@file:OptIn(ExperimentalMaterial3Api::class)

package com.maxalarm.ui

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import com.maxalarm.core.Alarm
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun EditAlarmScreen(
    initial: Alarm,
    onSave: (Alarm) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var draft by remember(initial) { mutableStateOf(initial) }
    val timeState = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = DateFormat.is24HourFormat(context),
    )

    val ringtonePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            val uri = IntentCompat.getParcelableExtra(data, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            // "Default" comes back as the settings URI; store null so we follow the system default.
            val isDefault = uri == null || RingtoneManager.isDefault(uri)
            draft = draft.copy(ringtoneUri = if (isDefault) null else uri.toString())
        }
    }
    val ringtoneTitle = remember(draft.ringtoneUri) {
        draft.ringtoneUri?.let { raw ->
            runCatching { RingtoneManager.getRingtone(context, Uri.parse(raw))?.getTitle(context) }.getOrNull()
        } ?: "Default alarm sound"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initial.id == Alarm.NEW_ID) "New alarm" else "Edit alarm") },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close") }
                },
                actions = {
                    TextButton(onClick = {
                        onSave(draft.copy(hour = timeState.hour, minute = timeState.minute))
                    }) { Text("Save") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = timeState)
            }

            OutlinedTextField(
                value = draft.label,
                onValueChange = { draft = draft.copy(label = it.take(40)) },
                label = { Text("Label") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Repeat", style = MaterialTheme.typography.titleSmall)
                DayPicker(draft.days) { draft = draft.copy(days = it) }
            }

            TaskCountPicker(draft.taskCount) { draft = draft.copy(taskCount = it) }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Snooze length", style = MaterialTheme.typography.titleSmall)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    Alarm.SNOOZE_CHOICES.forEachIndexed { i, minutes ->
                        SegmentedButton(
                            selected = draft.snoozeMinutes == minutes,
                            onClick = { draft = draft.copy(snoozeMinutes = minutes) },
                            shape = SegmentedButtonDefaults.itemShape(i, Alarm.SNOOZE_CHOICES.size),
                            label = { Text("$minutes") },
                        )
                    }
                }
                Text(
                    "Minutes until a snoozed alarm rings again.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column {
                ListItem(
                    headlineContent = { Text("Sound") },
                    supportingContent = { Text(ringtoneTitle) },
                    leadingContent = { Icon(Icons.Outlined.MusicNote, contentDescription = null) },
                    modifier = Modifier.clickable {
                        val existing = draft.ringtoneUri?.let(Uri::parse)
                            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Alarm sound")
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            // No "Silent" option: that would defeat the point.
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing)
                        runCatching { ringtonePicker.launch(intent) }
                    },
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text("Vibrate") },
                    leadingContent = { Icon(Icons.Outlined.Vibration, contentDescription = null) },
                    trailingContent = {
                        Switch(checked = draft.vibrate, onCheckedChange = { draft = draft.copy(vibrate = it) })
                    },
                    modifier = Modifier.clickable { draft = draft.copy(vibrate = !draft.vibrate) },
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text("Volume") },
                    supportingContent = { Text("Always the maximum your phone can play. Can't be changed.") },
                    leadingContent = { Icon(Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = null) },
                    trailingContent = { Icon(Icons.Outlined.Lock, contentDescription = "Locked") },
                )
            }

            if (initial.id != Alarm.NEW_ID) {
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null)
                    Spacer(Modifier.padding(4.dp))
                    Text("Delete alarm")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DayPicker(selected: Set<DayOfWeek>, onChange: (Set<DayOfWeek>) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        DayOfWeek.entries.forEach { day ->
            val on = day in selected
            val fullName = day.getDisplayName(TextStyle.FULL, Locale.getDefault())
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(
                        if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                    .clickable(role = Role.Checkbox) { onChange(if (on) selected - day else selected + day) }
                    .semantics {
                        contentDescription = fullName
                        this.selected = on
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** "Tasks to silence": 10 or 20 different actions before Snooze / Turn off appear. */
@Composable
fun TaskCountPicker(selected: Int, onChange: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Tasks to silence", style = MaterialTheme.typography.titleSmall)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            Alarm.TASK_COUNT_CHOICES.forEachIndexed { i, count ->
                SegmentedButton(
                    selected = selected == count,
                    onClick = { onChange(count) },
                    shape = SegmentedButtonDefaults.itemShape(i, Alarm.TASK_COUNT_CHOICES.size),
                    label = { Text("$count tasks") },
                )
            }
        }
        Text(
            "Different actions you must finish before Snooze and Turn off appear.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun SnoozeLengthPicker(selected: Int, onChange: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Snooze length", style = MaterialTheme.typography.titleSmall)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            Alarm.SNOOZE_CHOICES.forEachIndexed { i, minutes ->
                SegmentedButton(
                    selected = selected == minutes,
                    onClick = { onChange(minutes) },
                    shape = SegmentedButtonDefaults.itemShape(i, Alarm.SNOOZE_CHOICES.size),
                    label = { Text("$minutes") },
                )
            }
        }
        Text(
            "Minutes until a snoozed alarm rings again.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
