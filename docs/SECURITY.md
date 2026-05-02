# Security And Privacy

## Purpose

Document the trust model for an app that handles user input and presents itself as a local Bluetooth device.

## Current State

PhonePad has no account, cloud sync, advertising, analytics, or internet permission in v1.0.

## Current Rules

- User input must not leave the phone except as Bluetooth HID reports to the paired host.
- Do not collect telemetry.
- Do not add crash reporting unless it is opt-in, anonymous, and documented after v1.0.
- Keep local logs copyable by the user, not automatically uploaded.
- Keep source public and builds reproducible.

## Data Handling

| Data | Storage | Notes |
|---|---|---|
| Paired host display name/address | Local SQLite | Needed for reconnect and host list. |
| OS preset | Local SQLite | Needed for shortcut mapping. |
| Gesture mappings | Local SQLite | User preference. |
| Settings | Local SQLite | User preference. |
| Pointer movement history | Not stored | Processed into HID reports only. |
| Diagnostics logs | Local only | User can copy/share manually. |

## Permission Posture

- `INTERNET` is prohibited in v1.0.
- Bluetooth permissions must be explained in user-facing Korean.
- Notification permission must be justified by foreground connection status.
- If `BLUETOOTH_SCAN` is required, document why and avoid location-facing language where Android allows.

## Open Source Trust Signals

- Public code.
- No network permission.
- No tracking SDKs.
- Reproducible GitHub Actions release build.
- README support matrix listing tested and unsupported devices.

## Threats And Mitigations

| Risk | Mitigation |
|---|---|
| Overbroad permissions reduce trust. | Keep manifest minimal and document each permission. |
| Stuck mouse button disrupts host. | Safety release hooks and repeated Drag Mode tests. |
| Host spoofing or wrong target selection. | Clear active host UI and release before switching. |
| Logs leak private data. | Do not log raw input streams or typed content. |
| Supply-chain SDK risk. | Avoid ads, analytics, and unnecessary third-party SDKs. |

## Related Docs

- [DB.md](DB.md)
- [HID.md](HID.md)
- [MOBILE_APP.md](MOBILE_APP.md)
- [DEPLOY.md](DEPLOY.md)
