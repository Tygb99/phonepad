# Mobile App

## Purpose

Capture Android app implementation, build, permission, foreground service, and store-distribution rules.

## Current State

The product is Android-first. GitHub Releases APK is the first distribution path; Play Store comes after v1.0 confidence.

## Current Rules

- Android API 28+ is required.
- Target SDK should stay aligned with current Play requirements.
- The app must operate without `INTERNET`.
- Foreground service is allowed only for an active connected-device use case.
- Notification permission is requested after the user understands why reconnection/status needs it.

## Build Targets

| Target | Purpose |
|---|---|
| Debug APK | Phase 0 device tests. |
| Release APK | GitHub Releases distribution. |
| AAB | Play Store v1.1+ if store launch proceeds. |

## Manifest Checklist

- `BLUETOOTH_CONNECT` for Android 12+.
- `BLUETOOTH_ADVERTISE` if needed by the pairing path.
- `BLUETOOTH_SCAN` only after spike evidence.
- `POST_NOTIFICATIONS` requested only at runtime after first successful connection.
- `FOREGROUND_SERVICE` and connected-device foreground service declaration.
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
