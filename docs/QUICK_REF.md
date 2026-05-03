# Quick Reference

> Korean version: [ko/QUICK_REF.md](ko/QUICK_REF.md)

## Purpose

Keep the product contract visible for fast implementation decisions.

## Current State

PhonePad v1.0 is an Android Direct Bluetooth HID app for Windows/macOS. It is not a remote desktop product and does not require a desktop server app.

## Current Rules

- Default path: Android `BluetoothHidDevice`.
- Minimum Android: API 28.
- Target host OS: Windows 11 latest and current macOS.
- Host identity: standard Bluetooth HID mouse/keyboard composite device.
- Primary input surface: full-screen trackpad UI.
- Drag behavior: explicit toggle, not long press.
- Network posture: no internet permission.
- Business model: free, open source, optional donation.

## Must Ship In v1.0

- Compatibility check and unsupported-device explanation.
- Pairing flow and host list, up to five saved hosts with one active host.
- Cursor movement, left click, right click, scroll.
- Drag Mode toggle with safe release on disconnect, app exit, screen lock, unregister, and host switch.
- Three three-finger swipes mapped to macOS/Windows shortcuts.
- Settings for sensitivity, natural scroll, haptics, screen awake, Drag toggle position, OS preset.
- Korean UI and English README.
- GitHub Releases APK built by CI.
- Bridge Dongle spike result documented.

## Do Not Promise

- "Works on every Android."
- "Native Magic Trackpad" or "Precision Touchpad".
- "BIOS/UEFI support" for Direct Bluetooth mode.
- iPhone as a Direct Bluetooth HID mouse.
- One-click setup if Bluetooth pairing still needs OS steps.

## Success Metrics

- Two or more Android devices pass HID registration and input tests.
- Windows 11 and macOS pair without helper apps.
- Reconnect success rate is at least 90 percent, P95 reconnect time within 10 seconds.
- Drag Mode stuck-left-button incidents: zero in the defined test matrix.
- Manifest contains no `INTERNET` permission.

## Related Docs

- [HID.md](HID.md)
- [DRAG_MODE.md](DRAG_MODE.md)
- [TEST.md](TEST.md)
- [ROADMAP.md](ROADMAP.md)
