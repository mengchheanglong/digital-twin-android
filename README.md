# Digital Twin Android Companion

This is the first native Android companion scaffold for the Digital Twin web/backend app.

The companion is intentionally small. It renders a phone-first Today view from the same shape exposed by the backend endpoint:

```http
GET /api/mobile/today
Authorization: Bearer <token>
```

The app can run in fixture mode or call the real backend when a backend base URL and JWT/token are saved in Settings. It does not include or hardcode any real token.

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
- Glance home-screen widget v0 backed only by cached Today data

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

When either value is missing, the Today screen renders the local fixture and shows fixture mode. Successful fixture and network loads are cached locally. On app start, a cached Today payload is shown immediately while refresh can run. HTTP failures and invalid JSON are shown as concise refresh-failure status text without blanking an already cached Today view.

Settings are stored in app-private `SharedPreferences` for this v1 slice. Before a real release, token storage should move to encrypted storage such as AndroidX Security encrypted preferences or an equivalent platform-backed credential store.

## Launcher and widget status

This is not a launcher replacement. The native launcher/default-home implementation is deferred so the first Android slice can stabilize the API contract, UI, and privacy boundaries before taking over high-risk device behavior.

Widget v0 is implemented with AndroidX Glance. It reads only the local cached Today payload and displays a privacy-safe summary: app title, mood, streak, quest goal or fallback, next action label, and an open-app refresh hint. It does not fetch from the network, poll in the background, or display token, password, email, backend URL, raw JSON, journal, chat, or reflection content.

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
- kotlinx-serialization-json: `1.11.0`

`androidx.lifecycle:lifecycle-runtime-compose:2.11.0` is intentionally not included in this scaffold because its published metadata requires compile SDK 37 and AGP 9.1+, while this project follows the handoff's compile SDK 36 and AGP 9.0.1 target.

Run:

```bash
./gradlew test
./gradlew lint
./gradlew assembleDebug
```
