# Native UI Redesign v2 Polish

## User verdict

The v1 prototype works, but the UI still sucks.

That feedback is correct. v1 improved dark theme and features, but still feels like oversized debug cards with giant pills.

## Evidence from physical phone screenshots

Captured in:

```text
.hermes/evaluations/native-ui-redesign-v1/phone-screens/
```

Problems:

1. **Too chunky**
   - enormous pill buttons;
   - oversized headings;
   - too much vertical dead space;
   - cards look like inflated status blocks.

2. **Launcher top chrome is bad**
   - six visible pills near the top is too many;
   - `Refresh`, `App drawer`, `Companion`, `Settings`, plus page tabs compete;
   - it looks like a control panel, not a launcher.

3. **Chat is present but awkward**
   - keyboard opens/covers too much;
   - thread is hidden behind keyboard;
   - prompt chips are useful but too big;
   - needs a calmer chat-first shell.

4. **Quest page is not useful enough**
   - if no active quest, it must give a useful next step;
   - if active quest exists, progress and one action must be obvious.

5. **Visual language lacks premium restraint**
   - too much cyan;
   - too many outlines;
   - active states shout;
   - no subtle hierarchy.

## Target style

Use a restrained dark-product style inspired by Linear/Superhuman, adapted to native Compose:

```text
near-black canvas
slightly raised dark surfaces
thin low-opacity borders
fewer saturated fills
one accent used sparingly
compact Inter/system typography
one obvious primary action per screen
```

Do not copy those sites directly. Use the taste direction:

- Linear-like: precision, reduced chrome, subtle borders, compact controls.
- Superhuman-like: confident spacing, luxury restraint, no visual clutter.

## Product target

The native app should feel like:

```text
A calm phone-native mission-control companion.
Open → understand today → ask/chat/act → close.
```

Not:

```text
A debug dashboard with giant buttons.
```

## V2 acceptance

### Companion

- Keep dark theme.
- Keep horizontal pages: Today / Quest / Chat / Settings.
- Reduce header and tab row height.
- Use compact segmented tabs/chips, not giant pills.
- Cards should feel like precise panels, not huge boxes.
- Today first viewport should show:
  - small app header;
  - current state;
  - one next action;
  - compact check-in/quest summary if space allows.
- Chat page should not auto-focus/open keyboard on initial navigation.
- Chat composer should be visible and reachable, but not dominate until tapped.
- Suggested prompts should be compact chips.
- Quest page must have useful empty state and useful active state.

### Launcher

- Keep default-Home-safe behavior. Do not touch task-affinity/default-home behavior.
- Remove the giant top action grid feel.
- Top should feel like a launcher home:
  - title/status;
  - one primary action: Companion or Ask Twin;
  - smaller secondary actions: Apps / Refresh / Settings.
- Page tabs should be compact or moved below hero.
- Today page should be glanceable.
- Apps page should emphasize allowed/favorite apps, not look like settings.
- Twin page should provide two useful actions: Ask Twin / Reflect.

### Safety / privacy

- Do not display token, bearer, password, raw backend URLs, or raw JSON.
- Do not claim fake AI/DCT/journal ingestion.
- Do not automate or request default Home.
- Do not commit or push.
- Do not run full tests/lint yet; compile-only is okay for screenshot build.

## Visual rules

- Background: near black, not blue-black everywhere.
- Main surfaces: translucent/raised dark gray, subtle 1dp border.
- Accent: cyan/teal only for one primary CTA or active marker.
- Text: avoid giant all-white blocks; use muted supporting text.
- Radius: reduce from huge pills/cards. Prefer 16–22dp panels, 12–16dp buttons.
- Buttons: min touch target still >=48dp, but visual shape smaller/cleaner.
- Spacing: use 8dp rhythm; reduce vertical padding by ~25–35% from v1.
- No duplicate CTAs.

## Verification for this stage

Only after implementation:

1. compile-only `assembleDebug`;
2. install on physical phone;
3. capture screenshots;
4. logcat smoke scan for ANR/crash;
5. ask user to judge v2.

Do not run full tests/lint/commit until the user likes the visual direction.
