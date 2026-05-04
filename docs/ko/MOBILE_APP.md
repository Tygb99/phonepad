# 모바일 앱

> English original: [../MOBILE_APP.md](../MOBILE_APP.md)

## 목적

Android 앱 구현, 빌드, 권한, foreground service, 스토어 배포 규칙을 담습니다.

## 현재 상태

제품은 Android 우선입니다. 실제 기기 Bluetooth HID 테스트를 위한 네이티브 Kotlin Phase 0 debug APK가 있습니다. 첫 공개 배포 경로는 GitHub Releases APK이며, Play Store는 v1.0 확신 이후입니다.

## 현재 규칙

- Android API 28+가 필요합니다.
- Target SDK는 Galaxy S23 Ultra 테스트를 위해 현재 Android 16 / API 36입니다.
- Phase 0 검증 환경: Windows 11 PC, macOS 26.4.1 개발 호스트, Android 16 Galaxy S23 Ultra.
- 앱은 `INTERNET` 없이 동작해야 합니다.
- Foreground service는 활성 connected-device 사용 사례에만 허용됩니다.
- Notification 권한은 재연결/상태에 왜 필요한지 사용자가 이해한 뒤 요청합니다.

## 빌드 대상

| 대상 | 목적 |
|---|---|
| Debug APK | Phase 0 기기 테스트. |
| Release APK | GitHub Releases 배포. |
| AAB | 스토어 출시가 진행될 경우 Play Store v1.1+. |

현재 debug APK 경로:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 기기 테스트 순서

1. `권한`을 탭하고 Nearby devices를 허용합니다.
2. 앱이 HID 세션을 자동으로 준비하고 검색 가능 모드를 요청합니다.
3. 호스트 PC에서 기존 `PhonePad` Bluetooth 장치를 제거한 뒤 표시되는 `PhonePad - {기기명}` 장치를 페어링합니다.
4. `목록 새로고침`을 탭합니다.
5. 페어링된 PC 호스트를 선택하고 `선택 호스트 연결`을 탭합니다.
6. 오른쪽 터치패드 모드를 사용합니다. `스크롤 ↑` 또는 `스크롤 ↓`를 누르고 있으면 계속 스크롤됩니다.

연속 스크롤 버튼은 macOS의 호스트 측 스크롤 가속이 과하게 붙지 않도록 작은 wheel 리포트를 느린 간격으로 보냅니다.
호스트 목록은 페어링된 컴퓨터형 기기와 과거 연결 성공 이력이 있는 PC만 표시합니다.

로컬 빌드 명령:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export ANDROID_SDK_ROOT="$ANDROID_HOME"
./gradlew :app:assembleDebug
```

연결된 Android 기기 설치 명령:

```bash
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
"$ANDROID_HOME/platform-tools/adb" install -r app/build/outputs/apk/debug/app-debug.apk
```

## Manifest 체크리스트

- Android 12+용 `BLUETOOTH_CONNECT`.
- 페어링 경로에 필요하다면 `BLUETOOTH_ADVERTISE`.
- 스파이크 근거 이후에만 `BLUETOOTH_SCAN`.
- 첫 성공 연결 이후 런타임에서만 요청되는 `POST_NOTIFICATIONS`.
- 백그라운드 연결 동작을 출시하기 전에 `FOREGROUND_SERVICE`와 connected-device foreground service 선언.
- `INTERNET` 없음.
- 기본적으로 광고, 분석, crash-reporting SDK 없음.

## Foreground Service 규칙

- 활성 연결 중이거나 HID 연결을 유지할 때 시작합니다.
- 지속 상태 알림을 표시합니다.
- 사용자가 연결을 끊고 활성 재연결 시도가 없으면 중지합니다.
- 서비스 중지 전 항상 `releaseAllMouseButtons()`를 시도합니다.

## 스토어 포지셔닝

- 테스트 기기 매트릭스를 포함한 GitHub 우선 릴리스.
- 지원 정책, 개인정보 공개, 권한 설명이 안정된 뒤 Play Store.
- 재현 가능한 빌드가 깨끗해진 뒤 F-Droid 평가 가능.

## 관련 문서

- [HID.md](HID.md)
- [DEPLOY.md](DEPLOY.md)
- [SECURITY.md](SECURITY.md)
- [TEST.md](TEST.md)
