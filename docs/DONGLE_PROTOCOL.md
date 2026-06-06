# Dongle Protocol

> Korean version: [ko/DONGLE_PROTOCOL.md](ko/DONGLE_PROTOCOL.md)

## Purpose

Define the BLE packet contract for the optional PhonePad Bridge Dongle path:

```text
PhonePad app -> BLE GATT write -> ESP32-S3 dongle -> USB HID mouse/keyboard
```

This protocol does not replace the Android Direct HID MVP. It supports the iOS sender and hardware spike path.

## BLE GATT

Service UUID:

```text
7c2d2b6a-8f3e-4c6f-8d6f-01b0f4dd1000
```

Writable characteristic UUID:

```text
7c2d2b6a-8f3e-4c6f-8d6f-01b0f4dd1001
```

Mouse movement may use `writeWithoutResponse`. Keyboard, chord, and release/safety commands should prefer `writeWithResponse` when the characteristic supports it.

## v1 Mouse Packet

The original mouse packet remains exactly 4 bytes for compatibility:

```text
byte 0: dx, int8
byte 1: dy, int8
byte 2: buttons bitmask, bit 0 left, bit 1 right, bit 2 middle
byte 3: wheel, int8
```

Any non-mouse command must avoid exactly 4 bytes so firmware can continue treating 4-byte values as mouse input.

## v1 Keyboard Key Packet

Single-key keyboard packets use the same writable characteristic:

```text
byte 0: 0x03
byte 1: action, 0 tap, 1 press, 2 release
byte 2: Arduino USBHIDKeyboard key code
```

The iOS app pads this packet if needed so it is never exactly 4 bytes.

## v2 Safety And Chord Packets

### ReleaseAll

```text
byte 0: 0x10
byte 1: sequence id
```

Firmware behavior:

- Release left, right, and middle mouse buttons.
- Call keyboard `releaseAll()`.
- Use this on BLE disconnect, app background, manual disconnect, host switch, and chord failure recovery.

### KeyChord

```text
byte 0: 0x11
byte 1: sequence id
byte 2: modifier mask
byte 3: key count, max 6
byte 4...: Arduino USBHIDKeyboard key code list
```

Modifier mask:

| Bit | Modifier |
|---|---|
| `0x01` | Left Control |
| `0x02` | Left Shift |
| `0x04` | Left Alt |
| `0x08` | Left GUI |
| `0x10` | Right Control |
| `0x20` | Right Shift |
| `0x40` | Right Alt |
| `0x80` | Right GUI |

Firmware behavior:

1. Press modifiers.
2. Tap each listed key.
3. Release modifiers.
4. Call keyboard `releaseAll()` as a final safety cleanup.

### LanguageToggle

```text
byte 0: 0x12
byte 1: sequence id
byte 2: host profile
```

Host profiles:

| Value | Host profile | Sent behavior |
|---|---|---|
| `0x01` | Mac | `Control + Space` |
| `0x02` | Windows LANG1 | `LANG1` key code `0x90` |
| `0x03` | Windows Right Alt | Right Alt key code `0x86` |

Mac `Control + Space` is the known local baseline for PhonePad's host-language smoke test. Windows behavior still needs per-host validation because Korean IME settings vary.

## Validation Checklist

- iPhone scans and connects to the ESP32-S3 service.
- Pointer movement keeps working with the v1 4-byte packet.
- Click, right click, middle click, and scroll still work.
- `ReleaseAll` clears all mouse buttons and keyboard keys.
- App background or manual disconnect sends `ReleaseAll` before BLE cancellation when possible.
- Mac profile toggles input source with `Control + Space`.
- Windows profiles test both `LANG1` and `RightAlt`.
- BLE disconnect on the dongle calls the same release cleanup.
