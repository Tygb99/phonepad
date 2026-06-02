# Windows 재페어링 리셋 스모크 테스트 - 2026-06-02

> English original: [../WINDOWS_REPAIRING_RESET_SMOKE_2026-06-02.md](../WINDOWS_REPAIRING_RESET_SMOKE_2026-06-02.md)

## 목적

이 문서는 Windows 연결 실패가 양쪽 Bluetooth 페어링을 리셋한 뒤 복구된 사례를 기록합니다.

## 결과

stale pairing 원인으로 보는 조건부 통과. 첫 번째 수동 Windows 호스트 전환은 `CONNECTING`까지 진입했지만, Android bond 상태가 `BOND_BONDED`에서 `BOND_NONE`으로 떨어지며 `DISCONNECTED`가 됐습니다. Windows 장치 기록을 삭제하고 PhonePad의 `Android 페어링 삭제`와 `새 PC 연결` 흐름을 사용한 뒤 같은 Windows 호스트가 정상 연결됐습니다.

## 테스트 환경

| 항목 | 값 |
|---|---|
| 날짜 | 2026-06-02 |
| 소스 기준 | `22a582c Add Windows development setup` |
| 설치 앱 | `0.2.0-phase0`, `versionCode=18` |
| Android 기기 | Samsung Galaxy S23 Ultra, `SM-S918N` |
| Android 버전 | Android 16 / API 36 |
| Windows 호스트 label | `TYGB` |
| ADB transport | Wireless ADB, device serial redacted |

## 관찰된 로그 순서

Bluetooth 주소는 의도적으로 이 문서에 남기지 않습니다.

| 시간 | 이벤트 | 해석 |
|---|---|---|
| 10:25:48 | 연결된 Mac에서 `TYGB`로 `defer_switch_manual_switch` | PhonePad가 호스트 전환 전에 현재 호스트를 먼저 해제. |
| 10:25:48 | `TYGB` 대상 `connect_request_manual_switch`, bond `12`, HID 상태 `연결 안 됨` | Windows 호스트에 `BluetoothHidDevice.connect(host)` 시도. |
| 10:25:48 | `TYGB` 대상 `callback_state_연결 중` | HID profile이 연결 시도를 수락하고 connecting 상태로 진입. |
| 10:25:57 | `TYGB` 대상 `callback_state_연결 안 됨`, bond `10` | Windows/Android 페어링 상태가 더 이상 유효하지 않아 성공 연결 전에 끊김. |
| 10:26:04 | `TYGB` 대상 `remove_bond_request` | 사용자가 PhonePad의 Android 쪽 페어링 삭제를 실행. |
| 10:26:13 | `hid_foreground_service=start reason=discoverable_request` | 사용자가 새 PC 페어링 흐름을 시작. |
| 10:26:51 | `TYGB` 대상 `callback_state_연결됨`, bond `12` | 재페어링 완료 후 Windows HID 호스트가 정상 연결됨. |

## 확인된 복구 절차

1. Windows Bluetooth 장치에서 PhonePad를 삭제합니다.
2. PhonePad에서 stale Windows 호스트 후보를 선택합니다.
3. `Android 페어링 삭제`를 누릅니다.
4. `새 PC 연결`을 누릅니다.
5. Windows Bluetooth 기기 추가 흐름에서 새로 광고되는 `PhonePad - {기기명}` 항목을 페어링합니다.
6. PhonePad로 돌아와 `연결!`을 누릅니다.

## 해석

- 이번 사례는 최신 원격 변경의 Android 코드 회귀로 보지 않습니다. 최신으로 가져온 커밋은 Windows 개발 문서와 스크립트만 추가했습니다.
- 로그는 `connect request rejected` 경로가 아니라, 연결 시도 수락 후 `CONNECTING`에서 bond 상태가 `BOND_NONE`으로 떨어지며 끊긴 흐름을 보여줍니다.
- 전체 재페어링 뒤 연결이 성공했으므로 오래된 HID descriptor 또는 Windows Bluetooth cache에 남은 stale Windows/Android pairing record가 원인으로 보입니다.
- 새 PC 흐름에서 `PhonePad - {기기명}` 이름을 유지하는 것은 stale host record와 새 페어링 항목을 구분하는 데 계속 유용합니다.

## 후속

- Windows 재페어링 리셋 흐름을 표준 troubleshooting guide에 유지합니다.
- 이 문제가 자주 반복되면 Windows 호스트가 `CONNECTING` 직후 bond `10`으로 끊기는 경우 앱에서 더 명확한 안내를 표시합니다.
- 복구 후 호스트 측 커서 동작은 사용자가 확인했지만, 상세 클릭/스크롤/드래그 지표는 이번 로그 캡처 범위에 포함하지 않았습니다.

## 관련 문서

- [HID.md](HID.md)
- [TEST.md](TEST.md)
- [MOBILE_APP.md](MOBILE_APP.md)
