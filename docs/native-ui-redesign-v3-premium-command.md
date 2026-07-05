# Native UI Redesign v3 — Premium Command System

## User direction

The user supplied a useful visual-taste stack:

```text
Linear = dark precision, subtle borders, no clutter
Raycast = command utility, fast action language
xAI = technical restraint, monospace micro-labels
BMW = premium industrial geometry
Apple/Oura/WHOOP = glanceable daily state
```

Use this as taste input, not as a literal clone.

## Current state

There is already an uncommitted native UI/launcher worktree with a dark command-style companion:

- companion pages: `Today / Ask / Journal / Quest / Me`;
- current screenshots under `.hermes/evaluations/oasis-command-all-pages-v12/screens/`;
- main companion implementation in `app/src/main/java/com/transcendiverse/digitaltwin/ui/TodayScreen.kt`.

Do **not** reset, checkout, or discard the existing worktree. This v3 pass should refine the current design, not revert it to the older v2/v3 companion.

## Honest visual critique from v12 screenshots

The direction is much better than the old debug dashboard, but it still misses world-class premium restraint:

1. **Header is too loud**
   - `Command` is huge and consumes too much first-viewport energy.
   - The tab row is large, so chrome competes with content.

2. **Cards are still too chunky**
   - Borders are thick/visible on every card.
   - Large 16–20dp rounded cards repeat down the screen and still feel like heavy dashboard blocks.

3. **Typography lacks fine hierarchy**
   - Many labels are uppercase, but they are large and heavy rather than technical micro-labels.
   - Body text is oversized in places and creates vertical bloat.

4. **Accent is overused**
   - Purple appears in status pills, action pills, progress bars, selected states, and rating controls.
   - The result is more “purple dashboard” than restrained premium instrument.

5. **Prompt/action chips look like giant buttons**
   - Ask/Journal prompt chips are too tall and visually equal.
   - They should feel like command shortcuts, not feature cards.

6. **Daily-state components are close but not glanceable enough**
   - The three metric cells are useful.
   - They should be more compact and industrial: quieter labels, tighter bars, less card mass.

## V3 design target

Build a coherent **premium technical daily companion**:

```text
near-black command canvas
thin precise panels
monospace/uppercase micro-labels
one restrained violet/cyan accent
compact command shortcuts
Oura/WHOOP-like state glance
no dashboard clutter
```

It should feel like:

```text
an intelligent phone-native command layer for today
```

Not:

```text
a giant Material dashboard with purple buttons
```

## Practical token direction

Adopt these into Compose tokens where useful:

```text
background: #050607 / #07080A
surface: rgba(255,255,255,0.025) equivalent
surface elevated: rgba(255,255,255,0.045) equivalent
border: rgba(255,255,255,0.075) equivalent
text primary: #F4F1EA
text secondary: #B8B0A3
accent: one violet/cyan only, used sparingly
radius: 12–18dp, not giant bubbly 28dp everywhere
labels: small uppercase technical micro-labels
```

Compose cannot directly use CSS rgba strings; encode equivalent `Color(0x06FFFFFF)`, `Color(0x0BFFFFFF)`, etc. Keep accessibility/readability intact.

## Implementation scope

Allowed files:

```text
app/src/main/java/com/transcendiverse/digitaltwin/ui/TodayScreen.kt
app/src/test/java/com/transcendiverse/digitaltwin/CompanionUxContractTest.kt
app/src/test/java/com/transcendiverse/digitaltwin/CompanionUxV3ContractTest.kt
optional new test: app/src/test/java/com/transcendiverse/digitaltwin/PremiumCommandVisualContractTest.kt
docs/native-ui-redesign-v3-premium-command.md
```

Forbidden:

- Do not touch launcher files.
- Do not touch manifest/task-affinity/default-home behavior.
- Do not edit Gradle/build config.
- Do not add dependencies.
- Do not touch backend/data models except if compile requires an import cleanup inside allowed file.
- Do not display tokens, Bearer headers, password values, raw backend URLs, raw JSON, or raw hrefs in main UI.
- Do not introduce DCT claims, raw journal/chat ingestion claims, or fake AI claims.
- Do not commit or push.

## UX requirements

### 1. Make shell chrome compact

- Reduce header visual dominance.
- Keep title, date/user, and fixture/live status, but make them feel like an instrument header.
- Tabs should be compact command tabs, not big rounded cards.
- Preserve minimum touch target. Visual shape can be smaller via internal text/shape/border choices.

### 2. Make panels precise

- Panels should use very subtle surfaces and hairline borders.
- Avoid thick all-card outlines.
- Reduce repeated heavy card feel by making nested metric cells quieter.
- Radius should mostly sit in 12–18dp.

### 3. Use technical micro-labels

- Add/keep micro-label helper, but make it smaller/tighter.
- Use uppercase sparingly for section labels.
- Use muted color; do not make every label shout.

### 4. Make metrics glanceable

- The readiness/load/recovery metric cells should feel like Apple/Oura/WHOOP daily-state widgets:
  - clear number;
  - tiny label;
  - compact segmented bar;
  - low visual mass.
- Keep current deterministic scoring behavior.

### 5. Make actions command-like

- Prompt chips on Ask/Journal should become compact shortcut cells.
- Keep touch targets, but visually reduce bulk.
- Labels should be concise command verbs.
- Use one accent only for primary/selected/live states.

### 6. Improve check-in card polish

- Preserve v3 behavior: selected preset state, grouped rating controls, summary, local preview.
- Make preset pills and stepper controls match the premium token system.
- Avoid large filled purple blocks everywhere.

### 7. Keep product clarity

First viewport should still answer:

```text
What is my state?
What should I do now?
What is the smallest next step?
```

Do not bury check-in usefulness under pure aesthetic polish.

## Suggested component/token shape

This is guidance, not mandatory exact code:

```kotlin
private val CommandCanvas = Color(0xFF050607)
private val CommandPanel = Color(0x06000000) // choose readable equivalent carefully
private val CommandSurface = Color(0x08FFFFFF)
private val CommandElevated = Color(0x0CFFFFFF)
private val CommandHairline = Color(0x13FFFFFF)
private val CommandText = Color(0xFFF4F1EA)
private val CommandMuted = Color(0xFFB8B0A3)
private val CommandAccent = Color(0xFF9E7CFF) // or one cyan if visually better
```

Use existing Compose Material3 primitives. No new libraries.

Possible helpers:

```kotlin
CommandMicroLabel
CommandPanel
CommandShortcutChip
CommandMetricCell
CommandSegmentTab
```

## Tests / contracts

Add or update source-inspection tests to lock:

- premium token names / colors exist;
- shell uses compact command tabs;
- command micro-label helper exists;
- prompt chips are command shortcuts, not giant `PromptChip` blocks if renamed;
- privacy-sensitive strings stay out of main flow;
- selected check-in preset and rating controls still exist.

## Verification for this visual checkpoint

Run after implementation:

```bash
git diff --check
./gradlew --no-daemon --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process :app:testDebugUnitTest \
  --tests com.transcendiverse.digitaltwin.CompanionUxContractTest \
  --tests com.transcendiverse.digitaltwin.CompanionUxV3ContractTest \
  --tests com.transcendiverse.digitaltwin.PremiumCommandVisualContractTest \
  --tests com.transcendiverse.digitaltwin.QuickCheckInStateTest \
  --tests com.transcendiverse.digitaltwin.TodayLoginValidationTest \
  --tests com.transcendiverse.digitaltwin.TodayActionTest \
  --rerun-tasks
./gradlew --no-daemon --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process assembleDebug --rerun-tasks
```

For this visual stage, compile/runtime screenshots matter more than full lint. Run lint only if preparing to commit.

Runtime:

- Install debug APK on physical phone if available, otherwise emulator.
- Capture screenshots to:

```text
.hermes/evaluations/native-ui-premium-command-v3/screens/
```

Capture at least Today, Ask, Journal, and Me.

## Acceptance criteria

- The UI looks meaningfully more premium/technical than v12.
- Header/tabs no longer dominate.
- Panels are precise, darker, subtler, and less chunky.
- Accent is restrained and no longer paints every control.
- Ask/Journal shortcuts feel command-like.
- Daily metrics are easier to scan.
- Check-in remains usable.
- No launcher/default-home changes.
- No privacy/security regression.
- Build/runtime screenshot succeeds.

## Codex implementation note

The v3 polish pass adds explicit `Command*` visual tokens, compact command tabs, monospace micro-labels, command shortcut chips, quieter panels, and lower-mass metric cells while preserving selected check-in presets, grouped rating controls, local preview behavior, and signed-in save paths.
