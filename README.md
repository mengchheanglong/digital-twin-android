# Digital Twin Android Companion

This is the first native Android companion scaffold for the Digital Twin web/backend app.

The companion is intentionally small. It renders a phone-first Today view from the same shape exposed by the backend endpoint:

```http
GET /api/mobile/today
Authorization: Bearer <token>
```

The app can run in fixture mode or call the real backend when a backend base URL and JWT/token are saved in Settings. The production backend base URL is `https://digital-twin-orcin-omega.vercel.app`. It does not include or hardcode any real token.

## Included

- Kotlin Android app with Jetpack Compose
- Package `com.transcendiverse.digitaltwin`
- App name `Digital Twin`
- Serializable `MobileToday` domain models
- Fixture-backed Today repository and OkHttp-backed network repository
- Today screen with mood, streak, check-in status, quest status, next action, reflection, and launcher action labels
- Settings panel for backend base URL and JWT/token input, with Save and Refresh actions
- Unit tests for serialization, action priority, privacy guardrails, HTTP request/response behavior, and settings storage
- Local cache for the last successful Today payload
- Glance home-screen widget backed by cached Today data with a manual background refresh action
- WorkManager one-shot and periodic background Today sync using saved backend credentials

## Mobile Today client

The Android contract matches the backend response envelope:

```json
{
  "success": true,
  "today": {
    "version": "mobile-today.v0"
  }
}
```

When both Settings values are present, Refresh calls:

```http
GET /api/mobile/today
Authorization: Bearer <token>
Accept: application/json
```

When either value is missing, the Today screen renders the local fixture and shows fixture mode. Successful fixture and network loads are cached locally. On app start, a cached Today payload is shown immediately while refresh can run. HTTP failures and invalid JSON are shown as concise refresh-failure status text without blanking an already cached Today view. The screen also shows cached/last-updated status when cache metadata exists.

Background Today sync is implemented through WorkManager. App startup schedules periodic sync, login schedules periodic sync and enqueues an immediate sync, and manual/app/widget refresh actions enqueue one-time sync. The worker skips safely when saved credentials are missing.

Settings are stored in app-private `SharedPreferences` for this v1 slice. Before a real release, token storage should move to encrypted storage such as AndroidX Security encrypted preferences or an equivalent platform-backed credential store.

## Launcher prototype

Launcher mode means Digital Twin can be selected as an Android Home app. It is still the same companion app package, but Android may offer it in the Home app chooser when the launcher prototype entry point is present.

Do not set Digital Twin as the default Home app until the app drawer and Settings escape path have both been verified on your device. A launcher controls the phone's home surface, so keep a known-good way back to the system launcher before using it as default.

To enable the prototype as Home:

1. Install the debug build on the device.
2. Open Android Settings.
3. Go to Settings -> Apps -> Default apps -> Home app -> Digital Twin.
4. Press Home and confirm the Digital Twin launcher surface opens.

To switch back to the system launcher:

1. Open Android Settings from the launcher escape control or the system notification shade.
2. Go to Settings -> Apps -> Default apps -> Home app.
3. Select the device's original launcher, such as Pixel Launcher, One UI Home, or the OEM launcher.
4. Press Home and confirm the normal system launcher opens.

Real secrets must not be committed. Keep JWTs, passwords, signing keys, release keystores, Play deployment credentials, API tokens, and `.env` values out of the repository.

## Widget status

The widget is implemented with AndroidX Glance. It reads only the local cached Today payload and displays a privacy-safe summary: app title, mood, streak, quest goal or fallback, next action label, cache label, and a tap-to-refresh action. The refresh action enqueues WorkManager sync and then refreshes the widget from cache. It does not display token, password, email, backend URL, raw JSON, journal, chat, or reflection content.

## Build

Recommended versions used by this scaffold:

- Gradle wrapper: `9.1.0`
- Android Gradle Plugin: `9.0.1`
- Kotlin: `2.3.20`
- compileSdk: `36`
- minSdk: `26`
- targetSdk: `36`
- Compose BOM: `2026.06.00`
- Activity Compose: `1.13.0`
- AndroidX Glance AppWidget: `1.1.1`
- AndroidX WorkManager: `2.11.0`
- kotlinx-serialization-json: `1.11.0`

`androidx.lifecycle:lifecycle-runtime-compose:2.11.0` is intentionally not included in this scaffold because its published metadata requires compile SDK 37 and AGP 9.1+, while this project follows the handoff's compile SDK 36 and AGP 9.0.1 target.

Run:

```bash
./gradlew test
./gradlew lint
./gradlew assembleDebug
```

For a full local verification pass from Windows Git Bash:

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export ANDROID_HOME="/c/Users/User/AppData/Local/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
git diff --check
./gradlew test --rerun-tasks
./gradlew lint --rerun-tasks
./gradlew assembleDebug --rerun-tasks
```

Install the debug APK on a connected device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
