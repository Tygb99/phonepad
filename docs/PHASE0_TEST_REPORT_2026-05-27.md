# Phase 0 Test Report - 2026-05-27

> Korean version: [ko/PHASE0_TEST_REPORT_2026-05-27.md](ko/PHASE0_TEST_REPORT_2026-05-27.md)

## Summary

This report records the ADB-driven Phase 0 verification run performed on 2026-05-27.

Result: the tested Galaxy S23 Ultra and paired macOS host passed the ADB-verifiable Drag Mode, explicit reconnect, and lifecycle recovery checks. Host-side cursor movement and real file/window/text dragging still require human observation on the PC because ADB cannot inspect the host desktop.

## Environment

| Field | Value |
|---|---|
| Android device | Samsung Galaxy S23 Ultra, `SM-S918N` |
| Android version | Android 16, API 36 |
| App package | `com.tygb99.phonepad` |
| App version | `0.1.16-phase0`, `versionCode=17` |
| App last update | `2026-05-07 22:20:48` |
| Repo branch | `main` |
| Repo commit | `c253b11` (`c253b119e1eb1c8dd17f58d2593566a293bd2a4b`) |
| Paired host label | `yong의 Mac mini` |
| ADB transport | device serial redacteddevice serial redacted`device serial redacted` |

## Scope

ADB verified:

- PhonePad foreground launch and UI state through `uiautomator`.
- HID connection state through UI text and `PhonePad` logcat lines.
- Button and trackpad gestures through `adb shell input`.
- Drag Mode final state and in-app send counters.
- Reconnect timing from local command timing and `callback_state_연결됨` logcat evidence.
- Background, screen-off, and process-stop recovery.

ADB did not verify:

- Actual host cursor position.
- Real host-side file, window, or text-selection drag.
- Host desktop stuck-button state outside the Android-side release and UI/log evidence.

## Results

| Scenario | Method | Result | Evidence |
|---|---|---|---|
| Initial app recovery | `am force-stop`, app relaunch, wait for HID reconnect | Pass | UI changed from HID registration rejected to `호스트와 연결됐습니다: yong의 Mac mini`; Drag button enabled. |
| Drag Mode 30-cycle repeat | Tap `Drag`, short trackpad swipe, tap `Dragging` off, repeated 30 times | Pass | `전송 성공: 296`, `전송 실패: 0`, final button text `Drag`, last report `release_all reason=drag_off`. |
| Explicit reconnect 20-cycle repeat | Tap `연결 해제`, wait for disconnected callback, tap `호스트 연결/전환`, wait for connected callback | Pass | `20/20` successful reconnects, `0` failures. |
| Background recovery with Drag ON | Turn Drag ON, press HOME, wait, relaunch app | Pass | Recovered in `3583ms`; final `dragging_after=false`; connected callback received. |
| Screen-off recovery with Drag ON | Turn Drag ON, screen off, wait, wake, dismiss keyguard, relaunch app | Pass with caution | Recovered in `19298ms`; first auto reconnect request was rejected, second attempt connected. Final `dragging_after=false`. |
| Process force-stop recovery | Ensure Drag OFF, `am force-stop`, relaunch app | Pass | Recovered in `3581ms`; connected callback received; new process counters reset to zero. |

## Reconnect Metrics

Explicit disconnect/reconnect repeat count: 20.

| Metric | Value |
|---|---:|
| Success count | 20 |
| Failure count | 0 |
| Success rate | 100% |
| Min | 726ms |
| Median | 1646ms |
| P95 | 2251ms |
| Max | 2966ms |

Samples in milliseconds:

```text
2206, 1583, 1696, 726, 809, 1248, 2966, 866, 846, 1751,
790, 1215, 1646, 2251, 1983, 1646, 1175, 1863, 1748, 1784
```

The explicit reconnect run satisfies the Phase 0 target of at least 90% success rate and P95 within 10 seconds.

## Lifecycle Notes

- Background recovery passed after a Drag ON safety scenario. The app returned with Drag Mode off and HID connected.
- Screen-off recovery passed, but the run showed one transient `connect request rejected reason=auto_reconnect` before the next reconnect attempt succeeded. This path should stay in the regression suite.
- Process force-stop recovery passed from Drag OFF. A forced process kill while Drag is physically held cannot be fully proven safe from ADB alone because the process is terminated before it can guarantee a final release report.

## Gate Status

| Gate | Status | Note |
|---|---|---|
| Drag Mode 30-cycle ADB run | Pass | No in-app send failures, final state returned to `Drag`. |
| Explicit reconnect 20-cycle P95 | Pass | P95 `2251ms`, success `20/20`. |
| Background recovery sanity check | Pass | One ADB lifecycle run. |
| Screen-off recovery sanity check | Pass with caution | One ADB lifecycle run, slow due one rejected reconnect attempt. |
| Process-stop recovery sanity check | Pass | One ADB lifecycle run from Drag OFF. |
| Host-side cursor and real drag validation | Not covered by ADB | Requires visual/manual verification on the paired PC. |

## Follow-up

- Run the screen-off protocol as a full 20-cycle, 30-second-wait test before treating screen-off recovery as a release gate.
- Repeat the same report on Windows 11 and any additional Android devices in the support matrix.
- Keep host-side visual confirmation for real drag behavior because Android logs cannot prove the PC desktop effect.
