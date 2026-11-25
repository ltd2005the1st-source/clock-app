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
 * The ringing screen. There is no dismiss button, Back does nothing and the volume keys
 * are swallowed. The 20 snooze steps are the only way out.
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
                var snoozedFor by remember { mutableStateOf<Int?>(null) }
                val current = state

                LaunchedEffect(current?.isComplete) {
                    if (current != null && current.isComplete && snoozedFor == null) {
                        snoozedFor = current.snoozeMinutes
                        AlarmService.requestSnooze(this@RingingActivity)
                    }
                }
                LaunchedEffect(current == null, snoozedFor) {
                    if (current == null) {
                        // Nothing ringing: either we just snoozed (show the confirmation briefly) or this is stale.
                        if (snoozedFor != null) delay(2_500)
                        finish()
                    }
                }

                RingingScreen(
                    state = current,
                    snoozedFor = snoozedFor,
                    onStepDone = { RingSession.completeStep(it) },
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
