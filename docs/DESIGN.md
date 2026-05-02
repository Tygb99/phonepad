# Design

## Purpose

Define the user-facing screens, interaction states, and copy rules for a Korean-first trackpad utility.

## Current State

PhonePad should feel like a quiet tool, not a marketing app. The first usable experience is pairing and trackpad control.

## Current Rules

- Dark mode first, light mode supported.
- Use Material 3 components unless a custom trackpad surface needs special handling.
- Keep the trackpad main screen dense, stable, and hard to mis-tap.
- Use icons for common tool actions and text labels for critical safety actions like Drag Mode.
- Do not write copy that overpromises native trackpad behavior.
- Avoid onboarding walls. Explain only at the moment a permission or pairing step is needed.

## Screens

| Screen | Purpose | v1.0 |
|---|---|---|
| First Run / Compatibility | Check Android, Bluetooth, HID support. | Yes |
| Permission Guide | Explain Nearby devices and notification timing. | Yes |
| Pairing | Guide host OS Bluetooth pairing. | Yes |
| Host List | Manage paired hosts and OS presets. | Yes |
| Trackpad Main | Full-screen input surface, connection status, Drag toggle. | Yes |
| Settings | Sensitivity, scroll, haptic, screen awake, toggle position. | Yes |
| Gesture Mapping | Edit three representative gestures. | Yes |
| Diagnostics | Copy compatibility and connection logs. | v1.1 |
| About / Open Source | GitHub, license, donation links. | Yes |

## Trackpad Main Layout

```text
Connected: MacBook Pro      status dot
Dragging banner if active

[ full-height touch surface ]

Host      Settings      Drag / Dragging
```

## Core Components

| Component | Rule |
|---|---|
| Connection status | Always visible on the main screen. |
| Drag toggle | Large enough for reliable edge placement, supports left/right bottom position. |
| Host switcher | Releases buttons before switching. |
| Gesture row | Shows gesture name, target OS, and mapped shortcut. |
| Permission card | Only appears when an action is blocked. |
| Warning banner | Used for possible button release failure or unsupported HID profile. |

## Interaction States

- Connected, reconnecting, disconnected, unsupported, permission missing.
- Drag OFF, Drag ON, release pending, release error.
- Pairing ready, pairing in progress, pairing failed, paired.
- Gesture enabled, gesture disabled, shortcut invalid.

## Copy Rules

Use:

- "표준 Bluetooth 입력장치처럼 사용"
- "OS별 단축키 기반 제스처"
- "Drag Mode를 켜고 끄며 드래그"
- "이 기기에서는 Android HID Device를 사용할 수 없습니다"

Avoid:

- "모든 PC에서 작동"
- "진짜 Magic Trackpad"
- "Precision Touchpad"
- "BIOS에서도 사용 가능"
- "원클릭 연결"

## Related Docs

- [DRAG_MODE.md](DRAG_MODE.md)
- [MOBILE_APP.md](MOBILE_APP.md)
- [SECURITY.md](SECURITY.md)
- [TEST.md](TEST.md)
