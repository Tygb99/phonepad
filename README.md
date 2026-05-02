# PhonePad

PhonePad is an Android-first input app that turns a phone screen into a Bluetooth trackpad-style controller for Windows and macOS.

The v1.0 path is Android Direct Bluetooth HID Device mode: the host computer should see the phone as a standard Bluetooth mouse/keyboard composite device, without installing a desktop server app.

## Product Scope

- Android API 28+ compatibility check for `BluetoothHidDevice`.
- Pairing with Windows 11 and current macOS as a standard Bluetooth HID input device.
- Cursor movement, left click, right click, vertical and horizontal scroll.
- Explicit Drag Mode toggle instead of long-press drag.
- Three core three-finger gestures mapped to OS-specific keyboard shortcuts.
- No account, no ads, no tracking SDK, no internet permission.
- Korean-first app UI with English README and public GitHub release artifacts.
- Bridge Dongle is a Phase 0 spike, not the v1.0 default path.

## Non-Goals

- iOS Direct Bluetooth HID sender mode.
- Desktop helper/server app.
- Screen mirroring, file transfer, clipboard sync, or remote desktop.
- Native Precision Touchpad or Magic Trackpad emulation in v1.0.
- BIOS/UEFI support in Direct Bluetooth mode.

## Documentation

Start with:

- [docs/INDEX.md](docs/INDEX.md)
- [docs/QUICK_REF.md](docs/QUICK_REF.md)
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- [docs/HID.md](docs/HID.md)
- [docs/DRAG_MODE.md](docs/DRAG_MODE.md)
- [docs/TEST.md](docs/TEST.md)

Planning sources such as PRD drafts and exported chat logs are intentionally ignored by git. Keep product docs in `docs/` and source code in the normal Flutter/Android project tree.

## Repository

Remote target: https://github.com/Tygb99/phonepad.git

The repository is expected to stay documentation-first until the Phase 0 Android HID spike proves the core path.
