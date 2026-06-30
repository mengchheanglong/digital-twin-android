# Launcher v0 Mass Deployment Spec

## Objective

Build a safe native Android launcher prototype for Digital Twin. This means Digital Twin can appear as a selectable Android Home app while preserving the existing companion app, widget, login, check-in, background sync, and quest actions.

This is not just an app icon. It is a phone home surface.

## Success criteria

A build is acceptable when all of these are true:

1. Android offers Digital Twin as a Home app candidate.
2. Existing `MainActivity` remains the normal app launcher entry point.
3. New launcher activity is safe, exported, and declares HOME/DEFAULT categories.
4. Launcher shell shows a daily operating surface using existing cached Today data where available.
5. Launcher shell has an app drawer that can list launchable installed apps and open one.
6. Launcher shell has escape/system controls:
   - Open Android Settings.
   - Open Digital Twin companion app.
   - Clear visual indication that this is a prototype/safe launcher.
7. Launcher does not auto-set itself as default Home app. The user must choose it.
8. No secrets are displayed or logged.
9. Existing widget, app login, check-in, quest progress, and background sync continue to compile and pass tests.
10. Final integrated main passes:

```bash
git diff --check
./gradlew test --rerun-tasks
./gradlew lint --rerun-tasks
./gradlew assembleDebug --rerun-tasks
```

11. Final APK is installed and launched on the connected phone once.

## Operator checklist

Use this checklist before asking anyone to select Digital Twin as the device Home app.

### Build gates

- Confirm the worktree contains no secrets, generated release artifacts, keystores, token values, passwords, or `.env` files.
- Confirm the normal companion app entry point still exists and the launcher prototype does not auto-select itself as default Home.
- Run the required local gates:

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export ANDROID_HOME="/c/Users/User/AppData/Local/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
git diff --check
./gradlew test --rerun-tasks
./gradlew lint --rerun-tasks
./gradlew assembleDebug --rerun-tasks
```

### Install command

Install only after the build gates pass:

```bash
adb devices -l
adb -s <phone-serial> install -r app/build/outputs/apk/debug/app-debug.apk
```

### Verify HOME resolver command

Confirm Android can resolve Digital Twin as a Home candidate:

```bash
adb -s <phone-serial> shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME com.transcendiverse.digitaltwin
```

Expected result: the resolver output names the Digital Twin package or launcher activity. If it does not, do not continue to default Home selection.

### Manual phone checks

- Launch the normal companion app and confirm login, Today, check-in, quest actions, widget refresh, and background sync still behave as expected.
- Open Settings -> Apps -> Default apps -> Home app and confirm Digital Twin appears as a selectable Home app.
- Select Digital Twin only on a test device after the app drawer and Settings escape controls are visible.
- Press Home and confirm the Digital Twin launcher surface opens with a clear prototype warning.
- Open Android Settings from the launcher escape control.
- Open the Digital Twin companion app from the launcher escape control.
- Open the app drawer, verify installed apps are listed, and launch at least one non-Digital Twin app.
- Confirm no token, password, email, backend URL, raw JSON, journal, chat, or reflection content appears on the launcher surface.

### Rollback and switch-back steps

- Preferred switch-back: open Android Settings -> Apps -> Default apps -> Home app, then select the system launcher such as Pixel Launcher, One UI Home, or the OEM launcher.
- If the launcher UI is usable but not ready, switch back to the system launcher and keep Digital Twin installed only as a selectable prototype.
- If the launcher blocks normal use, open Settings from the notification shade or via ADB:

```bash
adb -s <phone-serial> shell am start -a android.settings.SETTINGS
```

- If needed, uninstall the debug build from ADB:

```bash
adb -s <phone-serial> uninstall com.transcendiverse.digitaltwin
```

- After rollback, press Home and confirm the original system launcher opens.

## Non-goals

- Do not force Digital Twin to become default Home app.
- Do not remove the existing normal app icon flow.
- Do not build a full production launcher with drag/drop icons, folders, wallpaper, gesture navigation, or custom widgets.
- Do not add Play Store signing, release keystores, or secrets.
- Do not store or expose password/token/email/backend URL in launcher UI beyond already-safe app settings screens.

## Architecture

### Existing entry points

- `com.transcendiverse.digitaltwin.MainActivity`
  - Keeps `MAIN` + `LAUNCHER`.
  - Existing companion app screen.

- `com.transcendiverse.digitaltwin.widget.TodayGlanceWidgetReceiver`
  - Existing widget receiver.

### New entry point

- `com.transcendiverse.digitaltwin.launcher.LauncherActivity`
  - New ComponentActivity.
  - Declares `MAIN`, `HOME`, and `DEFAULT` intent categories.
  - Uses `SharedPreferencesTodayCacheStore` to render cached Today summary.
  - Uses `TodaySyncScheduler`/`TodayWidgetUpdater` where safe for refresh triggers.
  - Must not require login to render. If signed out/no cache, show safe empty state.

### Launcher package

New package:

```text
app/src/main/java/com/transcendiverse/digitaltwin/launcher/
```

Recommended files:

```text
LauncherActivity.kt
LauncherScreen.kt
LauncherAppDrawer.kt
LauncherAppsRepository.kt
LauncherSafetyActions.kt
LauncherTodaySummary.kt
```

Keep pure logic testable without Android instrumentation where possible.

## Parallel task plan

### Task A — Home shell and manifest

Owns launcher registration and top-level activity.

Allowed primary files:

```text
app/src/main/AndroidManifest.xml
app/src/main/java/com/transcendiverse/digitaltwin/launcher/LauncherActivity.kt
app/src/main/java/com/transcendiverse/digitaltwin/launcher/LauncherScreen.kt
app/src/main/res/values/strings.xml
app/src/test/java/com/transcendiverse/digitaltwin/LauncherManifestContractTest.kt
```

Acceptance:

- Manifest has existing `MainActivity` launcher unchanged.
- Manifest adds `.launcher.LauncherActivity` with `MAIN`, `HOME`, `DEFAULT`.
- Launcher activity renders a Compose shell with title, prototype warning, Today placeholder, App Drawer button/slot, Settings button, Companion button.
- Unit test or static manifest test verifies both LAUNCHER and HOME registrations.

### Task B — App drawer and app launch model

Owns app listing/opening logic.

Allowed primary files:

```text
app/src/main/java/com/transcendiverse/digitaltwin/launcher/LauncherAppsRepository.kt
app/src/main/java/com/transcendiverse/digitaltwin/launcher/LauncherAppDrawer.kt
app/src/test/java/com/transcendiverse/digitaltwin/LauncherAppsRepositoryTest.kt
app/src/test/java/com/transcendiverse/digitaltwin/LauncherAppDrawerModelTest.kt
```

Acceptance:

- Defines `LauncherApp` model with packageName, label, optional activityName.
- Defines pure sort/filter helpers: alphabetical, remove self from default list if necessary, stable labels.
- Android repository can query launchable apps via PackageManager with `ACTION_MAIN` + `CATEGORY_LAUNCHER`.
- Android repository can build launch intents using PackageManager or explicit component.
- Compose drawer can show apps and call a callback to launch.
- Tests cover pure sort/filter intent model behavior without needing a device.

### Task C — Daily operating surface

Owns Today/cache summary rendering for launcher.

Allowed primary files:

```text
app/src/main/java/com/transcendiverse/digitaltwin/launcher/LauncherTodaySummary.kt
app/src/main/java/com/transcendiverse/digitaltwin/launcher/LauncherDailySurface.kt
app/src/main/java/com/transcendiverse/digitaltwin/launcher/LauncherSafetyActions.kt
app/src/test/java/com/transcendiverse/digitaltwin/LauncherTodaySummaryTest.kt
```

Acceptance:

- Converts `CachedToday?` into a launcher summary:
  - user/mood
  - streak
  - check-in status
  - current quest
  - next action
  - cache age label
  - safe empty state
- Does not display secrets.
- Provides safe action labels for: open companion, open settings, refresh, app drawer.
- Tests cover cached and empty states.

### Task D — Docs, safety, and release hygiene

Owns operator-facing docs and final safety checklist.

Allowed primary files:

```text
README.md
docs/launcher-v0-mass-deployment.md
app/src/test/java/com/transcendiverse/digitaltwin/LauncherSafetyCopyTest.kt
.hermes/evaluations/launcher-v0-*/codex-final.txt
```

Acceptance:

- README explains how to enable/disable Digital Twin as Home app:
  - Settings → Apps → Default apps → Home app → Digital Twin
  - How to switch back to system launcher.
- Documents that this is prototype/safe launcher mode.
- Adds tests for safety copy helpers if helpers exist.
- No generated artifacts or secrets.

## Merge strategy

1. Commit spec on main.
2. Create four worktrees from origin/main:
   - `feat/launcher-home-shell`
   - `feat/launcher-app-drawer`
   - `feat/launcher-daily-surface`
   - `feat/launcher-safety-docs`
3. Run Codex concurrently in all four worktrees.
4. Each task must run its own gates before local commit.
5. Merge into main in order:
   - daily surface
   - app drawer
   - home shell
   - safety/docs
6. Resolve conflicts once in main.
7. Run integrated forced gates once.
8. Push main.
9. Confirm GitHub Actions success.
10. Install on phone.

## Verification commands

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export ANDROID_HOME="/c/Users/User/AppData/Local/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="/mingw64/bin:/usr/bin:/c/Users/User/bin:$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/emulator:/c/Users/User/.gradle/wrapper/dists/gradle-9.1.0-all/7wzd0jkjit61aq2p43wpjgij9/gradle-9.1.0/bin:/c/Users/User/AppData/Local/hermes/node/bin:/c/Program Files/GitHub CLI:/c/Program Files/nodejs:/c/Python314:/c/Python314/Scripts:/c/Windows/system32:/c/Windows:/c/Windows/System32/OpenSSH:$PATH"

git diff --check
./gradlew test --rerun-tasks
./gradlew lint --rerun-tasks
./gradlew assembleDebug --rerun-tasks
```

Phone install:

```bash
adb devices -l
adb -s <phone-serial> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <phone-serial> shell am start -W -n com.transcendiverse.digitaltwin/.MainActivity
adb -s <phone-serial> shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME com.transcendiverse.digitaltwin
```

## Safety notes

- A launcher can create a bad user experience if it lacks escape routes. Always keep Settings and companion app buttons visible.
- Never auto-select default Home app.
- Keep the prototype warning visible until app drawer and escape paths are verified on device.
- If HOME registration works but launcher screen is not useful enough, ship it as selectable but do not ask user to set it default yet.
