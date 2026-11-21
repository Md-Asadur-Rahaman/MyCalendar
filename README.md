# MyCalendar Android

Android WebView wrapper for the existing `MyCalendar_Enhanced.html`. The original HTML file is preserved in the repository and copied unchanged into `app/src/main/assets/`, where the app loads it locally without needing a web server.

## Create the permanent signing key (once)

1. Push this repository to GitHub.
2. Open **Actions → Create MyCalendar Signing Keystore → Run workflow**.
3. Download the `MyCalendar-Signing-Setup` artifact immediately (it expires after one day).
4. Store `mycalendar-release.jks` safely offline. Never commit it or share it publicly.
5. In **Settings → Secrets and variables → Actions**, add these repository secrets:
   - `ANDROID_KEYSTORE_BASE64`: the complete one-line contents of `keystore-base64.txt`
   - `ANDROID_KEYSTORE_PASSWORD`: value of the same name in `credentials.txt`
   - `ANDROID_KEY_ALIAS`: value of the same name in `credentials.txt` (normally `mycalendar`)
   - `ANDROID_KEY_PASSWORD`: value of the same name in `credentials.txt`

## Build the signed APK

1. Open **Actions → Build Signed MyCalendar APK → Run workflow**.
2. Wait for the workflow to finish successfully.
3. Download **MyCalendar-Signed-APK** from the run's Artifacts section.

A push to `main` or `master` also starts the signed release workflow. The workflow intentionally stops with a clear error until all four signing secrets are configured.

**Important:** Back up the keystore and passwords securely. Every future update must use the same signing key. Losing it generally prevents updating an existing installation signed with that key.

## Local build

Requires JDK 17 and Gradle 8.7:

```bash
gradle assembleDebug
```

For a locally signed release, set `ANDROID_KEYSTORE_PATH`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and `ANDROID_KEY_PASSWORD`, then run `gradle assembleRelease`.

## Included Android improvements

- Bundles the original calendar HTML locally.
- Enables JavaScript and DOM storage for the calendar's interactive features and saved events/themes.
- Uses AndroidX WebKit's asset loader and a local HTTPS origin.
- Adds Android back navigation, a native launcher icon, and a dark native window theme.
- Keeps signing credentials out of source control.
