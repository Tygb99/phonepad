# Bridge Dongle

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
