# 아키텍처

> English original: [../ARCHITECTURE.md](../ARCHITECTURE.md)

## 목적

데스크톱 쪽을 서버리스로 유지하면서 PhonePad가 터치 입력을 Bluetooth HID 리포트로 바꾸는 방식을 설명합니다.

## 현재 상태

계획된 스택은 UI용 Flutter, Android Bluetooth HID용 Kotlin, 통합용 MethodChannel/EventChannel, 로컬 호스트 및 설정 데이터용 SQLite/Drift입니다.

## 현재 규칙

- Flutter UI에 크게 투자하기 전에 Kotlin HID 경로를 입증합니다.
- 호스트 컴퓨터는 표준 Bluetooth 페어링 외에는 건드리지 않습니다.
- 런타임 Drag Mode 상태는 영구 저장소에 넣지 않습니다.
- 모든 사용자 입력은 기기와 Bluetooth HID 전송 경로 안에만 둡니다.
- Bridge Dongle 작업은 앱 전용 MVP와 분리합니다.

## 시스템 레이어

```text
Flutter UI
  Trackpad surface, Drag toggle, host list, settings, gesture mapping
      |
      | MethodChannel / EventChannel
      v
Android Kotlin Native Layer
  BluetoothHidDevice registration, SDP, pairing, report sending,
  foreground service, lifecycle release hooks
      |
      | Bluetooth HID Device Profile
      v
Windows / macOS
  Standard HID mouse + keyboard composite input device
```

## 런타임 흐름

1. 앱이 시작되면 Android 버전, Bluetooth 사용 가능 여부, 필요한 권한, HID Device 프로필 접근을 확인합니다.
2. 네이티브 레이어가 최소 마우스/키보드 복합 디스크립터로 HID 앱을 등록합니다.
3. 사용자가 호스트 OS Bluetooth 설정에서 Android 휴대폰을 페어링합니다.
4. Flutter 트랙패드 이벤트가 정규화된 이동, 버튼, 휠, 단축키 액션으로 변환됩니다.
5. Kotlin 네이티브 레이어가 활성 호스트로 HID 리포트를 보냅니다.
6. 라이프사이클 및 연결 이벤트는 등록 해제나 상태 전환 전에 항상 `releaseAllMouseButtons()`를 시도합니다.

## 주요 컴포넌트

| 컴포넌트 | 책임 |
|---|---|
| `CompatChecker` | Android/BT/HID 기능 감지와 미지원 기기 사유. |
| `HidDeviceService` | HID 앱 등록/해제, 리포트 전송, 콜백 노출. |
| `HostRepository` | 페어링된 호스트와 기본 호스트 설정 저장. |
| `GestureEngine` | 포인터 스트림을 이동, 스크롤, 탭, 세 손가락 스와이프로 변환. |
| `DragModeController` | Drag Mode 상태 머신과 해제 보장 소유. |
| `SettingsRepository` | 민감도, 스크롤, 햅틱, 테마, 로케일, 토글 위치 저장. |
| `DiagnosticsRecorder` | 텔레메트리 없이 로컬 테스트 노트와 복사 가능한 로그 저장. |

## 실패 처리

- 미지원 Android: 사유를 표시하고 호환성 노트로 연결합니다.
- 등록 실패: 등록 해제 후 한 번 재시도하고, 이후 진단 상세를 표시합니다.
- 연결 해제: 연결 상태를 disconnected로 설정하고 버튼을 해제한 뒤 재연결을 시도합니다.
- 호스트 전환: 다른 호스트를 선택하기 전에 버튼을 해제합니다.
- 드래그 해제 실패: 눈에 띄는 경고와 수동 복구 안내를 표시합니다.

## 관련 문서

- [HID.md](HID.md)
- [DRAG_MODE.md](DRAG_MODE.md)
- [DB.md](DB.md)
- [API.md](API.md)
- [SECURITY.md](SECURITY.md)
