# Test Plan

> Korean version: [ko/TEST.md](ko/TEST.md)

## Purpose

Define the compatibility, input, lifecycle, Drag Mode, and release tests needed before PhonePad v1.0 can be trusted.

## Current State

Testing begins with Phase 0 spikes because Android Bluetooth HID support varies by device and manufacturer.

## Current Rules

- Test on real Android hardware, not only emulators.
- Record device model, Android version, host OS version, Bluetooth adapter, and app commit.
- Every Drag Mode failure is P0 until proven harmless.
- Reconnection metrics must use repeated runs, not one-off success.
- Release artifacts require a clean manifest check for no `INTERNET`.

## Direct HID Matrix

| Area | Minimum Target | Required Checks |
|---|---|---|
| Android | Pixel 1 device | HID profile, register, sendReport. |
| Android | Samsung Galaxy 1 device | Same as above. |
| Android | One lower-cost/other vendor device | Success or clear unsupported UX. |
| Windows | Windows 11 latest | Pairing, move, click, scroll, drag, shortcuts. |
| Windows | Windows 10 22H2 | Best-effort notes. |
| macOS | Current stable | Pairing, move, click, scroll, drag, shortcuts. |
| Bluetooth | Built-in plus one USB adapter | Stability notes. |
| Lifecycle | Screen off, background, foreground, process kill | Release and reconnect. |

## Drag Mode Protocol

Repeat at least 30 times for each Android/host pair:

1. Connect to host.
2. Turn Drag Mode ON.
3. Move for three seconds.
4. Confirm file/window/text selection drag remains held.
5. Turn Drag Mode OFF.
6. Confirm no stuck left-button state.
7. Repeat safety events: disconnect, host switch, app background, screen off.

Pass criteria:

- ON/OFF success rate at least 95 percent.
- Stuck left button: zero.
- Safety event stuck state: zero.
- Drag ON visual state mismatch: zero.

## Reconnect Protocol

Repeat 20 times per Android/host pair:

1. Connect.
2. Turn screen off.
3. Wait 30 seconds.
4. Return to foreground.
5. Record reconnect success and time.

Pass criteria:

- Success rate at least 90 percent.
- Successful P95 reconnect time within 10 seconds.

## Phase 0 Feedback Checks

- New-PC flow: after a newly bonded host returns to the app, it becomes the switch target and connection is attempted without an extra manual tap.
- Multi-pairing: previous/next changes only the switch target; actual switching requires `호스트 연결/전환`.
- Windows re-pair reset: delete PhonePad on Windows, select the stale Windows host in the app, use `Android 페어링 삭제`, then run `새 PC 연결` and pair again.
- Double-tap drag: verify default OFF, option persistence, tap-tap-hold movement, and all-buttons-up on finger-up/cancel.
- Scroll speed: verify slow/default/fast presets and confirm the default still avoids macOS hold-scroll over-acceleration.
- Windows failure logging: when Windows stays connecting or disconnects, capture `PhonePad` logcat `host_diag`, `defer_switch_*`, `connect_after_switch_*`, `connect_request_*`, and `connect_timeout_*` lines.

## Release Gate Checklist

- `INTERNET` absent from manifest.
- Required permissions documented.
- GitHub Actions produces release APK.
- README lists tested and unsupported devices.
- Drag Mode tests pass.
- Windows 11 and macOS pass core input tests.
- Known limitations documented.

## Related Docs

- [HID.md](HID.md)
- [DRAG_MODE.md](DRAG_MODE.md)
- [MOBILE_APP.md](MOBILE_APP.md)
- [DEPLOY.md](DEPLOY.md)
