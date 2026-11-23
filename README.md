# MyCalendar 2.0

বাংলা ও ইংরেজি ক্যালেন্ডার, বাংলাদেশ revised civil Bangla date, location-based sunrise/sunset and prayer-time estimates, persistent settings, Liquid Glass / Nothing OS themes, animated day/night sky, and Android event reminders.

## Build locally

Requires JDK 17 and Gradle 8.7.

```bash
gradle assembleDebug
```

## Signed APK with GitHub Actions

1. Run **Create MyCalendar Signing Keystore** once.
2. Download and securely back up the `MyCalendar-Signing-Setup` artifact.
3. Add repository Actions secrets:
   - `ANDROID_KEYSTORE_BASE64` = complete content of `keystore-base64.txt`
   - `ANDROID_KEYSTORE_PASSWORD` = password in `credentials.txt`
   - `ANDROID_KEY_ALIAS` = `mycalendar`
   - `ANDROID_KEY_PASSWORD` = password in `credentials.txt`
4. Run **Build Signed MyCalendar APK**.
5. Download `MyCalendar-Signed-APK` from the workflow run's Artifacts section.

Keep the keystore and passwords private. Back them up: future updates must use the same signing key.

## Notes

- Sunrise, sunset and prayer times are calculated locally from selected division coordinates or GPS. They are estimates; prayer calculation conventions vary. Confirm religious observance times with a trusted local mosque.
- Android notifications require notification permission. Exact alarm availability varies by Android version and device power settings; the app falls back to inexact idle-compatible alarms where necessary.
- GPS requires location permission and device location services. Division selection remains available as a manual fallback.
