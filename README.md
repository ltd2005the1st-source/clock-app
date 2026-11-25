# Max Alarm

An Android alarm clock (Kotlin, Jetpack Compose, Material 3 with Material You dynamic color) built around two rules:

1. **Every alarm rings at the device's maximum alarm volume, and that can't be changed.** There is no volume setting. While an alarm rings, the app sets the alarm stream to its maximum and puts it back to maximum within 0.2 s of any change (volume keys, quick settings, other apps). It also adds a little digital gain on top (`LoudnessEnhancer`). The volume keys do nothing on the ringing screen. Once the alarm is snoozed, the alarm volume goes back to where you had it.
2. **A ringing alarm can only be silenced by 20 different actions.** No dismiss or snooze button, no actions in the notification, Back does nothing. Each ring presents all 20 actions in a fresh random order. Only after the last one do two buttons appear: **Snooze** and **Turn off for today**. The alarm keeps ringing until you pick one.

## The 20 actions

Tap · double-tap · press and hold · swipe left · swipe right · swipe up · swipe down · catch a moving dot · type a word · solve a sum · drag a slider to the end · flip a switch · hold with two fingers · pick a named color · tick five boxes · key in a code · shake the phone · tap the largest number · tap tiles 1–6 in order · set a counter to a target.

## After the 20 actions

- **Snooze** stops the sound for the alarm's snooze length (1, 5, 10, 15 or 30 minutes, set per alarm). Then it rings again and needs all 20 actions again. A snooze can't be cancelled from the app, and deleting or switching off the alarm doesn't cancel a snooze already pending.
- **Turn off for today** stops it with no snooze. A repeating alarm rings again on its next day; a one-off alarm is done.

If the phone is switched off while an alarm is ringing, it rings again a few seconds after the phone boots, including before you unlock it.

## Other features

- Alarm list with next-alarm countdown, one-off or repeating alarms (pick days), labels, per-alarm sound (no "Silent" option) and vibration.
- A setup card on the main screen for the permissions that keep alarms reliable: notifications, full-screen alarms (Android 14+), exact alarms (Android 12), battery optimization, and optional Do Not Disturb access so alarms ring even in "Total silence".
- Bundled fallback beep if the chosen sound can't be played.

## Build

Open the folder in Android Studio (Ladybug or newer) and run, or from a terminal with the Android SDK installed:

```
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # scheduling + challenge logic tests
```

Pushing this to GitHub builds both APKs automatically (`.github/workflows/build.yml`); download them from the run's artifacts.

Min Android 8.0 (API 26), target Android 15 (API 35).

## Limits Android won't let any app get around

- **Force stop or uninstall** in system settings always stops the app, and with it the alarm.
- **Turning the phone off** stops the sound until it boots again (then it rings again).
- On Android 14+ the user can revoke **full-screen alarms**; the alarm still rings, but appears as a notification you tap to reach the steps.
- Some manufacturers (Xiaomi, Huawei, Samsung and others) kill background apps aggressively; allow "no battery restrictions" for reliable alarms.
- **Do Not Disturb "Total silence"** mutes alarms unless you grant Do Not Disturb access.
- The power button can't be intercepted, but locking the screen doesn't stop the alarm.
- Headphones: Android plays the alarm stream on the speaker as well when headphones are connected on most phones, but this is up to the manufacturer.
- Very old or unusual devices without an accelerometer get "tap 15 times" in place of the shake step.

## Code map

- `core/` platform-free logic: `Alarm`, next-ring calculation, the 20 challenge types and puzzle generation (unit tested).
- `data/` storage in device-protected SharedPreferences (works before first unlock).
- `alarm/` `AlarmScheduler` (AlarmManager alarm clocks), `AlarmReceiver`, `BootReceiver`, `AlarmService` (foreground service), `LoudPlayer` (max-volume enforcement), `RingSession` (step progress).
- `ui/` list and edit screens, `ui/ringing/` ringing screen and the 20 challenge composables.
