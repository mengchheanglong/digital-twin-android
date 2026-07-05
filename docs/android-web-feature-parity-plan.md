# Android ↔ Web Feature Parity Plan

Status: **planning / product direction**  
Implementation: **not started in this doc**  
Android repo: `C:/Users/User/projects/digital-twin-android`  
Active web repo: `C:/Users/User/archive/retired/digital-twin`  
Last grounded inspection: web routes/API files + current Android native repositories/surfaces.

---

## 0. Why this document exists

The Android app currently does **not** contain every feature from the web app. That is intentional in the current direction, but it was not explicit enough.

This doc answers:

1. What does the web app own?
2. What does Android own?
3. Which web features should become native Android features?
4. Which should be compact summaries?
5. Which should stay web/deep-link only for now?
6. What is the implementation order?

---

## 1. Product model

Use this model unless the user explicitly changes direction:

```text
Web app  = full Digital Twin brain / source-of-truth cockpit
Android = native phone nervous system / daily action layer
```

The Android app should not become a crowded clone of the web dashboard. It should make the **daily loop** feel native on the phone:

```text
unlock phone → see state → check in / ask / capture / progress quest → close
```

The web app remains the deeper workspace:

```text
analytics, history, timeline, reports, data export, full management, long-form review
```

---

## 2. Current web feature inventory

Grounded in current web routes/navigation:

### 2.1 Top-level web dashboard routes

```text
/dashboard/insight    Daily Log / Today
/dashboard/quest      Quest Board
/dashboard/chat       Companion Chat
/dashboard/journal    Journal
/dashboard/focus      Focus
/dashboard/analytics  Analytics
/dashboard/timeline   Timeline
/dashboard/history    History
/dashboard/profile    Me / Profile
/dashboard/checkin    Check-in flow
```

Desktop sidebar exposes:

```text
Daily Log, Quest Board, Companion, Journal, Focus, Analytics, Timeline, History
```

Mobile web bottom nav exposes only:

```text
Today, Quest, Chat, Journal, Me
```

The web `Me` mobile shortcuts expose:

```text
Focus, Analytics, Timeline, History, Check-in
```

### 2.2 Relevant web API surface

Current web app has API routes for:

```text
Auth
- /api/auth/login
- /api/auth/register
- /api/auth/forgot-password
- /api/auth/reset-password

Mobile compact state
- /api/mobile/today

Check-in
- /api/checkin/submit
- /api/checkin/history
- /api/checkin/questions
- /api/checkin/micro
- /api/checkin/parse

Quest
- /api/quest/all
- /api/quest/create
- /api/quest/suggest
- /api/quest/progress/[id]
- /api/quest/complete/[id]
- /api/quest/delete/[id]
- /api/quest/decompose
- /api/quest/reset
- /api/quest/log

Chat / Twin
- /api/chat/send
- /api/chat/history
- /api/twin/context-pack
- /api/memory

Journal
- /api/journal
- /api/journal/[id]

Focus
- /api/focus
- /api/focus/[id]

Analytics / reports
- /api/analytics/streaks
- /api/analytics/burnout
- /api/analytics/burnout-history
- /api/analytics/correlation
- /api/analytics/daily-rhythm
- /api/analytics/forecast
- /api/analytics/life-event-impact
- /api/analytics/mood-patterns
- /api/analytics/synergy
- /api/reports/weekly
- /api/reports/weekly-plan

Timeline / events / profile / data
- /api/timeline/checkin
- /api/timeline/insights
- /api/events
- /api/life-events
- /api/life-events/[id]
- /api/profile
- /api/user
- /api/user/dimensions
- /api/insight/state
- /api/export
```

---

## 3. Current Android native state

Grounded in current Android code:

### 3.1 Native surfaces

```text
Launcher
- quiet/Oasis-style home
- app list / drawer
- left Twin system surface
- inline quick actions for Check-in, Quest, Journal, Ask

Widget
- Today glance
- mood/quest/check-in/cache status
- tap-to-refresh/open behavior

Companion app
- Today
- Ask
- Journal
- Quest
- Me
```

### 3.2 Android-connected backend paths

Android currently has typed native repositories for:

```text
Auth/login
- POST /api/auth/login

Today state
- GET /api/mobile/today

Check-in
- POST /api/checkin/submit

Quest
- GET /api/quest/all
- PUT /api/quest/progress/[id]
- PUT /api/quest/complete/[id]

Ask/chat
- POST /api/chat/send

Journal
- POST /api/journal
```

### 3.3 Android intentionally missing today

Android does not yet provide native UI for:

```text
Focus sessions
Analytics dashboards
Timeline browser
History archive
Reports / weekly plan
Export / backup
Journal history list/edit/delete
Full quest creation/suggestion/decomposition/delete/reset/log
Chat history browsing beyond current local session
Life events / memories / user dimensions management
Advanced profile settings beyond connection/account basics
```

---

## 4. Parity policy

Every web feature should be classified into one of three buckets.

### Bucket A — Native Android core

Use when the feature is:

```text
frequent, phone-native, notification/widget/launcher-relevant, quick action, offline/cache useful
```

These should be real Android features.

### Bucket B — Android summary / compact view

Use when the feature is valuable on phone but too deep/heavy for full native parity.

Android should show:

```text
one-screen summary, one next action, link to full web if needed
```

### Bucket C — Web/deep-link first

Use when the feature is:

```text
rare, admin-like, data-heavy, chart-heavy, export-heavy, or better on a bigger web surface
```

Android should expose a clean link/entry point, not clone the whole feature yet.

---

## 5. Feature map

| Web feature | Current Android state | Target Android treatment | Priority | Notes |
|---|---:|---|---:|---|
| Login/auth | Native login in Me | Native core | P0 | Keep token private. Never pass JWT in URLs. |
| Today / Daily Log | Native Today + widget + launcher summary | Native core | P0 | Android should be best daily entry point. |
| Check-in | Native submit + launcher inline | Native core | P0 | Must support done-today state cleanly. |
| Ask / Chat send | Native Ask + launcher inline | Native core | P0 | Android sends messages, but does not yet browse full history. |
| Journal create | Native quick capture + launcher inline | Native core | P0 | Recently fixed to POST `/api/journal`. |
| Current Quest progress | Native Quest progress + launcher inline | Native core | P0 | Android updates progress/complete. |
| Widget | Native only | Native core | P0 | Web cannot provide this. |
| Launcher | Native only | Native core | P0 | Quiet phone shell, not web clone. |
| Focus | Missing native | Native core, small | P1 | Phone-native: start/end/current session. Do not port analytics first. |
| Quest create/suggest | Missing native | Native core, compact | P1 | Add simple `Plan quest` / `Suggest quest`, not full board management. |
| Journal history | Missing native | Summary view | P1 | Recent entries only; full edit/delete can deep-link first. |
| Chat history | Mostly local/session only | Summary/thread view | P1/P2 | Need `/api/chat/history` integration if persistent thread is required. |
| Profile / Me stats | Partial native | Summary view | P1 | Mastery/streak/state + web shortcuts. |
| Analytics | Missing native | Summary + web deep-link | P2 | Weekly/state glance only; no dense charts initially. |
| Timeline | Missing native | Summary + web deep-link | P2 | Recent activity strip, not full browser. |
| History | Missing native | Summary/search entry + web deep-link | P2 | Full archive remains web first. |
| Weekly reports / weekly plan | Missing native | Summary card + web deep-link | P2/P3 | Useful after daily loop is stable. |
| Life events | Missing native | Web/deep-link first | P3 | Creation/editing can remain web until need is proven. |
| Memory / context pack | Missing native UI | Internal/backend only | P3 | Do not expose raw memory plumbing in main UI. |
| User dimensions | Missing native UI | Summary only | P3 | Maybe in Me; editing stays web first. |
| Export / backup | Missing native | Web/deep-link first | P3 | Do not build native export until web flow is solid. |
| Advanced settings/theme | Minimal native settings | Web/deep-link first + native essentials | P3 | Android should keep only account/backend/theme essentials. |

---

## 6. Immediate product gap

The user expectation gap is real:

```text
The web has many features.
The Android app currently shows only the daily/native subset.
Android does not yet clearly explain or route to missing web features.
```

The fix is **not** to add 10 Android tabs. The fix is:

1. Make native daily loop strong.
2. Add a web-feature bridge/hub in Me.
3. Port only high-frequency phone-native flows.
4. Keep heavy dashboard features on web until daily usage proves they need native treatment.

---

## 7. Recommended implementation sequence

### Phase 0 — Freeze understanding before more code

Status: this document.

Acceptance:

- user understands Android is not a full web clone;
- user accepts or edits the bucket policy;
- no code is implemented from this doc until direction is confirmed.

### Phase 1 — Android `More from web` bridge in Me

Goal: make missing web features visible immediately without bloating Android.

Add a compact section to Android `Me`:

```text
MORE FROM WEB
- Focus
- Analytics
- Timeline
- History
- Export
- Full dashboard
```

Behavior:

- Open deployed web route in browser/custom tab.
- Do **not** pass JWT/token in the URL.
- If web session is not signed in, user signs in through web.
- Use generic route labels; do not show raw backend URLs in the main UI.

Likely files:

```text
app/src/main/java/com/transcendiverse/digitaltwin/ui/TodayScreen.kt
```

Possible helper if needed:

```text
app/src/main/java/com/transcendiverse/digitaltwin/WebRoutes.kt
```

Verification:

- source contract: no token/url rendered in main UI;
- runtime tap opens browser/custom tab;
- no launcher/default-home behavior touched.

### Phase 2 — Native Focus quick loop

Goal: first missing web feature that truly belongs on Android.

Android should support:

```text
current focus state
start focus
end focus
small timer/session status
```

Do not port all focus analytics initially.

Backend/API to inspect:

```text
/api/focus
/api/focus/[id]
```

Possible Android treatment:

- Today card: `Start focus` secondary action;
- Me or Today summary: current focus session;
- launcher left Twin surface: one compact focus action.

### Phase 3 — Recent activity summary

Goal: cover Timeline/History expectation without full native archive.

Android should show:

```text
RECENT SIGNALS
- latest check-in
- latest journal entry title
- latest quest event
- latest reflection/chat marker
```

API choice:

- Prefer a new compact backend endpoint such as `GET /api/mobile/recent` or extend `/api/mobile/today` carefully.
- Avoid making Android call many analytics/timeline endpoints on first load.

### Phase 4 — Journal history lite + Quest creation lite

Goal: close the most obvious daily content gaps.

Journal:

```text
recent entries list
open full journal on web
maybe edit/delete later
```

Quest:

```text
create/suggest one quest
progress/complete already exists
full board remains web
```

APIs to use/inspect:

```text
GET /api/journal
POST /api/quest/create
GET or POST /api/quest/suggest
POST /api/quest/decompose only if needed
```

### Phase 5 — Weekly state / analytics summary

Goal: make Android feel like it has intelligence without becoming chart-heavy.

Android should show Oura/WHOOP-style summary cards:

```text
weekly mood trend
quest consistency
energy/focus/stress pattern
burnout risk
one recommendation
```

Backend/API options:

```text
/api/reports/weekly
/api/reports/weekly-plan
/api/analytics/streaks
/api/analytics/burnout
/api/analytics/forecast
```

Prefer one mobile summary endpoint if web APIs are too broad.

### Phase 6 — Notifications / reminders

Goal: use Android-native value, not web parity.

Add only after daily loop is reliable:

```text
check-in reminder
quest nudge
reflection prompt
focus timer notification
```

Rules:

- opt-in;
- no shame copy;
- no raw private text in notification body unless user approves;
- no fake AI claims.

---

## 8. Deep-link/security rules

Do not solve web parity by leaking auth.

Never:

```text
append JWT to web URL
show Bearer token
show raw backend URL in main UI
render raw JSON/errors
send raw journal/chat/check-in records to Hermes by default
```

Allowed first bridge:

```text
open normal web route in browser/custom tab
```

Future secure bridge options, only if needed:

```text
same account signs into web separately
short-lived server-issued one-time handoff code
Android App Links with backend exchange
```

Do not implement a token handoff without a separate security design.

---

## 9. What Android should not become

Do not make Android:

```text
Dashboard with 10 tabs
full analytics chart suite
full timeline archive first
admin/settings clone
web app inside a native shell
```

The Android app should stay:

```text
phone-native
quick
quiet
stateful
cache/offline-aware
launcher/widget/notification-integrated
```

---

## 10. Decision needed before implementation

Recommended direction:

```text
Hybrid C:
Android = native daily layer + web bridge
Web = full dashboard/source of truth
```

If accepted, next coding slice should be:

```text
Phase 1 — Android Me web-feature bridge
```

Why this first:

- It immediately addresses “I do not see web features.”
- It does not bloat the native top-level tabs.
- It avoids risky backend work.
- It creates a clear place for Focus/Analytics/Timeline/History/Export while native parity matures.

---

## 11. Proposed Phase 1 handoff shape

Codex task:

```text
Implement Android Me `More from web` bridge.
Do not touch launcher/default-home/backend/data repositories unless explicitly required.
Do not pass tokens in URLs.
Do not render raw backend URLs in main companion UI.
Preserve dirty worktree.
```

Allowed likely files:

```text
app/src/main/java/com/transcendiverse/digitaltwin/ui/TodayScreen.kt
app/src/test/java/com/transcendiverse/digitaltwin/CompanionUxV3ContractTest.kt
app/src/test/java/com/transcendiverse/digitaltwin/PremiumCommandVisualContractTest.kt
```

Optional new helper if implementation needs route constants:

```text
app/src/main/java/com/transcendiverse/digitaltwin/WebRoutes.kt
```

Acceptance:

- Me shows a compact `MORE FROM WEB` / `FULL DASHBOARD` style section.
- Includes Focus, Analytics, Timeline, History, Export/full dashboard.
- Tapping opens web route via safe Android intent/custom tab.
- UI does not show raw URL/token.
- Source/contract tests cover privacy and route labels.
- Runtime smoke opens at least one route.

---

## 12. Final answer to the user question

The Android app is different from the web app because it is currently designed as the **native daily action layer**, not the full dashboard clone.

Missing web features are real. The plan is to:

1. expose them through a clean Android web bridge first;
2. port Focus and daily high-frequency actions natively;
3. add compact summaries for analytics/timeline/history;
4. keep deep dashboard/export/admin workflows on the web until native demand is proven.
