# Launcher UX v1 Prototype Spec

## Objective

Improve the Digital Twin launcher prototype before asking the user to make it the real/default Android launcher.

This is **not** default-home activation work. The launcher remains a selectable/prototype HOME activity that we test by launching `LauncherActivity` directly.

## Success Criteria

- Home surface feels useful before default-home testing.
- App drawer supports text search.
- User can favorite/unfavorite installed apps.
- Favorite apps appear on the launcher home surface for fast launch.
- Safety controls remain visible: open Android Settings and open Digital Twin companion.
- No secrets, tokens, URLs, raw journal/chat, or auth payloads are surfaced in launcher UI.
- No nested scroll/infinite-height Compose crash paths.

## Scope

### Include

- `LauncherFavoritesStore`: local SharedPreferences-backed favorite package names.
- App drawer search by app label or package name.
- Favorite toggle in drawer rows.
- Favorite apps section in daily launcher mode.
- Static/unit checks for wiring and privacy-safe launcher behavior.
- Build/install/runtime verification by direct `LauncherActivity` launch.

### Exclude

- Do not set Digital Twin as the default Home app.
- Do not automate default-launcher selection.
- Do not add launcher icon packs, widgets, or drag/drop yet.
- Do not add network calls from launcher UI beyond existing refresh scheduling.

## Commands

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export ANDROID_HOME="/c/Users/User/AppData/Local/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
git diff --check
./gradlew --no-daemon --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process :app:testDebugUnitTest --tests com.transcendiverse.digitaltwin.LauncherIntegrationContractTest --rerun-tasks
./gradlew --no-daemon --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process lint --rerun-tasks
./gradlew --no-daemon --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process assembleDebug --rerun-tasks
adb -s <phone> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <phone> shell am start -W -n com.transcendiverse.digitaltwin/.launcher.LauncherActivity
adb -s <phone> shell dumpsys window | grep -E 'mCurrentFocus|mFocusedApp'
adb -s <phone> logcat -d | grep -iE 'com.transcendiverse.digitaltwin|FATAL EXCEPTION|AndroidRuntime'
```

## Implementation Plan

1. Add persistent favorite package-name store.
2. Thread favorites through `LauncherActivity` state.
3. Add home favorite row.
4. Add drawer search and favorite toggle.
5. Update static contract tests.
6. Verify/build/install/runtime-check.

## Safety

- Always keep Settings and Companion escape controls visible.
- Launcher mode text must clearly say prototype/not default.
- Favorite store only saves package names, never app content or user data.
