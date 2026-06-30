# Companion UX Redesign v1

## Problem

The native Digital Twin companion currently feels like a debug dashboard rather than a daily companion:

- too many equal cards;
- backend/login/settings controls are always visible;
- raw URLs/hrefs show in the main flow;
- actions are not visually prioritized;
- the check-in flow is dense and technical;
- the visual style is generic Material cards instead of a calm personal system.

The launcher is now minimalist and text-first; the companion app should feel equally intentional, but it can be richer than the launcher because it is where the user acts.

## Direction

A phone-first daily companion, not a desktop dashboard.

Design mood:

- warm minimalism, closer to Notion/Apple than generic Material;
- calm paper-like background;
- one clear primary action near the top;
- readable hierarchy and generous spacing;
- soft cards/pills only where they clarify;
- no exposed debug details in the main daily flow.

## Main job

When the user opens the companion, they should immediately understand:

```text
How am I today?
What should I do now?
What is my current quest?
Where do I go if I need settings?
```

## Proposed first screen

```text
Digital Twin
Today, Jun 30

<name> is focused
Level 3 · 420 / 600 XP · 5-day streak

[primary action]
Check in / Continue quest / Reflect

Today
Energy / Focus / Stress as small calm chips
Check-in completed state or quick presets

Quest
Ship the smallest useful companion
42% · this week
[-10] [+10] [Complete]

Reflection
One short paragraph

Open web app · Reflect

Connection
Signed in · Last updated ...
[Connection settings]
```

## Rules

- Keep existing repository/network/persistence behavior.
- Keep fixture mode behavior.
- Keep check-in presets and rating adjustment functionality.
- Keep quest progress controls when signed in.
- Keep sign-in/settings available, but collapse them behind `Connection settings`.
- Do not display raw backend URL, auth token, email, or raw hrefs in the main daily flow.
- Do not show raw URLs/hrefs as plain text. If links are used, use labeled buttons/text actions.
- No new navigation framework yet.
- No bottom tabs yet.
- No launcher changes in this slice.

## Component direction

### Shell

- Background: warm off-white, e.g. `#FAFAF8` / `#F6F5F2`.
- Content max is implicit phone width; padding 20–24dp.
- Use vertical scroll, but top hierarchy should fit in first viewport.

### Header

- `Digital Twin` as title, but less generic dashboard bold.
- Date/status below.
- Mood sentence is the emotional hook.

### Primary action

- A clear card or pill near top from `recommendedTodayAction(today)`.
- Action copy should be human: `Check in`, `Continue quest`, `Reflect`.
- If the action opens a section already on screen, it can be a visual cue rather than complex navigation.

### Check-in

- If completed: calm completed state.
- If pending: three presets first (`Low`, `Okay`, `Strong`) and then ratings as quieter controls.
- Submit is one full-width primary button.

### Quest

- Show goal, progress, duration.
- Show update controls only if authenticated and active quest controls are available.
- Hide raw next-action href; show label/reason only.

### Reflection

- Short paragraph in a lower-density card.
- Avoid analytics-looking metric clutter.

### Connection settings

- Show a compact status row by default.
- Hide URL/email/password inputs until expanded.
- No token display.
- Keep production URL and login flows intact.

## Acceptance criteria

- Main UI no longer has raw href text in the quest card.
- Main UI no longer always shows backend URL/email/password fields.
- TodayScreen has clear components for daily hero, primary action, connection summary/settings.
- Existing unit tests still pass.
- New/updated static contract test covers the UX direction.
- APK builds and launches.
- Phone/emulator screenshot shows materially better hierarchy.
