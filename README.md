# Digital Twin Android Companion

This is the first native Android companion scaffold for the Digital Twin web/backend app.

The companion is intentionally small. It renders a phone-first Today view from the same shape exposed by the backend endpoint:

```http
GET /api/mobile/today
Authorization: Bearer <token>
```

The current app uses a fake fixture repository instead of making network calls. That keeps this v0 build local, testable, and free of hardcoded secrets while preserving the typed `MobileToday` contract that the real backend client can consume later.

## Included

- Kotlin Android app with Jetpack Compose
- Package `com.transcendiverse.digitaltwin`
- App name `Digital Twin`
- Serializable `MobileToday` domain models
- Fixture-backed Today repository
- Today screen with mood, streak, check-in status, quest status, next action, reflection, and launcher action labels
- Settings placeholders for backend base URL and JWT/token input
- Unit tests for serialization, action priority, and privacy guardrails

## Launcher and widget status

This is not a launcher replacement. The native launcher/default-home implementation is deferred so the first Android slice can stabilize the API contract, UI, and privacy boundaries before taking over high-risk device behavior.

Widget implementation is also deferred from code in this scaffold. The next exact widget plan is documented in `docs/widget-v0.md`.

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
- kotlinx-serialization-json: `1.11.0`

`androidx.lifecycle:lifecycle-runtime-compose:2.11.0` is intentionally not included in this scaffold because its published metadata requires compile SDK 37 and AGP 9.1+, while this project follows the handoff's compile SDK 36 and AGP 9.0.1 target.

Run:

```bash
./gradlew test
./gradlew lint
./gradlew assembleDebug
```
