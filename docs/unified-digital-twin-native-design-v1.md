# Unified Digital Twin Native Design v1

Status: **design proposal for review**  
Implementation: **not started**  
Scope: Android launcher + companion app + widget/notification bridge  
Goal: make every surface feel like one coherent system, not three unrelated prototypes.

---

## 0. User verdict that caused this redesign

The current launcher still looks bad. The companion also does not yet feel like the real Digital Twin product.

The deeper problem is not one button, color, or card. The problem is that the launcher and companion currently feel like different experiments:

```text
Launcher = black text launcher prototype
Companion = dark dashboard / debug app
Widget = separate small summary
```

The new target is:

```text
Digital Twin Native System
  Launcher = ambient / quiet shell
  Companion = expanded daily intelligence
  Widget = tiny glance of the same state
  Notification = timely nudge from the same system
```

They must feel merged, like one phone-native OS layer.

---

## 1. Research summary

### 1.1 Minimal launcher references

Sources sampled:

- **Oasis Launcher official site:** `https://www.oasislauncher.com`
- **Oasis Google Play:** `https://play.google.com/store/apps/details?id=com.crimson.oasislauncher&hl=en_US`
- **Oasis review/setup article:** `https://oasislauncher.com/blog/oasis-launcher-review`
- Niagara Launcher official site: `https://niagaralauncher.com`
- Before Launcher official site: `https://beforelabs.com`
- Olauncher Google Play: `https://play.google.com/store/apps/details?id=app.olauncher&hl=en_US`
- Existing project reference: `docs/native-launcher-v4-minimal-reference.md`

Important user context:

```text
User currently uses Oasis Launcher.
Therefore Oasis is the primary taste anchor for launcher direction.
Niagara/Olauncher/Before are secondary references only.
```

Patterns worth copying:

#### Oasis Launcher

Oasis is not pure empty minimalism. It is **minimal phone + productivity side system**.

Observed from official copy, Play Store, and screenshots:

- dark/wallpaper-first home screen;
- prominent stylized day/date/time block;
- very short vertical favorite app list;
- app names are text-first, mostly no icons;
- tiny corner shortcuts such as phone/camera;
- app drawer has `Apps`, search, folders/categories, simple list rows;
- “Productivity Oasis” page holds To-do, Notes, Calendar, Year/Month/Week/Day progress, App Usage;
- app interrupts add friction with timer/math/reflection before distracting apps;
- focus mode can reduce home access to only four essential apps;
- themes/fonts/wallpapers are important, not afterthoughts;
- user reviews specifically praise the app list, easy interface, side widgets, styling, and customization.

Design implication:

```text
Do not make Digital Twin Launcher a bare Olauncher clone.
Make it Oasis-like: a beautiful quiet home plus a richer side/companion surface.
The launcher home stays calm; the Digital Twin intelligence lives in the side Oasis/companion layer.
```

#### Niagara

Niagara describes itself around:

- decluttering Android home screen;
- list-based interface;
- one-handed app access;
- simple impactful features;
- ad-free / distraction-free experience;
- apps first, not dashboard cards.

Design implication:

```text
Home must be a calm vertical app/control surface, not a content dashboard.
```

#### Before Launcher

Before emphasizes:

- minimalist home screen;
- prioritize needed apps;
- mute trivial notifications;
- apps sorted/searchable;
- minimal but still customizable.

Design implication:

```text
Launcher can support productivity/digital wellbeing, but the visible home must remain quiet.
```

#### Olauncher

Olauncher emphasizes:

- no icons, ads, or distraction;
- clean home screen;
- rename/hide apps;
- gestures;
- hidden settings;
- no data collection;
- “just enough features.”

Design implication:

```text
The launcher should feel deliberately underpowered. Hidden capability is better than visible clutter.
```

### 1.2 Companion / habit / self-care references

Sources sampled:

- Daylio official site: `https://daylio.net`
- Finch App Store page: `https://apps.apple.com/us/app/finch-self-care-pet/id1528595748`
- Replika App Store / Google Play snippets from search

Patterns worth copying:

#### Daylio

Daylio works because its daily interaction is tiny:

```text
pick mood → pick activities → optionally write note → stats later
```

It avoids forcing long journaling. It also keeps privacy, dark mode, goals, reminders, trends, and export as deeper features.

Design implication:

```text
Daily check-in must be very fast. Deeper reflection/analytics come after the daily capture.
```

#### Finch

Finch works because self-care is embodied through a companion loop:

```text
quick check-ins
small goals
journaling prompts
breathing/exercises
rewards/progress
insights
```

Design implication:

```text
Digital Twin needs emotional continuity, not just metrics. It should feel like a companion that returns with context.
```

#### Replika-like AI companion pattern

Relevant pattern:

```text
chat is central, but memory/context makes it feel personal
```

Design implication:

```text
Ask Twin must be reachable as a core flow, but it should be grounded in Today/check-in/quest context, not a generic chatbot.
```

### 1.3 Visual design references

Loaded design skills/templates:

- `popular-web-designs`: Linear, Superhuman, Notion, Apple
- `phone-first-pwa-ux`
- `android-companion-ux`

Useful style lessons:

#### Linear

- dark-native system;
- near-black canvas;
- thin low-opacity borders;
- precise typography;
- one accent used sparingly.

#### Superhuman

- luxury restraint;
- confident spacing;
- minimal decoration;
- strong typographic hierarchy.

#### Notion

- warm minimalism;
- paper-like clarity;
- approachable calm;
- light structure, not heavy chrome.

#### Apple

- product surfaces breathe;
- one primary message per screen;
- cinematic section rhythm;
- blue/accent only for true interactions.

Design implication:

```text
Use Linear precision + Notion warmth + Apple restraint.
Do not use generic Material dashboard styling.
Do not flood the app with cyan.
```

---

## 2. Product thesis

Digital Twin should feel like a **quiet personal operating layer**, closer to an Oasis-style minimalist phone system than a plain text launcher.

It is not:

```text
AI dashboard
launcher theme
habit app clone
analytics app
chatbot skin
```

It is:

```text
A calm phone-native companion that knows today's state,
helps you choose the next small action,
and stays out of the way when you just need your phone.
```

### Core loop

Every surface should support one loop:

```text
Notice state → choose next action → act/check in/chat → return to life
```

### System promise

```text
The launcher keeps the phone quiet.
The companion gives the day meaning.
The widget keeps the signal visible.
Notifications nudge only when useful.
```

---

## 3. Unified design language: “Quiet Signal”

Working name: **Quiet Signal**

Mood:

```text
calm
premium
low-noise
human
slightly futuristic
not gamer
not neon dashboard
not corporate wellness
```

### 3.1 Color system

Stop using bright cyan as the main visual identity. It makes the UI feel cheap and prototype-y.

Proposed palette:

```text
Canvas / Void
  #050506  app/launcher root background
  #090A0D  elevated dark background
  #101116  primary surface

Paper / Ink
  #F4F1EA  primary text, warm off-white
  #C8C2B6  secondary text
  #8F8A80  muted metadata
  #57534A  disabled/hidden text

Signal accent
  #A78BFA  Twin violet / primary intelligence accent
  #7C6CF2  pressed/active violet
  #D8B4FE  soft highlight

Human warmth
  #F6C177  streak/energy/warm state, very sparing
  #A7F3D0  success, very sparing
  #FCA5A5  warning/error, only when needed

Borders
  rgba(255,255,255,0.06) default hairline
  rgba(255,255,255,0.10) active/elevated hairline
```

Rules:

- Violet is the Digital Twin signal, not a decoration flood.
- Warm yellow is only for streak/energy moments.
- Green is only for completion/success.
- Red is only for errors/destructive states.
- Most UI should be black, warm white, and muted gray.

### 3.2 Typography

Use system font / Inter-like behavior.

Launcher:

```text
Day/date block    32–44sp stylized, Oasis-like but cleaner
Clock             28–40sp, can sit inside/near date block
Status            14–16sp, muted
App rows          18–24sp, regular/medium
Corner shortcuts  18–22dp icons, tiny and quiet
Hints/footer      12–14sp, almost hidden if needed
```

Oasis correction:

```text
Do not over-enlarge app rows like Olauncher.
Oasis feels better because app names are compact, wallpaper-backed, and leave breathing room.
```

Companion:

```text
App label          13sp, medium, muted
Screen title       24–32sp, semibold
Hero state         32–42sp, semibold
Body               15–17sp, regular
Card title         17–20sp, semibold
Metadata           12–14sp, muted
Primary button     16–17sp, semibold
```

Rules:

- Avoid all-caps except tiny labels.
- Use fewer weights: regular, medium, semibold.
- Never use huge bold white text blocks everywhere.
- Text is the design; do not rely on icon decoration.

### 3.3 Shape and surfaces

Launcher should have almost no surfaces.

Companion may use surfaces, but they should be precise:

```text
Surface card radius     22dp
Button radius           16dp
Chip radius             999dp
Hairline border         1dp, low opacity
No heavy shadows
No giant pill stacks
No inflated cards
```

### 3.4 Motion

Motion should say “system intelligence,” not “party animation.”

Allowed:

- subtle fade between launcher and companion;
- card expansion from launcher “Digital Twin” row into companion hero;
- soft press/tonal feedback;
- tiny progress ring movement;
- swipe between companion pages.

Avoid:

- bouncing cards;
- neon glow pulses;
- constant gradients;
- busy loading skeletons.

---

## 4. Information architecture

### 4.1 System surfaces

```text
Launcher
  Ambient home
  App list / search
  Hidden settings

Companion app
  Today
  Ask
  Journal
  Quest
  Me / Settings

Widget
  Today signal
  Next action
  Refresh/open

Notification
  Check-in reminder
  Quest nudge
  Reflection prompt
```

### 4.2 One design grammar across all surfaces

Same state appears everywhere with different depth:

```text
Launcher:  focused · Check in
Widget:    Focused · 5-day streak · Check in
Companion: You're focused. Check in to tune today's plan.
Ask:       “Given I’m focused, what should I do next?”
Journal:   “Capture what made today focused.”
```

---

## 5. Launcher design

### 5.0 Oasis correction

Previous v4/v4.3 direction over-corrected toward a plain black text list. That is not enough.

Because the user currently uses Oasis Launcher, the new launcher should borrow Oasis's better balance:

```text
minimal home
+ strong date/time identity
+ short app list
+ optional wallpaper/theme personality
+ side productivity/intelligence surface
+ distraction friction where useful
```

The correct target is not:

```text
empty black Olauncher clone
```

The correct target is:

```text
Oasis-like calm phone home where Digital Twin is the native productivity/intelligence layer.
```

### 5.1 Launcher role

The launcher is not the Digital Twin product.

It is the **quiet front door**.

It should answer:

```text
What time is it?
What state am I in?
What app do I intentionally want?
How do I open Twin if I need guidance?
```

It should not answer:

```text
What are all my analytics?
What is every quest detail?
What are all settings?
What is the full chat thread?
```

### 5.2 Launcher visual target

Use almost no chrome.

Wireframe:

```text
┌────────────────────────────┐
│  8:16 PM                    │
│  Wednesday, Jul 1           │
│                             │
│  focused · 5-day streak     │
│  next: check in             │
│                             │
│                             │
│  Calendar                   │
│  Camera                     │
│  Chrome                     │
│  Messages                   │
│  Notes                      │
│                             │
│  Twin                       │  ← violet text, not giant CTA
│                             │
│                             │
│                             │
│       long press for setup  │  ← optional, nearly hidden
└────────────────────────────┘
```

### 5.3 Launcher actions

Visible actions:

```text
Tap app row       → launch app
Tap Twin          → companion Today
Swipe up          → app search/list
Long press blank  → launcher settings
Double tap        → lock screen later, optional
```

Avoid visible bottom footer if possible.

The previous footer made the launcher feel like a control panel. v5 should remove it or make it almost invisible.

### 5.4 Launcher app list/search

Drawer/search shape:

```text
┌────────────────────────────┐
│  Search apps                │
│  ─────────────────────      │
│  Calendar                   │
│  Camera                     │
│  Chrome                     │
│  Clock                      │
│  Contacts                   │
│  Drive                      │
│                             │
│  Manage shown apps          │  ← secondary, muted
└────────────────────────────┘
```

Rules:

- search field first;
- no app icons unless user explicitly asks;
- no card rows;
- app labels cleaned;
- allow/hide management is secondary, not on every row by default;
- long press app row can show actions: show/hide/rename.

### 5.5 Launcher state line

The state line should use Today cache only:

```text
focused · 5-day streak
next: check in
```

If no data:

```text
twin offline
open Twin to connect
```

No raw backend errors on launcher.

### 5.6 Launcher acceptance checklist

A screenshot should pass this stranger test:

```text
Would a stranger immediately recognize this as a minimalist launcher?
```

Must show:

- clock/date;
- simple vertical app names;
- one Twin row;
- no dashboard cards;
- no footer control panel;
- no tabs;
- no giant buttons;
- no raw debug/status text.

---

## 6. Companion app design

### 6.1 Companion role

The companion is the **expanded Digital Twin**.

It should answer:

```text
How am I today?
What should I do now?
Why?
Can I talk to my Twin?
Can I capture/reflection/journal?
Can I progress my quest?
```

### 6.2 Companion top-level navigation

Use 5 destinations max:

```text
Today | Ask | Journal | Quest | Me
```

Why not Settings as a tab?

Because Settings is not a daily destination. Put connection/profile/backend details inside `Me`.

Why `Ask` instead of `Chat`?

Because “Ask” is action-oriented and less generic. It means: ask your Twin what to do, reflect, plan.

### 6.3 Companion shell

Wireframe:

```text
┌────────────────────────────┐
│ Digital Twin       focused │
│ Wed, Jul 1 · cached        │
│                            │
│ ┌────────────────────────┐ │
│ │ You are focused.       │ │
│ │ Check in to tune today │ │
│ │                        │ │
│ │ [Check in]             │ │
│ └────────────────────────┘ │
│                            │
│ Energy  Focus  Stress      │
│  3/5     4/5     2/5       │
│                            │
│ Next small step            │
│ Continue quest for 10 min  │
│                            │
│ Reflection                 │
│ One sentence from backend  │
│                            │
│ Today Ask Journal Quest Me │
└────────────────────────────┘
```

### 6.4 Today screen

Purpose:

```text
Open → understand today → one action
```

Content order:

1. identity/date/status;
2. hero state sentence;
3. one primary CTA;
4. compact vitals;
5. next step;
6. current quest preview;
7. reflection preview;
8. connection status collapsed.

Primary CTA logic:

```text
if check-in not complete → Check in
else if active quest → Continue quest
else → Reflect with Twin
```

Visual rules:

- one hero panel only;
- no giant stacked cards above the fold;
- vitals are small text/pills, not dashboard metrics;
- reflection is one or two lines, not a paragraph block;
- no backend URL/token/debug in Today.

### 6.5 Check-in flow

Check-in must feel like Daylio-simple.

Flow:

```text
How are you arriving?
[Low] [Steady] [Strong]

Fine tune
Energy       [-] 3 [+]
Focus        [-] 4 [+]
Stress       [-] 2 [+]
Connection   [-] 3 [+]
Optimism     [-] 3 [+]

[Save check-in]
```

States:

```text
Not signed in → local preview only, clearly says not saved
Signed in     → submit, refresh Today, update widget
Done today    → calm completed state
```

Completed state:

```text
Today's check-in is done.
Next: continue quest / ask Twin.
```

No duplicate CTA.

### 6.6 Ask screen

Purpose:

```text
Talk to Twin with today's context.
```

Wireframe:

```text
┌────────────────────────────┐
│ Ask Twin                   │
│ Context: focused · quest   │
│                            │
│ Twin                       │
│ Based on today, start with │
│ the smallest next step...  │
│                            │
│ You                        │
│ Help me plan the next hour │
│                            │
│ [Plan today] [Reflect]     │
│ [Choose quest] [Calm down] │
│                            │
│ Message Twin...        send│
└────────────────────────────┘
```

Rules:

- composer pinned at bottom;
- message list scrolls, not whole screen awkwardly;
- suggested prompts compact;
- no auto keyboard on opening Ask;
- if unsigned, local-only preview message explains sign-in needed.

Core prompt chips:

```text
Plan today
Reflect
Pick next step
Decompress
Quest idea
Summarize me
```

### 6.7 Journal screen

Purpose:

```text
Capture thought quickly and connect it to Today.
```

Wireframe:

```text
┌────────────────────────────┐
│ Journal                    │
│ Capture the signal.        │
│                            │
│ What's worth remembering?  │
│ ┌────────────────────────┐ │
│ │                        │ │
│ └────────────────────────┘ │
│ [Save note] [Ask Twin]     │
│                            │
│ Recent                     │
│ 8:04 PM  felt clear after… │
│ Yesterday  avoided drift…  │
└────────────────────────────┘
```

Rules:

- inline composer, not modal black card;
- one create action;
- local draft if offline;
- no raw journal/chat ingestion claims until backend supports it;
- when backend support is missing, label as planned feature.

### 6.8 Quest screen

Purpose:

```text
Turn intention into progress.
```

Active quest state:

```text
Quest
Ship companion v1
40% complete

[-10] [ +10 ]
[Mark done]

Next: test on phone
Ask Twin for next step
```

Empty quest state:

```text
No active quest.
Choose one small thing for today.
[Ask Twin for quest]
```

Rules:

- progress control must be obvious;
- no duplicate Create/New Quest buttons;
- completion state should feel rewarding but calm;
- quest should borrow the same visual grammar as Today hero.

### 6.9 Me screen

Purpose:

```text
Identity, progress, settings, connection, privacy.
```

Contains:

```text
Profile / level / XP
Streak / mood history summary
Connection settings collapsed
Widget status
Privacy/export later
Sign out
Advanced debug hidden behind disclosure
```

Rules:

- backend URL/email are not on Today;
- token/password never shown;
- raw errors go here or temporary toast/status, not launcher;
- settings feel like a phone settings list, not a form dashboard.

---

## 7. Widget design

Widget is the smallest Digital Twin surface.

Current data available:

```text
mood
streak
quest
nextAction
cacheLabel
refreshLabel
```

Target widget:

```text
┌──────────────────────┐
│ Digital Twin          │
│ focused · 5-day       │
│ Next: Check in        │
│ Cached · tap refresh  │
└──────────────────────┘
```

Rules:

- same colors as companion, but higher contrast;
- no transparent text on dark wallpaper;
- no clipping;
- tap main area opens companion Today;
- refresh text is small.

---

## 8. Notification design

Notifications should feel like Twin nudges, not app spam.

Types:

```text
Morning check-in
  “Tune today before the day runs you.”

Quest nudge
  “10 minutes is enough to keep the quest alive.”

Reflection prompt
  “Capture one thing worth remembering.”
```

Rules:

- max one proactive nudge per context window;
- never guilt/shame;
- use actions: Check in / Ask Twin / Snooze;
- notification should open the matching companion tab.

---

## 9. Feature roadmap inside the design

### Phase A — Unified visual reset

Goal:

```text
Launcher + companion + widget look like the same product.
```

Includes:

- shared color tokens;
- shared typography sizes;
- shared component grammar;
- launcher v5 visual reset;
- companion shell reset;
- widget visual alignment.

No new backend features required.

### Phase B — Companion core features

Goal:

```text
The companion becomes useful every day.
```

Includes:

- Today hero with real primary action;
- completed check-in state;
- cleaner check-in flow;
- Ask screen with context prompts;
- Quest screen active/empty states;
- Me screen replacing Settings tab.

Uses existing current data:

- `MobileToday.user`
- `MobileToday.checkIn`
- `MobileToday.quest`
- `MobileToday.insight`
- current chat repository
- current check-in/quest repositories

### Phase C — Journal / memory bridge

Goal:

```text
Capture thoughts and make them available to Twin later.
```

Includes:

- Journal tab UI;
- local drafts;
- backend endpoint needed for persisted journal entries;
- later Ask Twin context integration.

Backend likely needed if not already present.

### Phase D — Native system integration

Goal:

```text
Digital Twin feels native to the phone.
```

Includes:

- widget polish;
- notification actions;
- app shortcuts;
- default-home trial only after explicit approval;
- launcher gestures;
- app hide/rename management.

### Phase E — Advanced intelligence

Goal:

```text
Digital Twin grows into deeper personal intelligence.
```

Includes:

- weekly patterns;
- timeline/history;
- memory review;
- profile progression;
- forecast/blueprint;
- automation suggestions;
- stronger personalization.

Not in first native design pass.

---

## 10. Unified component system

### 10.1 Components

```text
TwinShell
  shared background, safe areas, status treatment

SignalHero
  large daily state + one action

QuietRow
  text-first row for launcher/apps/settings

SignalChip
  small metric/status pill

ActionPanel
  one-card focused action block

TwinComposer
  Ask/Journal input shell

ProgressStrip
  quest/check-in progress visual

SettingsDisclosure
  collapsed technical controls
```

### 10.2 Shared states

```text
Offline
  muted, calm, “Open Twin to connect”

Fixture/local
  “Preview only — sign in to save”

Cached
  “Cached · 8:04 PM”

Loading
  keep stale content, show tiny loading line

Error
  no panic red full screen; small error in context

Done today
  completed state replaces CTA
```

---

## 11. Concrete screen-by-screen design spec

### 11.1 Launcher Home v5

Must remove:

- footer `Twin / Settings` if it still feels control-panel-like;
- any “Apps” footer;
- status paragraphs;
- cyan row screaming `Digital Twin`;
- dashboard card remnants.

Must show:

```text
Oasis-style day/date/time identity block
short favorite app list
subtle Twin state line or Twin row
small phone/camera or search affordance if needed
wallpaper/theme personality
```

Final preferred wireframe, Oasis-inspired:

```text
┌────────────────────────────┐
│                            │
│        WEDNESDAY           │
│     08:16  ·  Jul 1        │
│                            │
│          focused           │
│        next: check in      │
│                            │
│                            │
│  Spotify                   │
│  Gallery                   │
│  Gmail                     │
│  Chrome                    │
│  My Files                  │
│  Twin                      │
│                            │
│  ☎                    ◉    │
└────────────────────────────┘
```

Notes:

- Date/time block should be stylized and memorable like Oasis, not generic Material text.
- App rows are compact, not huge.
- The `Twin` row replaces a loud companion CTA.
- Background can be black, warm gradient, or curated wallpaper; not flat debug black only.
- Small corner actions can exist, but they must not become a footer control panel.

Hidden/gesture actions:

```text
swipe up / tap search hint → Search apps
swipe right / tap Twin row → Twin Oasis side panel or companion Today
long press empty area → Launcher settings/theme
long press app row → Rename / Hide / Pin / Add interrupt
```

If hidden gestures are too hard for prototype, show one tiny bottom hint:

```text
search
```

But do not show three footer controls.

### 11.2 Twin Oasis side panel

This is the missing bridge between launcher and companion.

Oasis has a productivity page. Digital Twin should have a **Twin Oasis** page:

```text
Twin Oasis

Today
focused · 5-day streak
Check in

Quest
Continue for 10 minutes

Journal
Capture one thing

Ask
Plan the next hour
```

Rules:

- This page can use compact cards/panels because it is not the home screen.
- It should feel like Oasis Productivity Widgets, but with Digital Twin features.
- It is the merged layer between launcher and full companion.
- The full companion opens when the user taps deeper into Today/Ask/Journal/Quest.

### 11.3 Launcher Search

```text
Apps
Search apps

Recently installed        ˅
Social                    ˅
Work                      ˅

Calendar
Camera
Chrome
Clock
Contacts
Drive

Manage home apps
```

### 11.4 Companion Today

```text
Digital Twin               focused
Wed, Jul 1 · cached 8:04

You are focused.
Check in to tune today.

[Check in]

Energy 3  Focus 4  Stress 2

Next small step
Continue quest for 10 minutes

Reflection
One calm sentence...
```

### 11.5 Companion Ask

```text
Ask Twin
Context: focused · quest active

Twin: Based on today...

[Plan today] [Reflect] [Next step]

Message Twin...
```

### 11.6 Companion Journal

```text
Journal
Capture the signal.

What's worth remembering?
[input]
[Save note]

Recent
...
```

### 11.7 Companion Quest

```text
Quest
Ship companion v1
40%

[-10] [+10] [Done]

Ask Twin for next step
```

### 11.8 Companion Me

```text
Me
Level 4 · 320/500 XP
5-day streak

Widget active
Connected to production

Connection settings ▾
Privacy & export ▾
Sign out
```

---

## 12. What not to build yet

Do not build these before the visual system is approved:

```text
full analytics
timeline/history
memory browser
automation engine
default Home prompt/automation
complex onboarding
multi-card dashboard
large profile progression tree
```

Those are later.

First make the system beautiful and coherent.

---

## 13. Implementation strategy after approval

No implementation until user approves this design direction.

When approved:

### Slice 1 — visual tokens + companion shell

Files likely touched:

```text
TodayScreen.kt
possibly new ui/theme file
```

Output:

- Today/Ask/Quest/Me navigation shape;
- no Settings tab as primary;
- shared color tokens.

### Slice 2 — launcher v5

Files likely touched:

```text
launcher/LauncherScreen.kt
launcher/LauncherAppDrawer.kt
launcher/LauncherAppsRepository.kt
```

Output:

- no footer control panel;
- app rows + Twin row;
- app search gesture/screen;
- same Quiet Signal colors.

### Slice 3 — widget alignment

Files likely touched:

```text
widget/TodayGlanceWidget.kt
widget/TodayWidgetSummary.kt
```

Output:

- visual match;
- no clipping;
- high contrast.

### Slice 4 — feature flow polish

Files likely touched:

```text
TodayScreen.kt
repositories/models as needed
```

Output:

- completed check-in state;
- Ask screen stabilized;
- Journal placeholder/local draft if backend missing;
- Quest empty/active states.

---

## 14. Verification strategy after implementation

During visual iteration:

```text
compile-only assembleDebug
install on emulator/phone
capture screenshots
logcat smoke
ask user to judge
```

Do not run full lint/tests until:

```text
user likes the visual direction
or commit is imminent
```

Before commit:

```text
git diff --check
focused unit tests
lint
assembleDebug
install smoke
screenshots
commit
push
CI check
```

---

## 15. Design acceptance checklist for user review

Approve only if this feels right:

### Launcher

- [ ] Looks like a minimalist launcher, not an app dashboard.
- [ ] Feels connected to Digital Twin without being consumed by it.
- [ ] No footer control panel.
- [ ] App rows are calm and useful.
- [ ] Twin entry feels like a doorway, not a banner.

### Companion

- [ ] Looks like the same product as launcher.
- [ ] Feels like a daily companion, not debug/backend UI.
- [ ] Today screen has one obvious action.
- [ ] Ask Twin feels central and useful.
- [ ] Journal/Quest/Me are clear enough as future/current surfaces.

### System

- [ ] Same color/typography/component language across launcher, app, widget.
- [ ] Not too cyan/neon.
- [ ] Not too empty/plain.
- [ ] Not too dashboard-like.
- [ ] Feels native to phone.

---

## 16. My recommendation

Adopt this direction:

```text
Quiet Signal
  Launcher = ambient minimal home
  Companion = rich but calm daily intelligence
  Widget = tiny state mirror
  Notifications = sparse useful nudges
```

Do **not** keep trying to make the current launcher text list “good enough.” It needs to be part of a unified system and lose the remaining control-panel feel.

Do **not** add all advanced features immediately. Make the core daily loop feel excellent first:

```text
Today → Check in → Ask Twin → Quest/Journal → return to life
```
