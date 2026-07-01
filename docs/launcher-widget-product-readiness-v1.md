# Launcher + Widget Product Readiness v1

## Objective

Move the native Digital Twin phone surface from **validated** to **trustworthy enough for a later supervised default-Home readiness decision**.

This slice does not make Digital Twin the default launcher. It only improves the surfaces that would matter if the user later chooses to test that manually.

## Current baseline

Already verified on the physical phone:

- production backend `/api/mobile/today` works with saved auth;
- Android cache parses live decimal scores;
- companion shows real Today state and complete reflection text;
- launcher can clean-launch and reads cached Today state;
- widget reads cached Today state;
- widget contrast fix makes text readable on the dark phone wallpaper;
- logcat had no crash/network/parser hits.

## Product problem

The system now works, but “works” is not enough for a phone surface.

Before any default-Home trial, the launcher/widget must make the user feel safe:

```text
Can I leave?
Can I find apps?
Can I get back to companion/settings?
Can I trust what Today is telling me?
Is this calm enough to live with?
```

## Success criteria

### Launcher home

- Keeps minimalist text-first direction.
- Shows a compact Today summary.
- Makes escape paths obvious without becoming noisy:
  - apps/app drawer;
  - companion;
  - settings;
  - refresh.
- Keeps allowed apps readable and tappable.
- Empty allowed-app state tells the user what to do next.
- No icon grid, no cards-heavy dashboard, no charts, no analytics.

### App drawer

- Search stays visible.
- Close path stays visible.
- App labels stay readable.
- `allow` / `hide` copy remains calm.
- Package names remain hidden from normal UI.

### Today copy

- Calm, not gamified.
- Do not show raw numbers when a qualitative label is clearer.
- Prefer `Check-in complete` over score-heavy copy unless score is intentionally useful.
- No raw URLs, auth strings, backend debug fields, JSON, journal, chat, or private payload text.

### Widget

- Reads the same cached Today state.
- Looks intentional and readable on dark wallpaper.
- Keeps compact fields:
  - Digital Twin;
  - mood;
  - streak;
  - quest;
  - next action;
  - cached label;
  - tap to refresh.
- No secrets/raw backend/debug fields.

## Explicit non-goals

- Do not request or set default Home.
- Do not open Android role/default-app settings automatically.
- Do not add app blocking/filtering.
- Do not add icon packs, animation, charts, analytics, or full dashboard UI.
- Do not rework companion/backend behavior.
- Do not add notifications.
- Do not introduce DCT/consciousness-transfer claims.

## Allowed implementation shape

Prefer a small visual/product polish diff over architecture churn.

Likely files:

```text
app/src/main/java/com/transcendiverse/digitaltwin/launcher/LauncherScreen.kt
app/src/main/java/com/transcendiverse/digitaltwin/launcher/LauncherDailySurface.kt
app/src/main/java/com/transcendiverse/digitaltwin/launcher/LauncherTodaySummary.kt
app/src/main/java/com/transcendiverse/digitaltwin/launcher/LauncherAppDrawer.kt
app/src/main/java/com/transcendiverse/digitaltwin/widget/TodayGlanceWidget.kt
app/src/test/java/com/transcendiverse/digitaltwin/LauncherIntegrationContractTest.kt
app/src/test/java/com/transcendiverse/digitaltwin/LauncherTodaySummaryTest.kt
app/src/test/java/com/transcendiverse/digitaltwin/TodayWidgetVisualContractTest.kt
```

## Verification plan

Focused local gates:

```bash
git diff --check
./gradlew --no-daemon --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process :app:testDebugUnitTest --tests com.transcendiverse.digitaltwin.LauncherIntegrationContractTest --tests com.transcendiverse.digitaltwin.LauncherTodaySummaryTest --tests com.transcendiverse.digitaltwin.TodayWidgetVisualContractTest --rerun-tasks
./gradlew --no-daemon --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process lint --rerun-tasks
./gradlew --no-daemon --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process assembleDebug --rerun-tasks
```

Runtime QA:

```text
install debug APK
launch MainActivity
clean-launch LauncherActivity with NEW_TASK/CLEAR_TASK flags
capture launcher screenshot
open app drawer and capture screenshot
return to system launcher widget page and capture widget screenshot
scan logcat for FATAL EXCEPTION / AndroidRuntime / parser/network errors
```

## Acceptance decision

After this slice, record one of:

```text
not-ready: blockers listed
ready-for-supervised-test: safety runbook written and user approved
```

Do not enable default Home in this slice.
