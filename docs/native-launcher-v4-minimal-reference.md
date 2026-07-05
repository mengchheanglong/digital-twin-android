# Native Launcher v4 — Actual Minimalist Launcher

## User verdict

The launcher still sucks because it still behaves like a Digital Twin dashboard, not a phone launcher.

Correct diagnosis:

```text
cards/tabs/buttons = dashboard
app names/search/gestures = launcher
```

The next pass must stop polishing the dashboard and replace it with an actual minimalist launcher structure.

## Web research references

Sources browsed/extracted:

- Niagara Launcher Google Play: `https://play.google.com/store/apps/details?id=bitpit.launcher&hl=en_US`
- Olauncher Google Play: `https://play.google.com/store/apps/details?id=app.olauncher&hl=en_US`
- Before Launcher Google Play: `https://play.google.com/store/apps/details?id=com.beforesoft.launcher&hl=en_US`
- MakeUseOf minimalist launchers list: `https://www.makeuseof.com/best-minimalist-launchers-android`
- How-To Geek Olauncher review: `https://www.howtogeek.com/this-android-launcher-helped-me-cut-my-phone-use-in-half`

Useful findings:

### Olauncher

- Single home page.
- No app icons/grid/widgets by default.
- Column of 0–8 app names on home.
- Everything else hidden in app drawer/search.
- Swipe gestures for quick launch.
- Hidden settings through long press.
- Goal: reduce screen time by making app launch intentional.

### Niagara

- App-first launcher, not dashboard-first.
- Favorites are a vertical list.
- Fast one-handed alphabetical access.
- Home is quiet and app access is immediate.
- Search/app access matters more than cards.

### Before Launcher

- Minimal home with favorite app names.
- All apps are on a separate searchable/sortable list.
- Notifications/secondary surfaces are off to the side.
- Settings are present but not loud.

## v4 design rule

This screen must look like a launcher if a stranger sees it.

That means:

```text
clock/date
small status line
favorite app text rows
subtle app drawer/search affordance
tiny companion/settings/refresh escape actions
```

It must NOT show:

```text
Today / Apps / Twin tabs
large cards
giant pills
dashboard metrics blocks
button grids
product-marketing copy
```

## Target home layout

Approximate shape:

```text
6:31
Wednesday, Jul 1

Excellent · 3-day streak
Next: Reflect

Telegram
Messages
Chrome
Camera
Spotify
Digital Twin

Apps                         Settings
```

If there are allowed/favorite apps:

- Show up to 7 as plain text rows.
- App name row launches app.
- No icon row on the home screen.
- Keep text large enough but not huge.

If no allowed apps:

```text
No allowed apps yet
Open Apps to choose what belongs on Home.
```

But this must be small and quiet, not a huge card.

## App drawer target

Keep/improve current drawer:

```text
Apps                         Close
Search apps

App name        Allow/Hide
App name        Allow/Hide
```

Rules:

- searchable text list;
- no card per app if possible, just rows/dividers;
- enough 48dp touch target;
- Allow/Hide action stays;
- Close/Settings/Companion escape controls remain available.

## Twin/companion placement

Digital Twin should not consume the whole launcher.

Use it as:

- one favorite-like row: `Digital Twin` or `Ask Twin`;
- a subtle status line: `Excellent · 3-day streak`;
- maybe next-action text: `Next: Reflect`.

The full companion is one tap away, not the entire home.

## Implementation scope

Allowed files:

```text
app/src/main/java/com/transcendiverse/digitaltwin/launcher/LauncherScreen.kt
app/src/main/java/com/transcendiverse/digitaltwin/launcher/LauncherAppDrawer.kt
```

No manifest/task-affinity changes.
No default-Home automation.
No companion app redesign in this pass.
No tests/lint until user likes visual direction or commit is imminent.

## Acceptance for screenshot pass

Physical phone first viewport should show:

- no `Today | Apps | Twin` tabs;
- no large rounded cards above the fold;
- a clock/date area;
- app-name rows as the main content;
- small bottom/top controls for Apps/Settings/Refresh/Companion;
- looks like Olauncher/Before/Niagara class, not Material dashboard.
