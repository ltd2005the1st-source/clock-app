package com.maxalarm.ui.ringing

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxalarm.alarm.AlarmService
import com.maxalarm.alarm.RingSession
import com.maxalarm.ui.theme.MaxAlarmTheme
import kotlinx.coroutines.delay

/**
 * The ringing screen. Back does nothing and the volume keys are swallowed. Snooze and
 * "Turn off for today" appear only after all 20 steps are done; until then there is no way out.
 */
class RingingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* no way out */ }
        })

        setContent {
            MaxAlarmTheme(darkTheme = true) {
                val state by RingSession.state.collectAsStateWithLifecycle()
                var outcome by remember { mutableStateOf<RingOutcome?>(null) }
                val current = state

                LaunchedEffect(current == null, outcome) {
                    if (current == null) {
                        // Nothing ringing: either the user just chose (show the confirmation briefly) or this is stale.
                        if (outcome != null) delay(2_500)
                        finish()
                    }
                }

                RingingScreen(
                    state = current,
                    outcome = outcome,
                    onStepDone = { RingSession.completeStep(it) },
                    // These two only exist on screen after all 20 steps; the service re-checks too.
                    onSnooze = {
                        if (current != null && current.isComplete && outcome == null) {
                            outcome = RingOutcome.Snoozed(current.snoozeMinutes)
                            AlarmService.requestSnooze(this@RingingActivity)
                        }
                    },
                    onTurnOff = {
                        if (current != null && current.isComplete && outcome == null) {
                            outcome = RingOutcome.TurnedOff
                            AlarmService.requestTurnOff(this@RingingActivity)
                        }
                    },
                )
            }
        }
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Volume keys do nothing while the alarm rings (the service would undo the change anyway).
        return when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE,
            KeyEvent.KEYCODE_MUTE,
            KeyEvent.KEYCODE_CAMERA,
            KeyEvent.KEYCODE_FOCUS,
            -> true
            else -> super.dispatchKeyEvent(event)
        }
    }

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, RingingActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
    }
}
