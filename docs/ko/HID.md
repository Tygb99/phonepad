# Bluetooth HID

> English original: [../HID.md](../HID.md)

## 목적

Android가 Windows와 macOS에 표준 입력 장치처럼 보이게 하는 Direct Bluetooth HID 경로를 정의합니다.

## 현재 상태

v1.0의 핵심 가정은 Android `BluetoothHidDevice`입니다. Phase 0에서는 실제 Android 기기가 안정적으로 등록, 광고, 페어링, 마우스/키보드 리포트 전송을 할 수 있는지 검증해야 합니다.

## 현재 규칙

- "Bluetooth HID"는 호스트와 btsnoop 관찰 뒤에만 확정된 표현으로 사용합니다.
- 전송/프로필 근거가 확인되기 전에는 HOGP라고 주장하지 않습니다.
- MVP에는 최소 마우스 + 키보드 복합 디스크립터를 사용합니다.
- Phase 0 권한 스파이크가 필요성을 입증하기 전에는 `BLUETOOTH_SCAN`을 피합니다.
- v1.0에는 Precision Touchpad 디스크립터를 구현하지 않습니다.
- 메인 UX는 앱이 전면에 들어오면 HID 세션을 자동 등록하고 앱을 나가면 자동 해제합니다.
- 앱 전면 진입만으로는 Android 검색 허용 팝업을 열지 않으며, 새 PC 연결 동작에서만 엽니다.
- HID 등록 뒤 마지막 성공 호스트로 조용히 자동 재연결을 시도합니다.
- PC에 표시되는 Bluetooth 이름은 `PhonePad - {기기명}` 형식을 사용합니다.
- 호스트 선택은 임의의 주변 기기를 숨기고, 페어링된 컴퓨터형 기기와 과거 연결 성공 이력이 있는 PC만 보여줍니다.
- 새 PC 연결 흐름 뒤 새로 감지된 bonded host는 현재 전환 대상으로 선택하고, 별도 수동 연결 탭 없이 연결을 시도합니다.
- Windows 연결 실패는 HID callback 상태, bond 상태, Bluetooth class, selected/known/candidate 플래그, `connect(host)` 수락 여부를 로그에 남겨야 합니다.
- 수동 전환과 새 PC 연결 시도는 HID connected 콜백이 오지 않으면 timeout 처리하고, 연결 중 상태에 머무르지 않도록 Windows 재페어링 안내를 표시합니다.

## Android 빌드 기준선

```text
minSdk: 28
targetSdk: 36
compileSdk: 36
foregroundServiceType: connectedDevice
```

## 권한

| 권한 | 규칙 |
|---|---|
| `BLUETOOTH_CONNECT` | Android 12+에서 Bluetooth 작업에 필요. |
| `BLUETOOTH_ADVERTISE` | 검색 가능/광고 경로에 필요할 것으로 예상. |
| `BLUETOOTH_SCAN` | 페어링/재연결이 없이는 동작하지 않는다는 근거가 있을 때만 추가. |
| `POST_NOTIFICATIONS` | 첫 성공 연결 후 명확한 이유와 함께 요청. |
| `FOREGROUND_SERVICE_CONNECTED_DEVICE` | 최신 Android의 connected foreground service에 필요. |
| `INTERNET` | 선언하면 안 됨. |

## Phase 0 HID 스파이크

| 스파이크 | 성공 기준 |
|---|---|
| 프로필 접근 | 최소 Pixel과 Samsung 테스트 기기에서 `getProfileProxy(BluetoothHidDevice)` 성공. |
| 등록 | 최소 디스크립터로 `registerApp` 성공. |
| 마우스 리포트 | `sendReport`로 호스트 커서 이동. |
| 클릭/스크롤 | 왼쪽 클릭, 오른쪽 클릭, 세로/가로 스크롤 동작. |
| 키보드 리포트 | Windows/macOS에서 단축키 리포트 3개 동작. |
| 전송 근거 | Windows/macOS 장치 이름과 btsnoop 로그 기록. |
| 최소 권한 | `BLUETOOTH_SCAN` 생략 가능 여부 확인. |

## HID 리포트 전략

- 마우스 리포트: `dx`, `dy`, `buttons`, `wheel`, `horizontalWheel`.
- 키보드 리포트: modifiers와 최대 6개 keycodes.
- Drag Mode ON: 마우스 리포트에 왼쪽 버튼 bit 포함.
- Drag Mode OFF: 모든 버튼을 지운 button-up 리포트 전송.
- 더블 탭 드래그는 선택 기능이며 기본값은 OFF입니다. 켜진 경우 두 번째 탭을 유지하면 손을 뗄 때까지 left-button-down을 보냅니다.
- 스크롤 버튼은 느림/기본/빠름 프리셋을 제공해 기본 macOS 과가속 완화값을 유지하면서 호스트별 체감 속도 차이를 조정합니다.
- 안전 해제: 연결 해제, 등록 해제, 호스트 전환, 앱 백그라운드 종료 경로, 화면 잠금, 가능한 경우 프로세스 종료 훅에서 all-buttons-up 전송.

## 호스트 테스트 노트

- 최신 Windows 11은 릴리스 게이트입니다.
- 최신 macOS는 릴리스 게이트입니다.
- Windows 10 22H2는 best-effort 호환성입니다.
- Linux는 v1.0에서 공식 지원하지 않습니다.
- BIOS/UEFI는 Direct Bluetooth 모드에서 약속하지 않습니다.

## 관련 문서

- [ARCHITECTURE.md](ARCHITECTURE.md)
- [DRAG_MODE.md](DRAG_MODE.md)
- [TEST.md](TEST.md)
- [MOBILE_APP.md](MOBILE_APP.md)
- [BRIDGE_DONGLE.md](BRIDGE_DONGLE.md)
