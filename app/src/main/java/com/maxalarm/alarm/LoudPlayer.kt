package com.maxalarm.alarm

import com.maxalarm.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.maxalarm.R

/**
 * Plays the alarm on the alarm stream at the device's maximum volume and keeps it there:
 * any change to the alarm volume (volume keys, quick settings, another app) is undone
 * within a fraction of a second for as long as the alarm rings.
 */
class LoudPlayer(private val context: Context) {

    private val audio = context.service<AudioManager>()
    private val notifications = context.service<NotificationManager>()
    private val handler = Handler(Looper.getMainLooper())

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private var player: MediaPlayer? = null
    private var enhancer: LoudnessEnhancer? = null
    private var focusRequest: AudioFocusRequest? = null
    private var previousVolume = -1
    private var previousFilter = -1
    private var receiverRegistered = false

    private val enforcer = object : Runnable {
        override fun run() {
            forceMaxVolume()
            handler.postDelayed(this, ENFORCE_INTERVAL_MS)
        }
    }

    private val volumeChanged = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = forceMaxVolume()
    }

    fun start(ringtoneUri: String?) {
        if (player != null) return
        previousVolume = audio.getStreamVolume(AudioManager.STREAM_ALARM)
        breakThroughDoNotDisturb()
        forceMaxVolume()
        requestFocus()

        // Before the phone is unlocked after a reboot, media sounds may be unreadable;
        // the bundled beep at the end of the list always works.
        val candidates = listOfNotNull(
            ringtoneUri?.let(Uri::parse),
            runCatching { RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM) }.getOrNull(),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            Uri.parse("android.resource://${context.packageName}/${R.raw.alarm_beep}"),
        )
        player = candidates.firstNotNullOfOrNull(::createPlayer)
        player?.let { mp ->
            enhancer = runCatching {
                LoudnessEnhancer(mp.audioSessionId).apply {
                    setTargetGain(LOUDNESS_GAIN_MB)
                    enabled = true
                }
            }.getOrNull()
            mp.start()
        }

        ContextCompat.registerReceiver(
            context, volumeChanged, IntentFilter(VOLUME_CHANGED_ACTION), ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        receiverRegistered = true
        handler.post(enforcer)
    }

    fun stop() {
        handler.removeCallbacks(enforcer)
        if (receiverRegistered) {
            runCatching { context.unregisterReceiver(volumeChanged) }
            receiverRegistered = false
        }
        enhancer?.release()
        enhancer = null
        player?.run {
            runCatching { stop() }
            release()
        }
        player = null
        focusRequest?.let { audio.abandonAudioFocusRequest(it) }
        focusRequest = null
        // Put the alarm volume and Do Not Disturb back the way the user had them.
        if (previousVolume >= 0) {
            runCatching { audio.setStreamVolume(AudioManager.STREAM_ALARM, previousVolume, 0) }
            previousVolume = -1
        }
        if (previousFilter >= 0) {
            runCatching { notifications.setInterruptionFilter(previousFilter) }
            previousFilter = -1
        }
    }

    private fun forceMaxVolume() {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        if (audio.getStreamVolume(AudioManager.STREAM_ALARM) != max) {
            try {
                audio.setStreamVolume(AudioManager.STREAM_ALARM, max, 0)
            } catch (e: SecurityException) {
                Log.w(TAG, "Could not raise alarm volume", e)
            }
        }
        player?.setVolume(1f, 1f)
    }

    /**
     * Alarms already ring through most Do Not Disturb modes. "Total silence", or a priority
     * mode that blocks alarms, would mute them, so switch to "Alarms only" while ringing.
     * Needs the optional Do Not Disturb access the app asks for on its main screen.
     */
    private fun breakThroughDoNotDisturb() {
        if (!notifications.isNotificationPolicyAccessGranted) return
        val filter = notifications.currentInterruptionFilter
        val blocksAlarms = when (filter) {
            NotificationManager.INTERRUPTION_FILTER_NONE -> true
            NotificationManager.INTERRUPTION_FILTER_PRIORITY ->
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                    (notifications.notificationPolicy.priorityCategories and
                        NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS) == 0
            else -> false
        }
        if (blocksAlarms) {
            runCatching {
                notifications.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
                previousFilter = filter
            }
        }
    }

    private fun requestFocus() {
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener { /* keep ringing no matter what */ }
            .build()
        audio.requestAudioFocus(request)
        focusRequest = request
    }

    private fun createPlayer(uri: Uri): MediaPlayer? {
        val mp = MediaPlayer()
        return try {
            mp.setAudioAttributes(attributes)
            mp.setDataSource(context, uri)
            mp.isLooping = true
            mp.setVolume(1f, 1f)
            mp.prepare()
            mp
        } catch (e: Exception) {
            Log.w(TAG, "Cannot play $uri, trying the next sound", e)
            mp.release()
            null
        }
    }

    private companion object {
        const val TAG = "LoudPlayer"
        const val ENFORCE_INTERVAL_MS = 200L
        /** Extra digital gain on top of the maximum stream volume, in millibels. */
        const val LOUDNESS_GAIN_MB = 1200
        const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
    }
}
