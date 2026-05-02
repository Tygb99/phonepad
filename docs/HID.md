# Bluetooth HID

## Purpose

Define the Direct Bluetooth HID path that makes Android appear as a standard input device to Windows and macOS.

## Current State

The v1.0 bet is Android `BluetoothHidDevice`. Phase 0 must verify that real Android devices can register, advertise, pair, and send mouse/keyboard reports reliably.

## Current Rules

- Treat "Bluetooth HID" as confirmed language only after host and btsnoop observations.
- Do not claim HOGP until transport/profile evidence confirms it.
- Use a minimal mouse + keyboard composite descriptor for MVP.
- Avoid `BLUETOOTH_SCAN` unless the Phase 0 permission spike proves it is required.
- Do not implement Precision Touchpad descriptor in v1.0.

## Android Build Baseline

```text
minSdk: 28
targetSdk: 35
compileSdk: 35+
foregroundServiceType: connectedDevice
```

## Permissions

| Permission | Rule |
|---|---|
| `BLUETOOTH_CONNECT` | Required on Android 12+ for Bluetooth operations. |
| `BLUETOOTH_ADVERTISE` | Expected for discoverability/advertising path. |
| `BLUETOOTH_SCAN` | Add only if pairing/reconnect cannot work without it. |
| `POST_NOTIFICATIONS` | Ask after first successful connection with clear reason. |
| `FOREGROUND_SERVICE_CONNECTED_DEVICE` | Required for connected foreground service on modern Android. |
| `INTERNET` | Must not be declared. |

## Phase 0 HID Spikes

| Spike | Success Criteria |
|---|---|
| Profile access | `getProfileProxy(BluetoothHidDevice)` succeeds on at least Pixel and Samsung test devices. |
| Registration | `registerApp` succeeds with a minimal descriptor. |
| Mouse report | Host cursor moves from `sendReport`. |
| Click/scroll | Left click, right click, vertical/horizontal scroll work. |
| Keyboard report | Three shortcut reports work on Windows/macOS. |
| Transport evidence | Windows/macOS device names and btsnoop logs are recorded. |
| Permission minimum | Confirm whether `BLUETOOTH_SCAN` can be omitted. |

## HID Report Strategy

- Mouse report: `dx`, `dy`, `buttons`, `wheel`, `horizontalWheel`.
- Keyboard report: modifiers plus up to six keycodes.
- Drag Mode ON: mouse reports include left-button bit.
- Drag Mode OFF: send a button-up report with all buttons cleared.
- Safe release: send all-buttons-up on disconnect, unregister, host switch, app background exit path, screen lock, and process shutdown hook where possible.

## Host Testing Notes

- Windows 11 latest is a release gate.
- Current macOS is a release gate.
- Windows 10 22H2 is best-effort compatibility.
- Linux is not officially supported in v1.0.
- BIOS/UEFI is not promised for Direct Bluetooth mode.

## Related Docs

- [ARCHITECTURE.md](ARCHITECTURE.md)
- [DRAG_MODE.md](DRAG_MODE.md)
- [TEST.md](TEST.md)
- [MOBILE_APP.md](MOBILE_APP.md)
- [BRIDGE_DONGLE.md](BRIDGE_DONGLE.md)
