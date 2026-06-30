# Widget v0 Plan

Widget code is deferred from the first scaffold to keep the initial Android app buildable and focused on the `/api/mobile/today` contract.

## Exact next implementation

1. Add Glance dependencies after the base app build is green:
   - `androidx.glance:glance-appwidget`
   - `androidx.glance:glance-material3`
2. Create `TodayWidgetReceiver` extending `GlanceAppWidgetReceiver`.
3. Create `TodayWidget` extending `GlanceAppWidget`.
4. Render only privacy-safe summary fields:
   - mood label
   - streak
   - next action label
   - current quest title or fallback
5. Keep widget data sourced from the same `MobileToday` repository contract.
6. Store no JWT, email, raw journal, raw chat, or password in widget state.
7. Add `res/xml/today_widget_info.xml` with a small home-screen footprint.
8. Register the receiver in `AndroidManifest.xml` without any launcher/default-home intent filters.
9. Add unit tests for widget summary mapping from `MobileToday`.
10. Add a manual verification step on emulator/device:
    - install debug APK
    - add widget
    - confirm privacy-safe fields render
    - confirm tapping action opens the companion app, not a launcher mode
