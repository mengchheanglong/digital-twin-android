# Native UI Redesign v1

## Problem

The current Android surface technically works, but it feels too bare:

- launcher is mostly flat text;
- companion is a vertical stack of cards;
- useful features are buried behind one long scroll;
- no native chat surface exists;
- the app does not feel like a dark, modern phone companion.

## Product direction

Make the native app feel like a real daily companion / phone home surface:

```text
open -> see today -> swipe to quest/chat/apps/settings -> act -> leave
```

## v1 goals

1. Dark theme by default for launcher and companion.
2. Horizontal swipe/page navigation so the phone surface is not one long vertical pile.
3. Add a visible native Chat page.
4. Preserve useful existing features:
   - Today summary
   - check-in
   - quest progress controls
   - connection/login settings
   - app drawer/allowed apps
   - settings and companion escape paths
5. Keep privacy and safety:
   - no raw token/auth/header/backend debug strings in main UI;
   - no raw JSON;
   - no default-Home automation;
   - no fake “sent to AI” claims in fixture mode.

## Proposed companion pages

Use a swipeable pager with page chips/dots:

```text
Today | Quest | Chat | Settings
```

### Today

- dark hero with user state, level, streak, mood;
- one primary “next step” CTA/card;
- quick check-in visible without hunting.

### Quest

- active quest card;
- progress controls when signed in;
- reflection/insight below.

### Chat

- native chat thread UI;
- signed-in mode calls real backend chat API:
  - `POST /api/chat/send` with `{ message, chatId }`;
  - response uses `reply` and `chatId`;
- fixture mode shows local-only preview and does not pretend anything was sent;
- suggested chips can seed the composer:
  - “What should I focus on?”
  - “Help me reflect”
  - “Break down my quest”

### Settings

- connection/login/refresh controls;
- status visible;
- remain collapsed/clean by default where possible.

## Proposed launcher pages

Use a dark Home shell with horizontal pages:

```text
Today | Apps | Twin
```

### Today page

- large Digital Twin identity;
- today summary;
- refresh / companion / settings actions.

### Apps page

- allowed apps as thumb-friendly rows/chips;
- app drawer entry clear;
- if empty, explain how to allow apps.

### Twin page

- chat/reflect entry point;
- reminder that companion opens the full Chat page;
- keep Home safe and uncluttered.

## Non-goals for v1

- Do not build a full chat history browser.
- Do not add voice/audio.
- Do not add app blocking/filtering features.
- Do not redesign the backend.
- Do not automate Android default Home.
- Do not commit until screenshots are approved.

## Feedback checkpoint

This is visual/product work. After implementation, show screenshots first. Run tests/lint only after the user likes the direction or before committing.
