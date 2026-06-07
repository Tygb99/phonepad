# Android 12 호환성 스모크 테스트 - 2026-05-31

> English original: [../ANDROID12_COMPAT_SMOKE_2026-05-31.md](../ANDROID12_COMPAT_SMOKE_2026-05-31.md)

## 목적

이 문서는 `OnBackInvokedCallback` 생성을 Android 13+ API guard 뒤로 미룬 뒤 실행한 Android 12 이하 호환성 스모크 테스트 결과를 기록합니다.

## 결과

통과. 실제 Android 12 Samsung 기기에서 debug APK가 실행됐고, 연결 drawer는 기존 back 경로로 열고 닫혔으며, logcat에서 시작 crash 또는 back navigation 관련 crash 신호가 확인되지 않았습니다.

## 테스트 환경

| 항목 | 값 |
|---|---|
| 날짜 | 2026-05-31 |
| 브랜치 | `codex/host-language-toggle-guide-panel` |
| 커밋 | `c2c4797 fix: defer drawer back callback creation` |
| 앱 버전 | `0.2.0-phase0`, `versionCode=18` |
| APK | `Android/app/build/outputs/apk/debug/app-debug.apk` |
| 기기 | Samsung `SM-N976N` |
| Android | `12`, API `31` |
| 앱 target SDK | `36` |
| ADB serial | Wireless ADB local endpoint redacted |
| 방향 | 가로 모드 |

## 확인 항목

| 확인 | 결과 | 근거 |
|---|---|---|
| Debug APK 빌드 | 통과 | `cd Android && ./gradlew assembleDebug` 성공. |
| APK 설치 | 통과 | `adb install -r app-debug.apk`가 `Success` 반환. |
| 런타임 권한 | 통과 | 스모크 테스트를 위해 `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE` 권한을 부여. |
| 콜드 런치 | 통과 | force-stop 후 재실행 시 `MainActivity`가 focused window가 됨. |
| Android 12 시작 호환성 | 통과 | logcat에서 `FATAL EXCEPTION`, `NoClassDefFoundError`, `OnBackInvoked` 매치 없음. |
| 연결 drawer 열기 | 통과 | `연결!` 버튼 탭으로 PhonePad 연결 패널 열림. |
| 기존 back 처리 | 통과 | `adb shell input keyevent BACK` 입력 후 drawer가 닫히고 메인 터치패드 화면으로 복귀. |
| 연결 대기 상태 service 정리 | 통과 | `dumpsys activity services com.tygb99.phonepad`에서 `HidSessionService`가 보이지 않음. |

## 참고

- 이 기기에서는 `uiautomator dump`가 `ERROR: null root node returned by UiTestAutomationBridge`를 반환했습니다. 따라서 UI 상태는 screenshot, focused-window 상태, logcat으로 확인했습니다.
- Android 12에서는 Android 13+의 `OnBackInvokedCallback` 경로를 실행할 수 없습니다. 이번 테스트는 API 31 런타임에서 callback class linkage로 crash가 나지 않는지, 기존 fallback back 동작이 유지되는지를 확인한 것입니다.
- 호스트 측 HID 이동, 클릭, 스크롤, 드래그, 한영 전환 동작은 이번 Android 12 스모크 테스트 범위에 포함하지 않았습니다.

## 후속 확인

- Android 12 이하 시작 및 drawer back 스모크 체크를 host-language/guide-panel 회귀 테스트 세트에 계속 포함합니다.
- Android 13+ system back callback 경로는 API 33 이상 기기에서 별도로 계속 검증합니다.
