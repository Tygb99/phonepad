# Internal API

> Korean version: [ko/API.md](ko/API.md)

## Purpose

Define the app-internal interfaces between Flutter, Android native HID code, runtime controllers, and the optional Bridge Dongle spike.

## Current State

No public web API is planned for v1.0. This document covers local MethodChannel/EventChannel contracts and the draft dongle packet protocol.

## Current Rules

- Keep the Flutter/native boundary small.
- Native Kotlin owns Bluetooth HID registration and report sending.
- Flutter owns trackpad UI, visible state, settings screens, and gesture input surface.
- Expose errors as typed codes, not free-form strings only.
- Do not add network APIs for v1.0.

## Flutter To Android Native

```kotlin
getCompatStatus(): CompatStatus
requestNearbyDevicePermissions(): PermissionResult
requestNotificationPermission(): PermissionResult
registerHidApp(): HidRegisterResult
unregisterHidApp()
startAdvertising()
stopAdvertising()
getPairedHosts(): List<Host>
connectToHost(address: String)
disconnect()
sendMouseReport(dx: Int, dy: Int, buttons: Int, wheel: Int, horizontalWheel: Int)
sendKeyReport(modifiers: Int, keycodes: List<Int>)
releaseAllMouseButtons(): Boolean
```

## Android Native To Flutter Events

```kotlin
onCompatStatusChanged(status)
onHidAppStatusChanged(registered: Boolean)
onConnectionStateChanged(state, host)
onHostPaired(host)
onReconnectAttempt(host, attemptNo, success, durationMs)
onMouseButtonReleaseAttempt(reason, success)
onError(code, message)
```

## Runtime Controller Rules

```kotlin
enableDragMode()
// state = enabling
// sendMouseReport(0, 0, LEFT_BUTTON, 0, 0)
// state = on

disableDragMode(reason)
// state = release_pending
// sendMouseReport(0, 0, 0, 0, 0)
// state = off on success

onPointerMove(dx, dy)
// if drag mode on: buttons = LEFT_BUTTON
// otherwise: buttons = 0
```

## Bridge Dongle Packet Draft

The detailed BLE packet source of truth is [DONGLE_PROTOCOL.md](DONGLE_PROTOCOL.md). Keep the v1 4-byte mouse packet compatible while adding v2 safety and chord commands.

```text
Packet: MouseMove
- type: 0x01
- dx: int16
- dy: int16
- buttons: uint8
- wheel: int8
- hwheel: int8

Packet: KeyCombo
- type: 0x02
- modifiers: uint8
- keycodes: uint8[6]

Packet: Control
- type: 0x03
- command: release_all_buttons | ping | ack
```

Current iOS sender + ESP32-S3 spike packet types:

```text
0x10: ReleaseAll(seq)
0x11: KeyChord(seq, modifier_mask, keys[0...6])
0x12: LanguageToggle(seq, host_profile)
```

## Error Code Families

| Prefix | Meaning |
|---|---|
| `compat.*` | Android version, Bluetooth, HID profile, or permission failure. |
| `hid.*` | Registration, descriptor, sendReport, unregister failure. |
| `host.*` | Pairing, reconnect, active host, or OS preset failure. |
| `drag.*` | Drag Mode state or release failure. |
| `perm.*` | Runtime permission denial or missing declaration. |

## Related Docs

- [ARCHITECTURE.md](ARCHITECTURE.md)
- [HID.md](HID.md)
- [DRAG_MODE.md](DRAG_MODE.md)
- [BRIDGE_DONGLE.md](BRIDGE_DONGLE.md)
- [DONGLE_PROTOCOL.md](DONGLE_PROTOCOL.md)
