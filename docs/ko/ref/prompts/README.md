# AI 코딩 프롬프트

> English original: [../../../ref/prompts/README.md](../../../ref/prompts/README.md)

## 목적

구현이 PhonePad 제약과 계속 맞도록 AI 코딩 도구용 재사용 프롬프트를 제공합니다.

## 현재 상태

이 프롬프트를 출발점으로 사용합니다. 생성 코드는 작고 테스트되어야 하며 실제 Android 하드웨어 근거와 연결되어야 합니다.

## 현재 규칙

- 구현과 검증 단계를 함께 요청합니다.
- `INTERNET` 권한이 없도록 요구합니다.
- 입력 변경에는 Drag Mode 안전 해제 확인을 요구합니다.
- 동작이 바뀌면 문서 업데이트를 요구합니다.
- 프롬프트에 비공개 PRD 초안이나 채팅 로그를 포함하지 않습니다.

## Phase 0 Kotlin HID Spike 프롬프트

```text
Implement a minimal Android Kotlin spike for PhonePad that checks Android API level,
gets BluetoothHidDevice through getProfileProxy, registers a minimal mouse HID app,
and exposes a test action that sends cursor movement to the paired host.

Constraints:
- minSdk 28
- no INTERNET permission
- document every Bluetooth permission used
- log device model, Android version, registration result, and sendReport result locally
- add a short test checklist for Windows 11 and macOS
```

## Drag Mode 리뷰 프롬프트

```text
Review this PhonePad change for Drag Mode safety.

Check:
- Drag Mode ON sends left-button-down once
- movement while ON keeps the left-button bit
- OFF sends all-buttons-up
- disconnect, host switch, unregister, app background, screen lock, and service stop call releaseAllMouseButtons
- runtime Drag Mode state is not persisted
- tests cover repeated ON/OFF and safety events
```

## Manifest 리뷰 프롬프트

```text
Review the Android manifest and Gradle files for PhonePad.

Must confirm:
- no INTERNET permission
- Bluetooth permissions are the minimum needed for targetSdk 35
- BLUETOOTH_SCAN is absent unless documented by a failing spike
- foreground service type is appropriate for connected device behavior
- notification permission is requested at runtime, not as unexplained onboarding
```

## 관련 문서

- [../../HID.md](../../HID.md)
- [../../DRAG_MODE.md](../../DRAG_MODE.md)
- [../../MOBILE_APP.md](../../MOBILE_APP.md)
