package com.maxalarm.ui.ringing

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maxalarm.alarm.RingSession
import com.maxalarm.core.SNOOZE_STEPS
import com.maxalarm.ui.TimeText
import kotlinx.coroutines.delay
import java.time.LocalTime

@Composable
fun RingingScreen(
    state: RingSession.State?,
    snoozedFor: Int?,
    onStepDone: (Int) -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Clock(label = state?.alarms?.lastOrNull()?.label)
            when {
                snoozedFor != null -> SnoozedMessage(snoozedFor)
                state != null -> Steps(state, onStepDone)
            }
        }
    }
}

@Composable
private fun Clock(label: String?) {
    val context = LocalContext.current
    val now by produceState(LocalTime.now()) {
        while (true) {
            value = LocalTime.now()
            delay(1_000)
        }
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            TimeText.full(context, now.hour, now.minute),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            label?.takeIf { it.isNotBlank() } ?: "Alarm",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Steps(state: RingSession.State, onStepDone: (Int) -> Unit) {
    val progress by animateFloatAsState(state.completed / SNOOZE_STEPS.toFloat(), label = "progress")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(
                "Step ${minOf(state.completed + 1, SNOOZE_STEPS)} of $SNOOZE_STEPS",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                "Snooze ${state.snoozeMinutes} min",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp))
    }

    val step = state.current ?: return
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (slideInHorizontally { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut())
            },
            label = "step",
        ) { s ->
            Column(
                // No scrolling here: the swipe challenges need vertical drags to themselves.
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(s.type.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    s.type.instruction,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    ChallengeContent(s) { onStepDone(s.index) }
                }
            }
        }
    }
    Text(
        "There's no dismiss button. Finish every step to snooze.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SnoozedMessage(minutes: Int) {
    val context = LocalContext.current
    val at = LocalTime.now().plusMinutes(minutes.toLong())
    Column(
        Modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Outlined.Snooze,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text("Snoozed", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Rings again at ${TimeText.full(context, at.hour, at.minute)}.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
