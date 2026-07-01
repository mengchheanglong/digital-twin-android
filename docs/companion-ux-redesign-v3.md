# Companion UX Redesign v3

## Goal

Refine the v2 native companion check-in so it feels tactile, compact, and intentionally phone-native.

v2 solved the biggest product problem: the check-in flow is no longer dead, and fixture mode is meaningful. But the visible check-in controls still feel like a rough prototype:

- the `Low / Okay / Strong` preset row has no obvious selected state;
- rating rows are visually sparse and too dashboard/form-like;
- `-` / `+` controls float far away from the rating value;
- there is no concise summary of the current check-in state;
- the card consumes a lot of vertical space without feeling polished.

This task is **not** a launcher task and **not** backend work.

## Product direction

The companion should keep the v2 hierarchy:

```text
State → Next step → Daily check-in → Quest → Reflection → Connection
```

But the check-in section should feel more like a daily phone interaction:

```text
Pick a preset → glance at five ratings → nudge only what changed → save/sign in later
```

The target feeling is calm and practical, not gamified pressure.

## V3 UX requirements

### 1. Selected preset must be visually obvious

When the default `Okay` preset is active, `Okay` should visibly look selected.

When the user taps `Low` or `Strong`, the selected pill should update.

Acceptable implementation:

- track `selectedPreset` separately from `ratings`, or infer it when exact preset ratings match;
- selected preset uses a filled/tonal background and stronger label;
- unselected presets stay quiet outlined pills;
- no color-only state: use fill/border/weight together.

### 2. Rating controls should be tactile and compact

The rating row should feel like a thumb-friendly phone control, not three loose text elements.

Acceptable implementation:

```text
Energy        [-]   3   [+]
```

But with:

- grouped control surface;
- 44–48dp tap areas for `-` and `+`;
- rating value in a clear center pill/label;
- disabled state at min/max remains readable;
- rows use consistent spacing and alignment;
- no tiny floating plus/minus controls.

Do not add sliders unless they are clearly simpler and still discrete 1–5 ratings.

### 3. Add a small check-in summary

The card should show a concise summary so the user understands their current choice.

Examples:

```text
Current shape: steady baseline · all 3/5
Current shape: strong day · mostly 4/5
Current shape: low energy · mostly 2/5
```

This does not need real AI. It should be deterministic from the current ratings/preset.

Keep the copy humble. Do not claim analysis beyond the selected values.

### 4. Preserve v2 behavior

Must remain true:

- check-in appears immediately after `TodayNextStepCard`;
- fixture mode remains a local preview;
- signed-in mode still saves via existing submit path;
- connection settings remain collapsed;
- no auth tokens/raw backend URLs/passwords/raw hrefs in the main flow;
- no launcher files changed;
- no Gradle/build config changes;
- no new dependencies.

### 5. Keep scope small

This task should only polish the check-in card and its regression tests.

Do not redesign hero, quest, reflection, launcher, app drawer, backend, settings, or login flow unless a tiny copy/test adjustment is required for integration.

## Suggested component shape

This is guidance, not a strict API:

```kotlin
private fun QuickCheckInCard(...)
private fun CheckInPresetRow(...)
private fun PresetPill(...)
private fun RatingControlRow(...)
private fun RatingStepperButton(...)
private fun checkInShapeSummary(ratings: List<Int>, selectedPreset: CheckInPreset?): String
```

Keep helpers simple and testable where possible.

## Test expectations

Update or add static/unit tests so regressions are caught:

- selected preset state exists and defaults to `Okay`;
- preset taps update both selected state and ratings;
- rating changes clear/customize selected preset state if ratings no longer match exact preset values, or re-infer selected preset when ratings match;
- rating control rows use real enabled `onDecrease` / `onIncrease` handlers, not no-op buttons;
- v2 privacy/hierarchy tests still pass;
- no launcher files changed.

Because these UI tests are source-inspection style today, it is acceptable to assert for component/helper names and copy strings. Prefer also adding pure Kotlin tests for deterministic helpers like `checkInShapeSummary`, `checkInPresetForRatings`, or rating updates when practical.

## Acceptance criteria

- First viewport still clearly shows state + next step + start of check-in.
- Check-in card visually communicates selected preset.
- Rating controls look/feel like grouped phone controls.
- Fixture mode still clearly says nothing is sent/saved until sign-in.
- Signed-in save path is preserved.
- Privacy/security constraints remain intact.
- Tests/lint/build pass.
- Runtime launch succeeds on phone/emulator.
- Screenshot confirms the check-in section looks more polished than v2.
