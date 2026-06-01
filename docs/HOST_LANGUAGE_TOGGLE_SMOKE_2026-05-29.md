# Host Language Toggle Smoke Test - 2026-05-29

> Korean version: [ko/HOST_LANGUAGE_TOGGLE_SMOKE_2026-05-29.md](ko/HOST_LANGUAGE_TOGGLE_SMOKE_2026-05-29.md)

## Purpose

This report records the successful macOS Korean/English language-toggle smoke test for PhonePad's host-aware keyboard HID path.

## Result

Pass. After refreshing the macOS Bluetooth pairing so the host recognized PhonePad as a mouse + keyboard HID device, the PhonePad `한영` button toggled the Mac input source repeatedly:

```text
ABC -> 2-Set Korean -> ABC -> 2-Set Korean
```

## Test Environment

| Item | Value |
|---|---|
| Test date | 2026-05-29 |
| Documented | 2026-06-02 |
| Implementation commit | `7e77f4f fix: send mac language toggle as staged chord` |
| Included release | `v0.2.0-phase0` |
| Android device | Samsung Galaxy S23 Ultra, `SM-S918N` |
| Android version | Android 16 / API 36 |
| Host | `yong의 Mac mini` |
| Host OS baseline | macOS 26.5 |
| PhonePad host preset | `Mac: Control + Space` |

## Checks

| Check | Result | Evidence |
|---|---|---|
| macOS local shortcut baseline | Pass | Local `Control + Space` toggled the Mac input source. |
| PhonePad keyboard report path | Pass | PhonePad emitted the Mac language-toggle stroke as `language_toggle preset=mac`. |
| Staged chord behavior | Pass | Mac preset sends Control first, then Control + Space, then releases keyboard keys. |
| Fresh pairing after descriptor change | Required | Existing macOS pairing could still treat PhonePad as mouse-only. Removing PhonePad from macOS Bluetooth and pairing again refreshed the keyboard collection. |
| Host-side result | Pass | The visible macOS input source changed `ABC -> 2-Set Korean -> ABC -> 2-Set Korean`. |

## Final Behavior

- macOS uses a staged `Control + Space` chord instead of a single simultaneous report.
- The sequence is modifier-down, key-down with modifier, then all-key-up.
- If the Mac does not react even though PhonePad logs show the send, remove PhonePad from macOS Bluetooth and pair again through the app's new-PC flow.
- The fallback guidance is important because old pairings made before the keyboard HID descriptor was added can remain mouse-only on the host side.

## Scope Boundary

- This report covers the macOS language-toggle success path.
- Windows language-toggle policy remains: `Keyboard LANG1` by default, with `Right Alt` as a per-host fallback.
- Windows host-side language-toggle success should be recorded in a separate Windows report when verified.

## Related Docs

- [HID.md](HID.md)
- [TEST.md](TEST.md)
- [MOBILE_APP.md](MOBILE_APP.md)
