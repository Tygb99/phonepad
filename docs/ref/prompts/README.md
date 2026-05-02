# AI Coding Prompts

## Purpose

Provide reusable prompts for AI coding tools so implementation stays aligned with PhonePad constraints.

## Current State

Use these prompts as starting points. Keep generated code small, tested, and tied to real Android hardware evidence.

## Current Rules

- Ask for implementation plus verification steps.
- Require no `INTERNET` permission.
- Require Drag Mode safety release checks for input changes.
- Require docs updates when behavior changes.
- Do not include private PRD drafts or chat logs in prompts.

## Phase 0 Kotlin HID Spike Prompt

```text
Implement a minimal Android Kotlin spike for PhonePad that checks Android API level,
gets BluetoothHidDevice through getProfileProxy, registers a minimal mouse HID app,
and exposes a test action that sends cursor movement to the paired host.

Constraints:
- minSdk 28
- no INTERNET permission
- document every Bluetooth permission used
- log device model, Android version, registration result, and sendReport result locally
- add a short test checklist for Windows 11 and macOS
```

## Drag Mode Review Prompt

```text
Review this PhonePad change for Drag Mode safety.

Check:
- Drag Mode ON sends left-button-down once
- movement while ON keeps the left-button bit
- OFF sends all-buttons-up
- disconnect, host switch, unregister, app background, screen lock, and service stop call releaseAllMouseButtons
- runtime Drag Mode state is not persisted
- tests cover repeated ON/OFF and safety events
```

## Manifest Review Prompt

```text
Review the Android manifest and Gradle files for PhonePad.

Must confirm:
- no INTERNET permission
- Bluetooth permissions are the minimum needed for targetSdk 35
- BLUETOOTH_SCAN is absent unless documented by a failing spike
- foreground service type is appropriate for connected device behavior
- notification permission is requested at runtime, not as unexplained onboarding
```

## Related Docs

- [../../HID.md](../../HID.md)
- [../../DRAG_MODE.md](../../DRAG_MODE.md)
- [../../MOBILE_APP.md](../../MOBILE_APP.md)
