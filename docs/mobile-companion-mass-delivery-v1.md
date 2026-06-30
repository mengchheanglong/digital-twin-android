# Mobile Companion Mass Delivery v1

## Objective

Turn the native Android Digital Twin companion from a display/check-in prototype into a daily-use companion that can be shipped in larger vertical batches instead of slow one-feature loops.

This milestone targets the current Android repo:

- Repo: `C:/Users/User/projects/digital-twin-android`
- Package: `com.transcendiverse.digitaltwin`
- Backend base URL: `https://digital-twin-orcin-omega.vercel.app`
- Backend repo: `C:/Users/User/archive/retired/digital-twin`

Success means a user can install the debug app, log in with minimal setup friction, check in, see/update their active quest, keep a useful widget, and developers get automated Android verification on every push.

## Operating mode

Stop tiny-slice loops. Use isolated worktrees and parallel Codex tasks for independent workstreams:

1. **Quest actions** — native active quest progress/complete controls.
2. **Onboarding/settings hardening** — default production URL, clearer login state, safer reset/diagnostics.
3. **Android CI/release hygiene** — GitHub Actions build/test/lint, version docs, faster future verification.

Hermes merges/inspects after agents finish, runs one forced verification pass, then installs once on the phone.

## Backend contracts

### Today

```http
GET /api/mobile/today
Authorization: Bearer <token>
Accept: application/json
```

Success envelope:

```json
{ "success": true, "today": { } }
```

### Login

```http
POST /api/auth/login
Content-Type: application/json
Accept: application/json
```

Request:

```json
{ "email": "user@example.com", "password": "..." }
```

Success:

```json
{ "token": "jwt", "user": { "id": "...", "email": "...", "name": "..." } }
```

### Daily check-in

```http
POST /api/checkin/submit
Authorization: Bearer <token>
Content-Type: application/json
Accept: application/json
```

Request:

```json
{ "ratings": [3, 3, 3, 3, 3] }
```

Dimension order: energy, focus, stressControl, socialConnection, optimism.

### Quest list

```http
GET /api/quest/all?limit=20&skip=0
Authorization: Bearer <token>
Accept: application/json
```

Returns an array of quests with `_id`, `goal`, `duration`, `progress`, `completed`, dates.

### Quest progress

```http
PUT /api/quest/progress/{id}
Authorization: Bearer <token>
Content-Type: application/json
Accept: application/json
```

Request:

```json
{ "progress": 75 }
```

Rules: `progress` must be a number from 0 to 100; backend rounds it.

Success:

```json
{ "msg": "Progress updated.", "quest": { }, "progression": null }
```

### Quest complete/reopen toggle

```http
PUT /api/quest/complete/{id}
Authorization: Bearer <token>
Accept: application/json
```

Success:

```json
{ "msg": "Quest completed.", "quest": { }, "progression": { } }
```

May return `quest: null` with `deleted: true` for archived recurring quest completion.

## Commands

Use the Windows Git Bash Android environment:

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export ANDROID_HOME="/c/Users/User/AppData/Local/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="/mingw64/bin:/usr/bin:/c/Users/User/bin:$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/emulator:/c/Users/User/.gradle/wrapper/dists/gradle-9.1.0-all/7wzd0jkjit61aq2p43wpjgij9/gradle-9.1.0/bin:/c/Program Files/GitHub CLI:/c/Program Files/nodejs:/c/Python314:/c/Python314/Scripts:/c/Windows/system32:/c/Windows:/c/Windows/System32/OpenSSH:$PATH"
```

Verification gates:

```bash
git diff --check
./gradlew test --rerun-tasks
./gradlew lint --rerun-tasks
./gradlew assembleDebug --rerun-tasks
```

Phone install gate:

```bash
adb devices -l
adb -s adb-1556394943000ZI-doQV7K._adb-tls-connect._tcp install -r app/build/outputs/apk/debug/app-debug.apk
adb -s adb-1556394943000ZI-doQV7K._adb-tls-connect._tcp shell am start -W -n com.transcendiverse.digitaltwin/.MainActivity
```

## Project structure

Android production files:

- `app/src/main/java/com/transcendiverse/digitaltwin/data/*Repository.kt`
- `app/src/main/java/com/transcendiverse/digitaltwin/model/*.kt`
- `app/src/main/java/com/transcendiverse/digitaltwin/ui/TodayScreen.kt`
- `app/src/main/java/com/transcendiverse/digitaltwin/sync/*`
- `app/src/main/java/com/transcendiverse/digitaltwin/widget/*`
- `app/src/main/res/**`

Tests:

- `app/src/test/java/com/transcendiverse/digitaltwin/*Test.kt`

Docs/CI:

- `docs/**`
- `.github/workflows/android.yml`
- `README.md`

## Code style

Use small typed repositories with transport injection for JVM tests:

```kotlin
data class TodayHttpRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String>,
    val body: String? = null,
)

interface TodayHttpTransport {
    fun execute(request: TodayHttpRequest): TodayHttpResponse
}
```

Rules:

- `@Serializable` DTOs for request/response models.
- Repository validates local inputs before network calls.
- Tests assert method/path/headers/body and non-2xx behavior.
- UI calls repository factories injected into `TodayScreen`.
- Secrets never appear in UI status, exceptions, logs, docs, or tests except as fake strings explicitly asserted not to leak.

## Testing strategy

Minimum for each task:

- TDD RED evidence in `.hermes/evaluations/<task>/codex-final.txt`.
- JVM unit tests for every new repository/helper.
- Existing tests must pass.
- `git diff --check`, `test`, `lint`, `assembleDebug` all pass before merge.

## Boundaries

Always do:

- Batch related work.
- Use worktrees for parallel branches.
- Keep Android app phone-first.
- Use actual backend contracts, not invented shapes.
- Keep tokens/passwords out of reports and UI.

Ask first:

- Replacing current auth system.
- Adding notifications.
- Making the app the default launcher/home app.
- Storing raw journal/chat/reflection data beyond current compact Today cache.

Never do:

- Commit real credentials, API keys, JWTs, passwords, MongoDB URIs, or `.env` values.
- Make widget submit check-ins/quest changes without opening app confirmation.
- Use the unrelated `digital-twin.vercel.app` hostname; it redirects to a different site.
- Block progress on emulator-only validation when the real wireless phone is connected.

## Task breakdown

### Task A — Quest actions

Add native quest progress/complete controls:

- `NetworkQuestRepository` for `GET /api/quest/all`, `PUT /api/quest/progress/{id}`, `PUT /api/quest/complete/{id}`.
- Tests for request shape, validation, success parsing, error privacy.
- Today UI shows active quest controls: `+10%`, `-10%`, `Complete`/`Reopen` if a quest id can be resolved.
- After action: refresh Today, cache, update widget, enqueue sync.

### Task B — Onboarding/settings hardening

Reduce setup friction:

- Default backend base URL to `https://digital-twin-orcin-omega.vercel.app` for fresh installs.
- Preserve user-edited URL if set.
- Add a clear `Use production` button and `Reset local app data`/safe clear controls if simple.
- Make login/status copy short and obvious.
- Tests for default settings and no secret leakage.

### Task C — CI/release hygiene

Make future batches faster:

- Add `.github/workflows/android.yml` running Gradle test/lint/assembleDebug on push/PR.
- Cache Gradle dependencies safely.
- Document local build/install commands and backend URL in README/docs.
- CI must use JDK 21 and must not include secrets, signing keys, release keystores, Play deployment, or API tokens.
- Do not introduce secrets or deployment keys.

## Merge strategy

1. Spawn tasks A/B/C in separate worktrees from current `origin/main`.
2. Let Codex commit inside each worktree.
3. Merge/cherry-pick into main in order: C → B → A.
4. Resolve any conflicts once.
5. Run one forced verification pass.
6. Push once.
7. Install/launch once on phone.
