# Android 12 Compatibility Smoke Test - 2026-05-31

> Korean version: [ko/ANDROID12_COMPAT_SMOKE_2026-05-31.md](ko/ANDROID12_COMPAT_SMOKE_2026-05-31.md)

## Purpose

This report records the Android 12-or-below smoke test run after deferring `OnBackInvokedCallback` creation behind the Android 13+ API guard.

## Result

Pass. The debug APK launched on a real Android 12 Samsung device, the connection drawer opened and closed through the legacy back path, and logcat showed no startup or back-navigation crash signatures.

## Test Environment

| Item | Value |
|---|---|
| Date | 2026-05-31 |
| Branch | `codex/host-language-toggle-guide-panel` |
| Commit | `c2c4797 fix: defer drawer back callback creation` |
| App version | `0.2.0-phase0`, `versionCode=18` |
| APK | `app/build/outputs/apk/debug/app-debug.apk` |
| Device | Samsung `SM-N976N` |
| Android | `12`, API `31` |
| App target SDK | `36` |
| ADB serial | Wireless ADB local endpoint redacted |
| Orientation | Landscape |

## Checks

| Check | Result | Evidence |
|---|---|---|
| Build debug APK | Pass | `./gradlew assembleDebug` completed successfully. |
| Install APK | Pass | `adb install -r app-debug.apk` returned `Success`. |
| Runtime permissions | Pass | `BLUETOOTH_CONNECT` and `BLUETOOTH_ADVERTISE` were granted for the smoke run. |
| Cold launch | Pass | `MainActivity` became the focused window after force-stop and relaunch. |
| Android 12 startup compatibility | Pass | No `FATAL EXCEPTION`, `NoClassDefFoundError`, or `OnBackInvoked` logcat match. |
| Connection drawer open | Pass | Tapping `연결!` opened the PhonePad connection panel. |
| Legacy back handling | Pass | `adb shell input keyevent BACK` closed the drawer and returned to the main touchpad screen. |
| Disconnected waiting state service cleanup | Pass | `dumpsys activity services com.tygb99.phonepad` did not show `HidSessionService`. |

## Notes

- `uiautomator dump` returned `ERROR: null root node returned by UiTestAutomationBridge` on this device, so UI state was verified with screenshots, focused-window state, and logcat instead.
- Android 12 cannot exercise the Android 13+ `OnBackInvokedCallback` path. This run verifies that the API 31 runtime does not crash from callback class linkage and that the existing fallback back behavior still works.
- Host-side HID movement, click, scroll, drag, and language-toggle behavior were not covered by this Android 12 smoke test.

## Follow-Up

- Keep Android 12-or-below launch and drawer-back smoke checks in the host-language/guide-panel regression set.
- Continue validating the Android 13+ system-back callback path separately on an API 33+ device.
