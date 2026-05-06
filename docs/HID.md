# Bluetooth HID

> Korean version: [ko/HID.md](ko/HID.md)

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
- Main UX auto-registers the HID session when the app enters foreground and unregisters when the app leaves.
- App foreground entry never opens the Android discoverable prompt by itself; only the new-PC action does.
- The app quietly attempts automatic reconnect to the last successful host after HID registration.
- PC-visible Bluetooth name uses `PhonePad - {device name}`.
- Host selection hides arbitrary nearby devices; it shows paired computer-like devices and previously successful PC hosts.
- Newly bonded hosts discovered after the new-PC flow are selected as the current switch target and connection is attempted without requiring another manual connect tap.
- Host switching is serialized: disconnect the current HID host first, then connect the switch target after the disconnect callback.
- Windows connection failures must log HID callback state, bond state, Bluetooth class, selected/known/candidate flags, and `connect(host)` acceptance.
- Manual and new-PC connection attempts time out if the HID connected callback does not arrive, then show Windows re-pairing guidance instead of staying in connecting state.
- If Windows was removed only on the PC side, the selected host can be removed from Android pairing in-app before running the new-PC flow again.

## Android Build Baseline

```text
minSdk: 28
targetSdk: 36
compileSdk: 36
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
- Double-tap drag is optional and defaults OFF; when enabled, the second tap-and-hold sends left-button-down until finger-up or a safety release.
- Scroll buttons expose slow/default/fast presets so host-specific speed differences can be tuned without reintroducing macOS over-acceleration by default.
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
