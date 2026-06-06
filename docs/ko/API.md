# 내부 API

> English original: [../API.md](../API.md)

## 목적

Flutter, Android 네이티브 HID 코드, 런타임 컨트롤러, 선택적 Bridge Dongle 스파이크 사이의 앱 내부 인터페이스를 정의합니다.

## 현재 상태

v1.0에는 공개 웹 API를 계획하지 않습니다. 이 문서는 로컬 MethodChannel/EventChannel 계약과 동글 패킷 프로토콜 초안을 다룹니다.

## 현재 규칙

- Flutter/native 경계는 작게 유지합니다.
- 네이티브 Kotlin이 Bluetooth HID 등록과 리포트 전송을 소유합니다.
- Flutter가 트랙패드 UI, 표시 상태, 설정 화면, 제스처 입력 표면을 소유합니다.
- 오류는 자유 형식 문자열만이 아니라 타입화된 코드로 노출합니다.
- v1.0에는 네트워크 API를 추가하지 않습니다.

## Flutter에서 Android 네이티브로

```kotlin
getCompatStatus(): CompatStatus
requestNearbyDevicePermissions(): PermissionResult
requestNotificationPermission(): PermissionResult
registerHidApp(): HidRegisterResult
unregisterHidApp()
startAdvertising()
stopAdvertising()
getPairedHosts(): List<Host>
connectToHost(address: String)
disconnect()
sendMouseReport(dx: Int, dy: Int, buttons: Int, wheel: Int, horizontalWheel: Int)
sendKeyReport(modifiers: Int, keycodes: List<Int>)
releaseAllMouseButtons(): Boolean
```

## Android 네이티브에서 Flutter 이벤트로

```kotlin
onCompatStatusChanged(status)
onHidAppStatusChanged(registered: Boolean)
onConnectionStateChanged(state, host)
onHostPaired(host)
onReconnectAttempt(host, attemptNo, success, durationMs)
onMouseButtonReleaseAttempt(reason, success)
onError(code, message)
```

## 런타임 컨트롤러 규칙

```kotlin
enableDragMode()
// state = enabling
// sendMouseReport(0, 0, LEFT_BUTTON, 0, 0)
// state = on

disableDragMode(reason)
// state = release_pending
// sendMouseReport(0, 0, 0, 0, 0)
// state = off on success

onPointerMove(dx, dy)
// if drag mode on: buttons = LEFT_BUTTON
// otherwise: buttons = 0
```

## Bridge Dongle 패킷 초안

자세한 BLE packet source of truth는 [DONGLE_PROTOCOL.md](DONGLE_PROTOCOL.md)입니다. v1 4바이트 mouse packet 호환성을 유지하면서 v2 safety/chord 명령을 추가합니다.

```text
Packet: MouseMove
- type: 0x01
- dx: int16
- dy: int16
- buttons: uint8
- wheel: int8
- hwheel: int8

Packet: KeyCombo
- type: 0x02
- modifiers: uint8
- keycodes: uint8[6]

Packet: Control
- type: 0x03
- command: release_all_buttons | ping | ack
```

현재 iOS sender + ESP32-S3 spike packet type:

```text
0x10: ReleaseAll(seq)
0x11: KeyChord(seq, modifier_mask, keys[0...6])
0x12: LanguageToggle(seq, host_profile)
```

## 오류 코드 계열

| 접두사 | 의미 |
|---|---|
| `compat.*` | Android 버전, Bluetooth, HID 프로필, 권한 실패. |
| `hid.*` | 등록, 디스크립터, sendReport, 등록 해제 실패. |
| `host.*` | 페어링, 재연결, 활성 호스트, OS 프리셋 실패. |
| `drag.*` | Drag Mode 상태 또는 해제 실패. |
| `perm.*` | 런타임 권한 거부 또는 선언 누락. |

## 관련 문서

- [ARCHITECTURE.md](ARCHITECTURE.md)
- [HID.md](HID.md)
- [DRAG_MODE.md](DRAG_MODE.md)
- [BRIDGE_DONGLE.md](BRIDGE_DONGLE.md)
- [DONGLE_PROTOCOL.md](DONGLE_PROTOCOL.md)
