# Companion UX Redesign v2

## Goal

Make the native Android Digital Twin companion feel genuinely useful, not just prettier.

The launcher/app drawer direction is already acceptable: minimalist, text-first, distraction-reducing, Oasis/Olauncher-like. This v2 task is only about the in-app **Digital Twin daily companion** experience.

## Current v1 state

v1 improved the app by replacing the old debug-dashboard opening with:

- a daily hero;
- warm minimal Notion/Apple-like visual tokens;
- one dark `Now` card;
- quest and reflection cards;
- collapsed `Connection settings`;
- no raw quest href in the main flow;
- a static UX regression test.

Current visible hierarchy from the v1 screenshot:

```text
Digital Twin
Tuesday, Jun 30
Alex is focused
Level 7 - 340 / 500 XP - 5-day streak
Energy / Focus / Stress chips
Now / Check in primary card
Quest card
Reflection card
Open check-in / View profile actions
Connection card
```

## Honest critique

v1 is cleaner, but still not enough as a daily companion.

Problems:

1. **The primary action is passive.**
   - `Now / Check in` reads like an action, but the visible card itself is not clearly actionable.
   - The separate `Open check-in` button appears later and currently behaves like a no-op.

2. **Fixture mode feels less useful than it should.**
   - In the screenshot, the primary action says to sign in from Connection settings to save check-in.
   - But a user trying the app should still be able to understand/practice the check-in flow without signing in.

3. **Too many equal cards remain.**
   - The page is prettier than a debug dashboard, but Quest and Reflection still visually compete with the primary action.
   - The first viewport should more directly answer:
     - What is my state?
     - What should I do now?
     - What is the smallest next step?

4. **Quest repeats the check-in instruction.**
   - The primary card says `Check in`.
   - The quest card also shows a green `Check in` next action.
   - This creates duplicate CTAs instead of a calm path.

5. **Reflection still has analytics-dashboard residue.**
   - `Trend: rising - Focus: mobile companion` is useful as data but feels less human.
   - It should be rephrased or visually de-emphasized as context, not dashboard output.

## Product direction

The companion should feel like a calm phone-native daily guide:

```text
State → Next small action → Do it → then continue quest/reflect
```

Design language:

- warm minimalism;
- human copy;
- one obvious next step;
- low visual noise;
- no raw backend/debug/security details in the daily flow;
- no fake AI claims;
- no DCT claims;
- no raw journal/chat ingestion claims.

## V2 behavior requirements

### 1. Make check-in real/usable

The user should not see a dead `Open check-in` action.

Acceptable implementation:

- remove the old no-op action row entirely; and
- render a real check-in section in the daily flow; and
- make the primary `Check in` action reveal/focus/emphasize that section, or place the section immediately after the primary action so the action feels self-evident.

For fixture mode:

- still show a meaningful check-in preview;
- let the user select presets/adjust ratings locally;
- clearly say it is a preview and sign-in is needed to save;
- do not pretend data was saved.

For signed-in mode:

- keep existing network submission behavior;
- keep validation;
- keep background sync enqueue after submit.

### 2. Improve first-screen hierarchy

The top of the screen should answer:

```text
State: Alex is focused
Now: check in for 30 seconds
Smallest next step: choose a preset, then save
```

Use concise copy. Prefer:

- `Start with a 30-second check-in.`
- `Pick a preset, adjust if needed, then save.`
- `After that, continue one quest.`

Avoid:

- dashboard metrics overload;
- grandiose AI promises;
- admin/security copy in the main flow.

### 3. One primary action

Keep only one primary next action near the top.

If using a button, it should do something real:

- expand/check-in section;
- scroll/focus to check-in section;
- or make the immediately following section the check-in section and remove the redundant button.

The old `LauncherActions` row with `Open check-in` / `View profile` should be removed or replaced with real, non-dead behavior.

### 4. Calm companion personality

Copy should feel like a quiet digital twin companion:

- direct;
- useful;
- humble;
- specific;
- short.

Examples:

```text
Good: Start with a 30-second check-in.
Good: Pick the closest preset. Adjust only if needed.
Good: After check-in, continue one small quest step.
Bad: Your AI twin has analyzed your full life context.
Bad: Digital consciousness transfer state synchronized.
Bad: Journal/chat stream uploaded to Hermes.
```

### 5. Preserve privacy and safety

Must remain true:

- no auth tokens displayed;
- no raw backend URLs in main flow;
- password masked;
- backend/login controls behind collapsed `Connection settings`;
- no raw nextAction href displayed;
- no raw journal/chat transmission claims;
- no launcher/default-home automation in this task.

## Suggested UI structure

A simple KISS structure is enough:

```text
Hero
  Digital Twin
  Tuesday, Jun 30
  Alex is focused
  Level/streak small line
  Energy / Focus / Stress chips

Next step card
  Now
  Start with a 30-second check-in.
  Pick a preset, adjust if needed, then save.
  [Check in now]   // real: expands/focuses check-in OR section is directly below

Check-in card
  Daily check-in
  Low / Okay / Strong presets
  Energy / Focus / Stress control / Social connection / Optimism
  [Save check-in] signed-in
  OR [Sign in to save] fixture-mode helper, not fake save

Quest card
  One small quest step
  Ship the smallest useful companion
  40% complete · daily
  After check-in: continue one small step.

Reflection card
  Short reflection paragraph
  Context: rising trend · mobile companion focus  // optional, quieter

Connection
  Signed in / Fixture mode
  status
  [Connection settings]
```

This is not a demand for exact copy or exact layout; it is the contract for hierarchy and behavior.

## Test expectations

Update or add static contract tests so regressions are caught:

- v2 has explicit `NextStep`/`CheckIn` hierarchy;
- old no-op `LauncherActions` row is gone or no longer has empty click handlers;
- fixture mode has meaningful check-in preview copy;
- connection settings remain collapsed by default;
- raw backend/debug details stay out of the main flow;
- no raw href/token/password displayed.

## Acceptance criteria

- First screen feels like a useful daily companion, not a dashboard.
- There is one obvious next action.
- Check-in action is real/usable or the dead action is removed/replaced.
- Fixture/prototype mode still feels meaningful.
- Connection settings remain collapsed by default.
- No raw backend/debug/security clutter in the main flow.
- No launcher files changed.
- Tests/lint/build pass.
- Runtime launch succeeds on phone/emulator.
- Screenshot confirms visible UX improvement.
