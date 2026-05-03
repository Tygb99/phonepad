# Drag Mode

> Korean version: [ko/DRAG_MODE.md](ko/DRAG_MODE.md)

## Purpose

Specify the explicit Drag Mode toggle so drag behavior is reliable on a phone touch surface and never leaves the host in a stuck mouse-button state.

## Current State

Long-press drag is not the default. PhonePad uses a visible Drag toggle that sends left-button-down on enable and left-button-up on disable.

## Current Rules

- Drag Mode state is runtime-only and must not be persisted to SQLite.
- App startup always begins with Drag Mode OFF.
- Drag Mode ON disables one-finger tap-to-click and three-finger gesture recognition.
- Every safety event attempts all-buttons-up before changing host, unregistering, or exiting.
- Stuck left button count must be zero in release tests.

## State Machine

```text
off
  -> enabling
  -> on
  -> release_pending
  -> off

any state
  -> error
  -> off after user recovery or reconnect
```

## User Flow

| Step | User Action | System Action |
|---|---|---|
| 1 | Tap `Drag` | Set state to `enabling`, send left-button-down. |
| 2 | Move one finger | Send movement reports with left-button bit set. |
| 3 | Tap `Dragging` | Set state to `release_pending`, send all-buttons-up. |
| 4 | Release succeeds | Set state to `off`. |
| 5 | Release may have failed | Show warning and recovery guidance. |

## Safety Events

Always call `releaseAllMouseButtons()` for:

- Host switch.
- Bluetooth disconnect.
- HID unregister.
- App background if connection cannot be maintained.
- Screen lock.
- Process shutdown hook where Android allows it.
- Foreground service stop.

## UI States

| State | Visual | Haptic | Input Handling |
|---|---|---|---|
| OFF | Outline `Drag` control | None | Normal trackpad input. |
| ON | Filled `Dragging` control and small top banner | Short double pulse | Movement holds left button. |
| Release pending | Disabled control with progress indicator | None | Ignore new drag actions. |
| Error | Red warning state | Strong pulse | Guide user to reconnect or manually click/press Escape. |

## Acceptance Criteria

1. ON sends one left-button-down report.
2. Movement while ON preserves left-button bit.
3. OFF sends at least one all-buttons-up report.
4. Safety events send all-buttons-up.
5. Reopening the app never restores Drag Mode ON.
6. Repeated ON/OFF cycles pass at least 30 iterations per test device/host pair.

## Related Docs

- [HID.md](HID.md)
- [TEST.md](TEST.md)
- [DESIGN.md](DESIGN.md)
- [API.md](API.md)
