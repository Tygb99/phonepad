# Roadmap

> Korean version: [ko/ROADMAP.md](ko/ROADMAP.md)

## Purpose

Keep the implementation order and scope boundaries clear for a solo/small-team build.

## Current State

PhonePad is documentation-ready and should start with Phase 0 technical spikes before full product implementation.

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
