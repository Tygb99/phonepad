# Mobile App

> Korean version: [ko/MOBILE_APP.md](ko/MOBILE_APP.md)

## Purpose

Capture Android app implementation, build, permission, foreground service, and store-distribution rules.

## Current State

The product is Android-first. A native Kotlin Phase 0 debug APK exists for real-device Bluetooth HID testing. GitHub Releases APK is the first public distribution path; Play Store comes after v1.0 confidence.

## Current Rules

- Android API 28+ is required.
- Target SDK is currently Android 16 / API 36 for Galaxy S23 Ultra testing.
- Verified Phase 0 setup: Windows 11 PC, macOS 26.4.1 development host, Galaxy S23 Ultra on Android 16.
- The app must operate without `INTERNET`.
- Foreground service is allowed only for an active connected-device use case.
- Notification permission is requested after the user understands why reconnection/status needs it.

## Build Targets

| Target | Purpose |
|---|---|
| Debug APK | Phase 0 device tests. |
| Release APK | GitHub Releases distribution. |
| AAB | Play Store v1.1+ if store launch proceeds. |

GitHub Actions now builds the debug APK on push, pull request, and manual dispatch. Tagged `v*` builds attach the debug-labeled APK and checksum to GitHub Releases.

Current debug APK path:

```text
Android/app/build/outputs/apk/debug/app-debug.apk
```

## Device Test Order

1. Tap `권한` and allow Nearby devices.
2. The app automatically prepares the HID session and quietly tries the last successful PC.
3. If reconnect fails, tap `목록 새로고침`, choose a PC candidate, then tap `선택 호스트 연결`.
4. For a new PC, tap `새 PC 연결`, allow discoverable mode, then pair the visible `PhonePad - {device name}` device.
5. Use the right-side touchpad mode. Hold `스크롤 ↑` or `스크롤 ↓` to keep scrolling.

Continuous scroll buttons intentionally use small paced wheel reports to avoid host-side scroll acceleration on macOS.
The host list only shows paired computer-like devices and PCs that have previously connected successfully.
Opening the app alone does not show the Android discoverable prompt.
`호스트 연결/전환` is for switching among paired PCs. Double-tap drag is available as an explicit OFF-by-default option, and scroll button speed can be changed between slow/default/fast.

Local build command:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export ANDROID_SDK_ROOT="$ANDROID_HOME"
(cd Android && ./gradlew :app:assembleDebug)
```

Install command for a connected Android device:

```bash
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
"$ANDROID_HOME/platform-tools/adb" install -r Android/app/build/outputs/apk/debug/app-debug.apk
```

## Manifest Checklist

- `BLUETOOTH_CONNECT` for Android 12+.
- `BLUETOOTH_ADVERTISE` if needed by the pairing path.
- `BLUETOOTH_SCAN` only after spike evidence.
- `POST_NOTIFICATIONS` requested only at runtime after first successful connection.
- `FOREGROUND_SERVICE` and connected-device foreground service declaration before background connected operation ships.
- No `INTERNET`.
- No ad, analytics, or crash-reporting SDK by default.

## Foreground Service Rules

- Start while actively connected or maintaining HID connection.
- Show persistent status notification.
- Stop when user disconnects and no reconnect attempt is active.
- Always attempt `releaseAllMouseButtons()` before service stop.

## Store Positioning

- GitHub-first release with tested device matrix.
- Play Store after support policy, privacy disclosure, and permission explanations are stable.
- F-Droid can be evaluated after reproducible builds are clean.

## Related Docs

- [HID.md](HID.md)
- [DEPLOY.md](DEPLOY.md)
- [SECURITY.md](SECURITY.md)
- [TEST.md](TEST.md)
