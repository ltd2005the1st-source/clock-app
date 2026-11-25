package com.maxalarm.alarm

import com.maxalarm.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.maxalarm.MaxAlarmApp
import com.maxalarm.R
import com.maxalarm.core.Alarm
import com.maxalarm.core.SNOOZE_STEPS
import com.maxalarm.data.AlarmStore
import com.maxalarm.ui.ringing.RingingActivity

/**
 * Foreground service that owns a ringing alarm. It has no "stop" or "dismiss" action:
 * the only way out is [ACTION_SNOOZE], which it accepts only after all 20 steps are done.
 */
class AlarmService : Service() {

    private lateinit var store: AlarmStore
    private var player: LoudPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        store = AlarmStore.get(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val ringing = store.ringing()
        val isSnooze = intent?.action == ACTION_SNOOZE
        // Set up the session before the full-screen notification can open the ringing screen.
        if (!isSnooze && ringing.isNotEmpty()) RingSession.ensure(ringing)
        // Always go foreground first: a service started with startForegroundService must.
        try {
            startInForeground(buildNotification(ringing))
        } catch (e: RuntimeException) {
            // Android can refuse a foreground start from the background (for example when the
            // system restarts us after killing us). Hand the alarm back to AlarmManager, whose
            // alarm-clock broadcasts are allowed to start us, so it rings again in a second.
            if (!isSnooze) {
                store.clearRinging()
                ringing.forEach {
                    AlarmScheduler.scheduleSnooze(this, store.addSnooze(it, System.currentTimeMillis() + 1_000))
                }
                RingSession.clear()
            }
            stopSelf()
            return START_NOT_STICKY
        }

        if (isSnooze) {
            val session = RingSession.state.value
            if (session != null && session.isComplete) {
                snooze(session.alarms.ifEmpty { ringing }, session.snoozeMinutes)
                return START_NOT_STICKY
            }
            // Snooze asked for without finishing the steps: ignore it and keep ringing.
        }

        if (ringing.isEmpty()) {
            finish()
            return START_NOT_STICKY
        }

        RingSession.ensure(ringing)
        startRinging(ringing.last())
        // START_STICKY: if the system kills the service, it comes back and rings again.
        return START_STICKY
    }

    private fun startRinging(alarm: Alarm) {
        if (wakeLock == null) {
            wakeLock = service<PowerManager>()
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MaxAlarm:ringing")
                .apply { setReferenceCounted(false); acquire() }
        }
        if (player == null) {
            player = LoudPlayer(this).also { it.start(alarm.ringtoneUri) }
        }
        if (vibrator == null && store.ringing().any { it.vibrate }) {
            startVibrating()
        }
        launchRingingScreen()
    }

    private fun launchRingingScreen() {
        // Works when the app is in the foreground; otherwise the full-screen notification opens it.
        runCatching { startActivity(RingingActivity.intent(this)) }
    }

    private fun snooze(alarms: List<Alarm>, minutes: Int) {
        val at = System.currentTimeMillis() + minutes * 60_000L
        alarms.forEach { AlarmScheduler.scheduleSnooze(this, store.addSnooze(it, at)) }
        finish()
    }

    private fun finish() {
        store.clearRinging()
        RingSession.clear()
        player?.stop()
        player = null
        vibrator?.cancel()
        vibrator = null
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        // If the system destroys us while ringing, START_STICKY brings the service back and
        // the persisted ringing set makes it ring again.
        player?.stop()
        vibrator?.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }

    private fun startVibrating() {
        val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            service<VibratorManager>().defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            service<Vibrator>()
        }
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 800, 400), 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(
                effect,
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
            )
        }
        vibrator = v
    }

    private fun startInForeground(notification: Notification) {
        val type = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            else -> 0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    private fun buildNotification(ringing: List<Alarm>): Notification {
        val open = PendingIntent.getActivity(
            this, 0, RingingActivity.intent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = ringing.lastOrNull()?.label?.takeIf { it.isNotBlank() } ?: "Alarm"
        return NotificationCompat.Builder(this, MaxAlarmApp.CHANNEL_RINGING)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(title)
            .setContentText("Complete all $SNOOZE_STEPS steps to snooze. Tap to open.")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(open)
            .setFullScreenIntent(open, true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 42
        const val ACTION_RING = "com.maxalarm.action.START_RINGING"
        const val ACTION_SNOOZE = "com.maxalarm.action.SNOOZE"

        fun ring(context: Context, alarm: Alarm) {
            AlarmStore.get(context).addRinging(alarm)
            ContextCompat.startForegroundService(
                context, Intent(context, AlarmService::class.java).setAction(ACTION_RING),
            )
        }

        /** Called by the ringing screen once all 20 steps are done. */
        fun requestSnooze(context: Context) {
            ContextCompat.startForegroundService(
                context, Intent(context, AlarmService::class.java).setAction(ACTION_SNOOZE),
            )
        }
    }
}
