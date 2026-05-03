# Architecture

> Korean version: [ko/ARCHITECTURE.md](ko/ARCHITECTURE.md)

## Purpose

Describe how PhonePad turns touch input into Bluetooth HID reports while keeping the desktop side serverless.

## Current State

The planned stack is Flutter for UI, Kotlin for Android Bluetooth HID, MethodChannel/EventChannel for integration, and SQLite/Drift for local host and settings data.

## Current Rules

- Prove the Kotlin HID path before investing heavily in Flutter UI.
- Keep host computers untouched except for standard Bluetooth pairing.
- Keep runtime Drag Mode state out of persistent storage.
- Keep all user input local to the device and Bluetooth HID transport.
- Keep Bridge Dongle work isolated from the app-only MVP.

## System Layers

```text
Flutter UI
  Trackpad surface, Drag toggle, host list, settings, gesture mapping
      |
      | MethodChannel / EventChannel
      v
Android Kotlin Native Layer
  BluetoothHidDevice registration, SDP, pairing, report sending,
  foreground service, lifecycle release hooks
      |
      | Bluetooth HID Device Profile
      v
Windows / macOS
  Standard HID mouse + keyboard composite input device
```

## Runtime Flow

1. App starts and checks Android version, Bluetooth availability, required permissions, and HID Device profile access.
2. Native layer registers the HID app with a minimal mouse/keyboard composite descriptor.
3. User pairs the Android phone from the host OS Bluetooth settings.
4. Flutter trackpad events are converted to normalized movement, button, wheel, and shortcut actions.
5. Kotlin native layer sends HID reports to the active host.
6. Lifecycle and connection events always attempt `releaseAllMouseButtons()` before unregistering or switching state.

## Main Components

| Component | Responsibility |
|---|---|
| `CompatChecker` | Android/BT/HID capability detection and unsupported-device reasons. |
| `HidDeviceService` | Register/unregister HID app, send reports, expose callbacks. |
| `HostRepository` | Persist paired hosts and default host preference. |
| `GestureEngine` | Convert pointer streams into movement, scroll, taps, and three-finger swipes. |
| `DragModeController` | Own Drag Mode state machine and release guarantees. |
| `SettingsRepository` | Persist sensitivity, scroll, haptics, theme, locale, and toggle position. |
| `DiagnosticsRecorder` | Store local test notes and copyable logs without telemetry. |

## Failure Handling

- Unsupported Android: show reason and link to compatibility notes.
- Registration failure: unregister, retry once, then show diagnostic details.
- Disconnect: set connection state to disconnected, release buttons, then attempt reconnect.
- Host switch: release buttons before selecting another host.
- Drag release failure: show visible warning and manual recovery guidance.

## Related Docs

- [HID.md](HID.md)
- [DRAG_MODE.md](DRAG_MODE.md)
- [DB.md](DB.md)
- [API.md](API.md)
- [SECURITY.md](SECURITY.md)
