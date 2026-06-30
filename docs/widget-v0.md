# Widget v0

Widget v0 is implemented with AndroidX Glance and is intentionally cache-only.

## Behavior

The widget reads the last successful cached `/api/mobile/today` payload through `SharedPreferencesTodayCacheStore`. It does not perform network fetches, background polling, WorkManager jobs, notifications, or launcher/default-home behavior.

Displayed fields:

- `Digital Twin`
- mood emoji and label
- streak
- current quest goal or `No active quest`
- next action label
- `Open app to refresh`

If no cached Today payload exists, the widget shows `Digital Twin` and `Open app to refresh Today`.

## Privacy

The widget summary intentionally excludes token, password, email, backend URL, raw JSON, journal, chat, reflection text, and full insight content. Unit tests cover this display boundary.
