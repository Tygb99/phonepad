# CLAUDE.md

## Purpose

This file gives AI coding agents the current product rules for PhonePad so they can implement without reopening private planning sources.

## Current State

- Product: Android phone as a Bluetooth HID trackpad-style input device for Windows/macOS.
- Main path: Android Direct `BluetoothHidDevice`.
- Plan B: Bridge Dongle spike, documented separately.
- Current repo phase: documentation scaffold plus Phase 0 implementation planning.
- Git hygiene: PRD drafts and KakaoTalk exports are local planning sources and must not be committed.

## Current Rules

- Keep v1.0 serverless on the desktop side. Do not add a Windows/macOS helper app.
- Do not request Android internet permission.
- Do not add account, ads, analytics, or tracking SDKs.
- Treat Drag Mode safety release as a P0 behavior.
- Do not claim native OS touchpad behavior. Use "Bluetooth HID mouse + keyboard shortcut mapping".
- Prefer Kotlin proof-of-concept for Phase 0 before broad Flutter UI work.
- Record Android device, host OS, Bluetooth transport/profile observations, and failure modes in docs.
- Preserve Korean-first UX copy. Use English where it helps open-source contributors.

## Implementation Priority

1. Minimal Kotlin Android app that obtains `BluetoothHidDevice`.
2. Register mouse-only HID app and send movement reports.
3. Pair and test Windows 11, then macOS.
4. Add click, scroll, Drag Mode ON/OFF reports, and safe release hooks.
5. Attach Flutter UI through MethodChannel after the native path is proven.

## Hard Acceptance Gates

- At least two Android devices register HID successfully or show a clear unsupported-device UX.
- Windows 11 and macOS pair without desktop server software.
- Drag Mode OFF and lifecycle safety events leave no stuck left-button state.
- Manifest contains no `INTERNET` permission.
- CI can produce a reproducible GitHub Releases APK.

## Related Docs

- [docs/INDEX.md](docs/INDEX.md)
- [docs/HID.md](docs/HID.md)
- [docs/DRAG_MODE.md](docs/DRAG_MODE.md)
- [docs/TEST.md](docs/TEST.md)
- [docs/ref/prompts/README.md](docs/ref/prompts/README.md)
