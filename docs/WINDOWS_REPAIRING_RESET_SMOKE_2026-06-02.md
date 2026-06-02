# Windows Re-Pairing Reset Smoke Test - 2026-06-02

> Korean version: [ko/WINDOWS_REPAIRING_RESET_SMOKE_2026-06-02.md](ko/WINDOWS_REPAIRING_RESET_SMOKE_2026-06-02.md)

## Purpose

This report records a Windows connection failure that recovered after resetting both sides of the Bluetooth pairing.

## Result

Pass with stale-pairing root cause. The first manual Windows host switch reached `CONNECTING`, then fell back to `DISCONNECTED` while the Android bond state changed from `BOND_BONDED` to `BOND_NONE`. After deleting the Windows device record and using PhonePad's `Android 페어링 삭제` plus `새 PC 연결` flow, the same Windows host connected successfully.

## Test Environment

| Item | Value |
|---|---|
| Date | 2026-06-02 |
| Source baseline | `22a582c Add Windows development setup` |
| Installed app | `0.2.0-phase0`, `versionCode=18` |
| Android device | Samsung Galaxy S23 Ultra, `SM-S918N` |
| Android version | Android 16 / API 36 |
| Windows host label | `TYGB` |
| ADB transport | Wireless ADB, device serial redacted |

## Observed Log Sequence

Bluetooth addresses are intentionally omitted from this report.

| Time | Event | Interpretation |
|---|---|---|
| 10:25:48 | `defer_switch_manual_switch` from the connected Mac to `TYGB` | PhonePad released the current host before switching. |
| 10:25:48 | `connect_request_manual_switch` for `TYGB`, bond `12`, HID state `연결 안 됨` | `BluetoothHidDevice.connect(host)` was attempted against the Windows host. |
| 10:25:48 | `callback_state_연결 중` for `TYGB` | The HID profile accepted the connection attempt and entered connecting state. |
| 10:25:57 | `callback_state_연결 안 됨` for `TYGB`, bond `10` | Windows/Android pairing state was no longer valid; the host disconnected before a successful HID connection. |
| 10:26:04 | `remove_bond_request` for `TYGB` | User invoked PhonePad's Android-side pairing removal. |
| 10:26:13 | `hid_foreground_service=start reason=discoverable_request` | User started the new-PC pairing flow. |
| 10:26:51 | `callback_state_연결됨` for `TYGB`, bond `12` | Re-pairing completed and the Windows HID host connected successfully. |

## Confirmed Recovery Procedure

1. Delete PhonePad from Windows Bluetooth devices.
2. In PhonePad, select the stale Windows host candidate.
3. Tap `Android 페어링 삭제`.
4. Tap `새 PC 연결`.
5. In Windows Bluetooth device add flow, pair the newly advertised `PhonePad - {device name}` entry.
6. Return to PhonePad and tap `연결!`.

## Interpretation

- This was not a source-code regression from the latest remote change; the latest fetched commit only added Windows development docs and scripts.
- The log did not show a direct `connect request rejected` path. It showed an accepted connection attempt followed by a disconnect while the bond state dropped to `BOND_NONE`.
- The successful recovery after full re-pairing points to a stale Windows/Android pairing record, likely from an older HID descriptor or host-side Bluetooth cache.
- Keeping `PhonePad - {device name}` in the new-PC flow remains useful because it distinguishes fresh pairing entries from stale host records.

## Follow-Up

- Keep the Windows re-pairing reset flow in the standard troubleshooting guide.
- If this recurs frequently, add clearer in-app guidance when a Windows host disconnects with bond state `10` immediately after `CONNECTING`.
- Host-side cursor movement after the restored connection was user-confirmed, but detailed click/scroll/drag metrics were not part of this log capture.

## Related Docs

- [HID.md](HID.md)
- [TEST.md](TEST.md)
- [MOBILE_APP.md](MOBILE_APP.md)
