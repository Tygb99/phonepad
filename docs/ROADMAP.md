# Roadmap

> Korean version: [ko/ROADMAP.md](ko/ROADMAP.md)

## Purpose

Keep the implementation order and scope boundaries clear for a solo/small-team build.

## Current State

*Last reviewed: 2026-08-30. Latest commit on `main`: `fb4ad41` (2026-08-07).*

Phase 0 is complete and Phase 1 is largely complete. Active development has been paused since 2026-08-07.

**Done and verified on real hardware**

- Phase 0 passed. `BluetoothHidDevice` proxy, `registerApp`, Windows 11 and macOS pairing, cursor movement, clicks, scroll, Drag Mode reports, foreground service lifecycle, and reconnect were verified on a Galaxy S23 Ultra running Android 16. See [PHASE0_TEST_REPORT_2026-05-27.md](PHASE0_TEST_REPORT_2026-05-27.md), [ANDROID12_COMPAT_SMOKE_2026-05-31.md](ANDROID12_COMPAT_SMOKE_2026-05-31.md), [WINDOWS_REPAIRING_RESET_SMOKE_2026-06-02.md](WINDOWS_REPAIRING_RESET_SMOKE_2026-06-02.md).
- Most of Phase 1 shipped: host list filtered to paired computers, trackpad UI, movement, left/right click, continuous scroll buttons with slow/default/fast speed, quiet auto-reconnect to the last successful host, and foreground service scoping.
- Two items landed ahead of the plan: per-host language toggle via keyboard HID registration ([HOST_LANGUAGE_TOGGLE_SMOKE_2026-05-29.md](HOST_LANGUAGE_TOGGLE_SMOKE_2026-05-29.md)), and an iOS app that reaches the host through the Bridge Dongle instead of direct HID.
- Bridge Dongle is past PoC. Physical ESP32-S3 hardware exists, and the collaborator verified mouse plus English keyboard emulation working on 2026-07-06.
- License is settled: GPL-3.0-or-later.
- Repository layout is settled: one repository with `Android/`, `ios/`, `shared/`.

**Open and blocking**

- `codex/bridge-dongle-v2-status` is still unmerged. The collaborator verified it and agreed to merge on 2026-07-06, but issue #3 was filed against the same branch and is still open.
- Dongle firmware cannot be reflashed locally: the ESP32-S3 upload port does not appear on the maintainer's Mac, so v2 firmware and BIOS/UEFI behavior remain unverified by the maintainer.
- Korean text input over the dongle is unimplemented (issue #4). English keycodes work; Hangul does not.
- BIOS/UEFI operation is still unverified and must not be claimed.
- Phase 2 has not started: no three-finger gestures, no gesture mapping screen, and one beta tester rather than five.

## Current Rules

- Do not build v1.1 convenience features before the Direct HID path works.
- Keep Bridge Dongle as a parallel spike with a separate decision gate.
- Keep Play Store launch after GitHub-first validation.
- Keep accessibility-specific major work out of v1.0 unless it directly improves core reliability.

## Phase 0: Product And Tech Spike, Weeks 1-3

- Minimal Kotlin Android app.
- `BluetoothHidDevice` profile proxy.
- `registerApp` with minimal descriptor.
- Windows 11 pairing and cursor movement.
- Drag Mode report test.
- macOS pairing and core input.
- Foreground service and lifecycle release checks.
- Reconnect measurement.
- Bridge Dongle PoC in parallel if collaborator bandwidth exists.
- Go/No-Go decision.

## Phase 1: Direct HID Mouse Alpha, Weeks 4-6

- Stable mouse HID descriptor.
- Pairing and host list.
- Trackpad UI v0.
- Movement, left click, right click, scroll.
- Drag Toggle UI and safety release.
- Foreground notification.
- Connection and reconnect logs.

## Phase 2: Gesture Pad Beta, Weeks 7-9

- Keyboard composite report.
- Three-finger up/left/right swipe recognition.
- macOS and Windows presets.
- Gesture mapping screen v0.
- Sensitivity, scroll, haptic, Drag settings.
- Five beta testers.

## Phase 3: Release Candidate, Weeks 10-11

- UI polish.
- Diagnostics copy flow.
- Korean UX completion.
- English and Korean README coverage.
- GitHub Releases APK and reproducible CI.
- Demo GIF or video.

## Phase 4: v1.0 Release, Week 12

- Beta feedback fixes.
- Supported and unsupported device list.
- FAQ for iOS, BIOS/UEFI, and native touchpad limitations.
- Public GitHub release.
- Bridge Dongle spike appendix.

## v1.1+

- Full gesture mapping editor.
- JSON import/export.
- Four-finger gestures.
- Pinch zoom.
- Additional OS presets.
- Play Store release.
- F-Droid evaluation.
- Optional diagnostics sharing.

## Related Docs

- [QUICK_REF.md](QUICK_REF.md)
- [TEST.md](TEST.md)
- [DEPLOY.md](DEPLOY.md)
- [BRIDGE_DONGLE.md](BRIDGE_DONGLE.md)
