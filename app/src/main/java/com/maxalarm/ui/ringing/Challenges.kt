package com.maxalarm.ui.ringing

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.East
import androidx.compose.material.icons.filled.North
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.South
import androidx.compose.material.icons.filled.West
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxalarm.core.ChallengeData
import com.maxalarm.core.ChallengeStep
import com.maxalarm.core.ChallengeType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

/** Shows the action for one snooze step and calls [onDone] once the user has performed it. */
@Composable
fun ChallengeContent(step: ChallengeStep, onDone: () -> Unit) {
    val done by rememberUpdatedState(onDone)
    val finish: () -> Unit = { done() }
    when (step.type) {
        ChallengeType.TAP -> TapChallenge(finish)
        ChallengeType.DOUBLE_TAP -> DoubleTapChallenge(finish)
        ChallengeType.LONG_PRESS -> LongPressChallenge(finish)
        ChallengeType.SWIPE_LEFT -> SwipeChallenge(Direction.LEFT, finish)
        ChallengeType.SWIPE_RIGHT -> SwipeChallenge(Direction.RIGHT, finish)
        ChallengeType.SWIPE_UP -> SwipeChallenge(Direction.UP, finish)
        ChallengeType.SWIPE_DOWN -> SwipeChallenge(Direction.DOWN, finish)
        ChallengeType.MOVING_TARGET -> MovingTargetChallenge(step.seed, finish)
        ChallengeType.TYPE_WORD -> TypeWordChallenge(step.seed, finish)
        ChallengeType.MATH -> MathChallenge(step.seed, finish)
        ChallengeType.SLIDER -> SliderChallenge(finish)
        ChallengeType.SWITCH -> SwitchChallenge(finish)
        ChallengeType.TWO_FINGER_HOLD -> TwoFingerChallenge(finish)
        ChallengeType.COLOR_PICK -> ColorPickChallenge(step.seed, finish)
        ChallengeType.CHECKBOXES -> CheckboxChallenge(finish)
        ChallengeType.CODE_ENTRY -> CodeChallenge(step.seed, finish)
        ChallengeType.SHAKE -> ShakeChallenge(finish)
        ChallengeType.LARGEST_NUMBER -> LargestNumberChallenge(step.seed, finish)
        ChallengeType.NUMBER_ORDER -> NumberOrderChallenge(step.seed, finish)
        ChallengeType.COUNTER -> CounterChallenge(step.seed, finish)
    }
}

// ---------------------------------------------------------------- shared pieces

@Composable
private fun Panel(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    text: String,
    iconModifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    Surface(
        modifier = modifier.fillMaxWidth().height(220.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, modifier = iconModifier.size(56.dp))
                    Spacer(Modifier.height(12.dp))
                }
                Text(text, style = MaterialTheme.typography.titleMedium)
                content()
            }
        }
    }
}

@Composable
private fun ErrorLine(text: String?) {
    Text(
        text ?: " ",
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

// ---------------------------------------------------------------- 1. tap

@Composable
private fun TapChallenge(onDone: () -> Unit) {
    Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(96.dp)) {
        Text("Tap", style = MaterialTheme.typography.headlineSmall)
    }
}

// ---------------------------------------------------------------- 2. double-tap

@Composable
private fun DoubleTapChallenge(onDone: () -> Unit) {
    Panel(
        modifier = Modifier.pointerInput(Unit) { detectTapGestures(onDoubleTap = { onDone() }) },
        icon = Icons.Outlined.TouchApp,
        text = "Double-tap here",
    )
}

// ---------------------------------------------------------------- 3. press and hold

@Composable
private fun LongPressChallenge(onDone: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Box(
        modifier = Modifier
            .size(180.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    val job = scope.launch {
                        progress.animateTo(1f, tween(durationMillis = 2_000, easing = LinearEasing))
                        onDone()
                    }
                    tryAwaitRelease()
                    if (progress.value < 1f) {
                        job.cancel()
                        scope.launch { progress.animateTo(0f) }
                    }
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { progress.value },
            modifier = Modifier.size(164.dp),
            strokeWidth = 10.dp,
        )
        Text("Hold", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

// ---------------------------------------------------------------- 4-7. swipes

private enum class Direction { LEFT, RIGHT, UP, DOWN }

@Composable
private fun SwipeChallenge(direction: Direction, onDone: () -> Unit) {
    var drag by remember { mutableStateOf(Offset.Zero) }
    val threshold = with(LocalDensity.current) { 110.dp.toPx() }
    val icon = when (direction) {
        Direction.LEFT -> Icons.Filled.West
        Direction.RIGHT -> Icons.Filled.East
        Direction.UP -> Icons.Filled.North
        Direction.DOWN -> Icons.Filled.South
    }
    fun matches(d: Offset): Boolean = when (direction) {
        Direction.LEFT -> d.x < -threshold && abs(d.x) > abs(d.y) * 1.5f
        Direction.RIGHT -> d.x > threshold && abs(d.x) > abs(d.y) * 1.5f
        Direction.UP -> d.y < -threshold && abs(d.y) > abs(d.x) * 1.5f
        Direction.DOWN -> d.y > threshold && abs(d.y) > abs(d.x) * 1.5f
    }
    Panel(
        modifier = Modifier.pointerInput(direction) {
            detectDragGestures(
                onDragStart = { drag = Offset.Zero },
                onDragEnd = {
                    if (matches(drag)) onDone()
                    drag = Offset.Zero
                },
                onDragCancel = { drag = Offset.Zero },
            ) { change, amount ->
                change.consume()
                drag += amount
            }
        },
        icon = icon,
        iconModifier = Modifier.offset {
            IntOffset((drag.x / 3).roundToInt().coerceIn(-120, 120), (drag.y / 3).roundToInt().coerceIn(-60, 60))
        },
        text = "Swipe " + direction.name.lowercase(),
    )
}

// ---------------------------------------------------------------- 8. moving target

@Composable
private fun MovingTargetChallenge(seed: Long, onDone: () -> Unit) {
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer),
    ) {
        val dot = 64.dp
        var pos by remember { mutableStateOf(Offset(0.5f, 0.5f)) }
        LaunchedEffect(seed) {
            val r = Random(seed)
            while (true) {
                delay(850)
                pos = Offset(r.nextFloat(), r.nextFloat())
            }
        }
        val x by animateDpAsState((maxWidth - dot) * pos.x, tween(450), label = "x")
        val y by animateDpAsState((maxHeight - dot) * pos.y, tween(450), label = "y")
        Box(
            Modifier
                .offset(x, y)
                .size(dot)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable { onDone() },
        )
    }
}

// ---------------------------------------------------------------- 9. type a word

@Composable
private fun TypeWordChallenge(seed: Long, onDone: () -> Unit) {
    val word = remember(seed) { ChallengeData.word(seed) }
    var text by remember { mutableStateOf("") }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(word, style = MaterialTheme.typography.displaySmall, letterSpacing = 6.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                if (it.trim().equals(word, ignoreCase = true)) onDone()
            },
            singleLine = true,
            label = { Text("Type it here") },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ---------------------------------------------------------------- 10. math

@Composable
private fun MathChallenge(seed: Long, onDone: () -> Unit) {
    var attempt by remember { mutableIntStateOf(0) }
    val problem = remember(seed, attempt) { ChallengeData.math(seed + attempt) }
    var answer by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    fun check() {
        if (answer.toIntOrNull() == problem.answer) {
            onDone()
        } else {
            error = "Not quite. Here's a new one."
            attempt++
            answer = ""
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("${problem.text} = ?", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = answer,
            onValueChange = { answer = it.filter(Char::isDigit).take(4); error = null },
            singleLine = true,
            label = { Text("Answer") },
            isError = error != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { check() }),
            modifier = Modifier.fillMaxWidth(),
        )
        ErrorLine(error)
        Button(onClick = { check() }, enabled = answer.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            Text("Check")
        }
    }
}

// ---------------------------------------------------------------- 11. slider

@Composable
private fun SliderChallenge(onDone: () -> Unit) {
    var value by remember { mutableFloatStateOf(0f) }
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${(value * 100).roundToInt()}%", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Slider(
            value = value,
            onValueChange = { value = it },
            onValueChangeFinished = { if (value >= 0.99f) onDone() else value = 0f },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ---------------------------------------------------------------- 12. switch

@Composable
private fun SwitchChallenge(onDone: () -> Unit) {
    var on by remember { mutableStateOf(false) }
    LaunchedEffect(on) {
        if (on) {
            delay(350)
            onDone()
        }
    }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("I'm awake", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        Switch(checked = on, onCheckedChange = { on = it })
    }
}

// ---------------------------------------------------------------- 13. two-finger hold

@Composable
private fun TwoFingerChallenge(onDone: () -> Unit) {
    var fingers by remember { mutableIntStateOf(0) }
    val progress = remember { Animatable(0f) }
    val holding = fingers >= 2
    LaunchedEffect(holding) {
        if (holding) {
            progress.animateTo(1f, tween(durationMillis = 1_500, easing = LinearEasing))
            onDone()
        } else {
            progress.animateTo(0f, tween(200))
        }
    }
    Panel(
        modifier = Modifier.pointerInput(Unit) {
            awaitEachGesture {
                do {
                    val event = awaitPointerEvent()
                    fingers = event.changes.count { it.pressed }
                    event.changes.forEach { it.consume() }
                } while (event.changes.any { it.pressed })
                fingers = 0
            }
        },
        text = "Fingers down: $fingers of 2",
    ) {
        Spacer(Modifier.height(16.dp))
        CircularProgressIndicator(progress = { progress.value }, modifier = Modifier.size(72.dp), strokeWidth = 8.dp)
    }
}

// ---------------------------------------------------------------- 14. pick a color

private val PALETTE = listOf(
    "red" to Color(0xFFE53935),
    "orange" to Color(0xFFFB8C00),
    "yellow" to Color(0xFFFDD835),
    "green" to Color(0xFF43A047),
    "blue" to Color(0xFF1E88E5),
    "purple" to Color(0xFF8E24AA),
)

@Composable
private fun ColorPickChallenge(seed: Long, onDone: () -> Unit) {
    val (order, target) = remember(seed) { ChallengeData.colorPick(seed, PALETTE.size) }
    var error by remember { mutableStateOf<String?>(null) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(PALETTE[target].first.uppercase(), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        order.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { i ->
                    Box(
                        Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(PALETTE[i].second)
                            .clickable { if (i == target) onDone() else error = "That's ${PALETTE[i].first}." },
                    )
                }
            }
        }
        ErrorLine(error)
    }
}

// ---------------------------------------------------------------- 15. checkboxes

private val CHECK_ITEMS = listOf("Eyes open", "Sitting up", "Feet on the floor", "Lights on", "Ready to get up")

@Composable
private fun CheckboxChallenge(onDone: () -> Unit) {
    val checked = remember { mutableStateListOf(*Array(CHECK_ITEMS.size) { false }) }
    val all = checked.all { it }
    LaunchedEffect(all) {
        if (all) {
            delay(300)
            onDone()
        }
    }
    Column(Modifier.fillMaxWidth()) {
        CHECK_ITEMS.forEachIndexed { i, label ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { checked[i] = !checked[i] }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = checked[i], onCheckedChange = { checked[i] = it })
                Text(label, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// ---------------------------------------------------------------- 16. enter a code

@Composable
private fun CodeChallenge(seed: Long, onDone: () -> Unit) {
    val code = remember(seed) { ChallengeData.code(seed) }
    var entered by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    fun press(d: Char) {
        error = null
        entered = (entered + d).take(code.length)
        if (entered.length == code.length) {
            if (entered == code) onDone() else {
                error = "Wrong code. Try again."
                entered = ""
            }
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(code.toList().joinToString("  "), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text(
            entered.padEnd(code.length, '_').toList().joinToString("  "),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        ErrorLine(error)
        listOf("123", "456", "789", " 0<").forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { key ->
                    when (key) {
                        ' ' -> Spacer(Modifier.size(width = 72.dp, height = 52.dp))
                        '<' -> FilledTonalButton(
                            onClick = { entered = entered.dropLast(1) },
                            modifier = Modifier.size(width = 72.dp, height = 52.dp),
                        ) { Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = "Delete") }
                        else -> FilledTonalButton(
                            onClick = { press(key) },
                            modifier = Modifier.size(width = 72.dp, height = 52.dp),
                        ) { Text(key.toString(), style = MaterialTheme.typography.titleLarge) }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- 17. shake

@Composable
private fun ShakeChallenge(onDone: () -> Unit) {
    val context = LocalContext.current
    val sensors = remember { context.getSystemService(SensorManager::class.java) }
    val accelerometer = remember { sensors?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    if (sensors == null || accelerometer == null) {
        RapidTapFallback(onDone)
        return
    }
    val target = 12
    var shakes by remember { mutableIntStateOf(0) }
    DisposableEffect(accelerometer) {
        var last = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val (x, y, z) = event.values
                val force = abs(sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH)
                val now = SystemClock.elapsedRealtime()
                if (force > 9f && now - last > 180) {
                    last = now
                    shakes++
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensors.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensors.unregisterListener(listener) }
    }
    LaunchedEffect(shakes) { if (shakes >= target) onDone() }
    Column(
        Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(Icons.Outlined.Vibration, contentDescription = null, modifier = Modifier.size(64.dp))
        LinearProgressIndicator(
            progress = { (shakes.toFloat() / target).coerceAtMost(1f) },
            modifier = Modifier.fillMaxWidth().height(12.dp),
        )
        Text("${minOf(shakes, target)} of $target shakes", style = MaterialTheme.typography.titleMedium)
    }
}

/** Used instead of shaking on devices without an accelerometer. */
@Composable
private fun RapidTapFallback(onDone: () -> Unit) {
    val target = 15
    var taps by remember { mutableIntStateOf(0) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("No motion sensor. Tap $target times instead.", style = MaterialTheme.typography.bodyLarge)
        Button(onClick = { taps++; if (taps >= target) onDone() }, modifier = Modifier.fillMaxWidth().height(96.dp)) {
            Text("$taps / $target", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

// ---------------------------------------------------------------- 18. largest number

@Composable
private fun LargestNumberChallenge(seed: Long, onDone: () -> Unit) {
    val numbers = remember(seed) { ChallengeData.numberGrid(seed) }
    val largest = numbers.max()
    var error by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        numbers.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { n ->
                    OutlinedButton(
                        onClick = { if (n == largest) onDone() else error = "$n isn't the biggest." },
                        modifier = Modifier.size(width = 88.dp, height = 60.dp),
                    ) { Text("$n", style = MaterialTheme.typography.titleLarge) }
                }
            }
        }
        ErrorLine(error)
    }
}

// ---------------------------------------------------------------- 19. tiles in order

@Composable
private fun NumberOrderChallenge(seed: Long, onDone: () -> Unit) {
    val tiles = remember(seed) { ChallengeData.tileOrder(seed) }
    var next by remember { mutableIntStateOf(1) }
    var error by remember { mutableStateOf<String?>(null) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Next: $next", style = MaterialTheme.typography.titleMedium)
        tiles.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { n ->
                    val doneTile = n < next
                    Box(
                        Modifier
                            .width(88.dp)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (doneTile) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainerHighest,
                            )
                            .border(
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                RoundedCornerShape(20.dp),
                            )
                            .clickable(enabled = !doneTile) {
                                if (n == next) {
                                    error = null
                                    next++
                                    if (next > tiles.size) onDone()
                                } else {
                                    error = "Wrong tile. Start again from 1."
                                    next = 1
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "$n",
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (doneTile) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
        ErrorLine(error)
    }
}

// ---------------------------------------------------------------- 20. counter

@Composable
private fun CounterChallenge(seed: Long, onDone: () -> Unit) {
    val target = remember(seed) { ChallengeData.counterTarget(seed) }
    var value by remember { mutableIntStateOf(0) }
    LaunchedEffect(value) {
        if (value == target) {
            delay(400)
            onDone()
        }
    }
    Column(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Target: $target", style = MaterialTheme.typography.titleLarge)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            FilledIconButton(onClick = { value = (value - 1).coerceAtLeast(0) }, modifier = Modifier.size(64.dp)) {
                Icon(Icons.Filled.Remove, contentDescription = "Minus one")
            }
            Text(
                "$value",
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.width(96.dp),
                textAlign = TextAlign.Center,
            )
            FilledIconButton(onClick = { value++ }, modifier = Modifier.size(64.dp)) {
                Icon(Icons.Filled.Add, contentDescription = "Plus one")
            }
        }
    }
}
