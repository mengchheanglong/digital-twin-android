# Launcher Minimalist Text v2 Spec

## Direction

The Digital Twin launcher should move toward a minimalist, distraction-reducing Android launcher inspired by Oasis/Olauncher-style text launchers.

This means: **calm text, fewer choices, no icon grid, no card-heavy dashboard, no gamified clutter**.

The launcher is still prototype-only for now. Do **not** set it as the default Home app yet.

## Reference Pattern

Observed public patterns from Oasis/Olauncher/minimalist launcher references:

- text-first app access instead of icon grids;
- only a small set of intentional apps on the home screen;
- search or hidden list for everything else;
- minimal visual noise;
- designed to reduce screen time/distraction;
- optional distraction controls can come later.

## Product Goal

Turn the phone home screen into a calm decision surface:

```text
What matters today?
What few apps are allowed right now?
How do I leave safely?
```

## Success Criteria

- Home screen is mostly text.
- No dense cards, icon-grid feeling, or decorative dashboard layout.
- Allowed apps are shown as simple text rows.
- App drawer remains searchable but understated.
- Today state is compressed to 1–3 short text lines.
- Settings and Companion escape controls remain present but visually quiet.
- No default-home automation.
- No secrets, URLs, auth strings, raw journal/chat, or private payloads are displayed.
- No nested scroll/infinite-height Compose crash path.

## UX Shape

### Home

```text
Digital Twin
Tue, Jun 30

Today
Mood · Quest · Check-in status

Allowed
Notes
Calendar
Messages
Maps

all apps
companion · settings
```

Rules:

- App names are text, not buttons with heavy chrome.
- No stars on home.
- No card borders unless needed for readability.
- Max 5 to 7 visible allowed apps.
- If no apps are allowed: show one quiet line: `choose allowed apps`.

### App Drawer

```text
Apps
search

Calendar        add/remove
Messages        add/remove
Notes           add/remove
```

Rules:

- Search field stays.
- Rows are text-first.
- Allowed-app toggle wording should be calm: `allow` / `hide` or `show` / `hide`, not flashy stars.
- Package names should be hidden by default or de-emphasized; they are debug/developer noise.

### Today Area

Today should not become a dashboard. It should compress the cache-backed summary into:

```text
Mood: calm
Quest: finish one focused block
Check-in: pending
```

Deep interaction goes to Companion.

## Explicit Non-Goals

- No icon packs.
- No widgets yet.
- No animated dashboard.
- No charts/analytics.
- No default launcher selection automation.
- No blocking/filtering apps yet; first ship intentional access UI.

## Implementation Plan

1. Rename user-facing copy to allowed/intentional apps where appropriate.
2. Replace the home pinned-app card with a text-only allowed apps section.
3. Simplify header and prototype copy.
4. Reduce Today surface density.
5. Simplify drawer rows: label-first, package text muted or removed, toggle as text action.
6. Update focused contract tests for text-first/minimalist behavior.
7. Build/install/runtime-check by launching `LauncherActivity` directly.

## Verification

```bash
git diff --check
./gradlew --no-daemon --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process :app:testDebugUnitTest --tests com.transcendiverse.digitaltwin.LauncherIntegrationContractTest --tests com.transcendiverse.digitaltwin.LauncherUxModelTest --rerun-tasks
./gradlew --no-daemon --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process lint --rerun-tasks
./gradlew --no-daemon --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process assembleDebug --rerun-tasks
adb -s <phone> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <phone> shell am start -W -n com.transcendiverse.digitaltwin/.launcher.LauncherActivity
adb -s <phone> shell dumpsys window | grep -E 'mCurrentFocus|mFocusedApp'
adb -s <phone> logcat -d | grep -iE 'com.transcendiverse.digitaltwin|FATAL EXCEPTION|AndroidRuntime'
```
