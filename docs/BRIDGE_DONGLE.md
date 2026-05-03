# Bridge Dongle

> Korean version: [ko/BRIDGE_DONGLE.md](ko/BRIDGE_DONGLE.md)

## Purpose

Track the optional hardware Plan B: phone to BLE dongle to USB HID host input.

## Current State

Bridge Dongle is not required for v1.0 app-only MVP. It is a Phase 0 parallel spike that may unlock BIOS/UEFI, iOS sender, or more reliable host compatibility later.

## Current Rules

- Keep dongle work separate from Direct HID release scope.
- Do not block app-only MVP on hardware.
- Treat firmware, BLE custom protocol, and enclosure/production as post-spike work.
- Do not claim BIOS or iOS support until the spike proves it.

## Candidate Architecture

```text
Phone App
  Touch UI, Drag Mode, gesture mapping
      |
      | BLE custom packets
      v
Bridge Dongle
  BLE receiver, USB HID mouse/keyboard firmware
      |
      | USB HID
      v
Windows / macOS / possible BIOS or UEFI
```

## Prior Art: ESPRemoteControl

Reference links:

- Reddit discussion: [Turn your iPhone into a wireless BLE keyboard + trackpad for any device with a USB port](https://www.reddit.com/r/esp32/comments/1qmqez2/turn_your_iphone_into_a_wireless_ble_keyboard/?tl=ko)
- GitHub repo: [KoStard/ESPRemoteControl](https://github.com/KoStard/ESPRemoteControl)

Observed architecture:

```text
iPhone SwiftUI app
  Keyboard input, trackpad area, gestures
      |
      | Custom Bluetooth LE command protocol
      v
ESP32-S3 firmware
  BLE receiver, command translation
      |
      | TinyUSB USB HID
      v
Target device
  Standard USB keyboard and mouse
```

Why it matters for PhonePad:

- It validates the phone -> BLE -> ESP32-S3 -> USB HID bridge pattern.
- It uses ESP32-S3 native USB, TinyUSB, and NimBLE, which are useful starting points for the spike.
- It shows the same broad input categories PhonePad cares about: keyboard input, cursor movement, left click, right click, scroll, function keys, and key combos.
- It supports the product assumption that USB HID can cover devices where app-only Bluetooth HID is not enough.
- It is better treated as prior art and spike reference than a drop-in dependency: the repo appears early-stage, has no release artifacts, and is iOS-first.

PhonePad-specific follow-up checks:

- Confirm whether the custom BLE command format is enough for Drag Mode button-hold semantics.
- Add an explicit `release_all_buttons` control command if the reference protocol does not already guarantee safe release.
- Measure BLE-to-USB HID latency and report-rate stability with PhonePad-like pointer movement.
- Check whether Android can use the same BLE peripheral/central role cleanly, depending on the final dongle direction.
- Record board, firmware stack, host OS, and USB HID descriptor details before claiming BIOS/UEFI or iOS support.

## Spike Success Criteria

| Area | Success Criteria |
|---|---|
| Board selection | One MCU board chosen and documented. |
| USB HID sample | Windows/macOS recognize mouse/keyboard without drivers. |
| BLE link | Phone to dongle stable at 5 meters. |
| Mouse move | Phone input moves host cursor. |
| Report rate | 60Hz report path is feasible. |
| Latency | P95 under 30ms in a simple test. |
| BIOS/UEFI | At least one PC recognizes USB mouse if this is claimed. |
| iOS possibility | Separate note on iPhone BLE packet feasibility. |

## Packet Draft

See [API.md](API.md) for the current MouseMove, KeyCombo, and Control packet draft.

## Decision Gate

Proceed beyond spike only if at least one of these is true:

- Direct Android HID fails on too many target devices.
- BIOS/UEFI use becomes a clear product requirement.
- iOS sender mode becomes important enough to justify hardware.
- Collaborator firmware capacity is available without slowing v1.0.

## Related Docs

- [API.md](API.md)
- [HID.md](HID.md)
- [ROADMAP.md](ROADMAP.md)
- [TEST.md](TEST.md)
