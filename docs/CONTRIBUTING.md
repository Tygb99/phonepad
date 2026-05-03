# Contributing

> Korean version: [ko/CONTRIBUTING.md](ko/CONTRIBUTING.md)

## Purpose

Set collaboration rules for early PhonePad work, especially when product, Android HID, and optional dongle work happen in parallel.

## Current State

The project is at the start of GitHub collaboration. Early contributions should focus on proving the input path and recording evidence.

## Current Rules

- Open small PRs with one main purpose.
- Include device and host OS evidence for Bluetooth/HID changes.
- Do not commit local PRD drafts, chat exports, secrets, or generated private planning notes.
- Do not add desktop helper apps to v1.0 scope.
- Keep dongle work behind a separate spike boundary.

## Good First Work Items

- Minimal Kotlin HID registration spike.
- Mouse movement report proof.
- Windows 11 pairing notes.
- macOS pairing notes.
- Drag Mode ON/OFF report test.
- Android permission minimum test.
- README support matrix template.

## PR Checklist

- What device and host OS were tested?
- Does this change affect Drag Mode release behavior?
- Does the manifest still avoid `INTERNET`?
- Are permissions documented?
- Are new logs local-only and free of raw input history?
- Are docs updated when behavior or scope changes?

## Issue Labels

| Label | Meaning |
|---|---|
| `hid` | Bluetooth HID registration, descriptors, reports. |
| `drag-mode` | Drag toggle, safety release, stuck-button risk. |
| `android` | Android lifecycle, service, permissions. |
| `windows` | Windows host behavior. |
| `macos` | macOS host behavior. |
| `dongle-spike` | Bridge Dongle research. |
| `docs` | Documentation only. |

## Related Docs

- [ARCHITECTURE.md](ARCHITECTURE.md)
- [HID.md](HID.md)
- [DRAG_MODE.md](DRAG_MODE.md)
- [BRIDGE_DONGLE.md](BRIDGE_DONGLE.md)
