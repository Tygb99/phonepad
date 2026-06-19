# PhonePad

> English original: [README.md](README.md)

PhonePad는 휴대폰 화면을 Windows와 macOS용 Bluetooth 트랙패드형 컨트롤러로 바꾸는 Android 우선 입력 앱입니다.

v1.0의 기본 경로는 Android Direct Bluetooth HID Device 모드입니다. 호스트 컴퓨터는 데스크톱 서버 앱 설치 없이 휴대폰을 표준 Bluetooth 마우스/키보드 복합 장치로 인식해야 합니다.

PhonePad는 GNU General Public License v3.0 or later로 배포되는 무료 오픈소스 프로젝트입니다.
GitHub Releases에서 APK를 직접 받을 수 있고, 직접 빌드해서 사용할 수도 있습니다. 추후 Play Store 유료판은 쉬운 설치와 자동 업데이트를 제공하는 후원형 편의판으로 제공될 수 있습니다.

PhonePad는 광고, 계정, 분석 SDK, 추적 SDK, 인터넷 권한을 사용하지 않습니다.
GitHub Sponsors에서 PhonePad 개발을 후원할 수 있습니다: [github.com/sponsors/Tygb99](https://github.com/sponsors/Tygb99).

## 제품 범위

- Android API 28+ `BluetoothHidDevice` 호환성 확인.
- Windows 11과 최신 macOS에서 표준 Bluetooth HID 입력 장치로 페어링.
- 커서 이동, 왼쪽 클릭, 오른쪽 클릭, 세로/가로 스크롤.
- 길게 누르기 드래그 대신 명시적인 Drag Mode 토글.
- OS별 키보드 단축키에 매핑되는 핵심 세 손가락 제스처 3개.
- 계정 없음, 광고 없음, 추적 SDK 없음, 인터넷 권한 없음.
- 한국어 우선 앱 UI, 영어 README와 공개 GitHub 릴리스 산출물.
- Bridge Dongle은 Phase 0 스파이크이며 v1.0 기본 경로가 아닙니다.

## 비목표

- iOS Direct Bluetooth HID 송신 모드.
- 데스크톱 헬퍼/서버 앱.
- 화면 미러링, 파일 전송, 클립보드 동기화, 원격 데스크톱.
- v1.0에서 네이티브 Precision Touchpad 또는 Magic Trackpad 에뮬레이션.
- Direct Bluetooth 모드에서 BIOS/UEFI 지원.

## 문서

먼저 볼 문서:

- [docs/ko/INDEX.md](docs/ko/INDEX.md)
- [docs/ko/QUICK_REF.md](docs/ko/QUICK_REF.md)
- [docs/ko/ARCHITECTURE.md](docs/ko/ARCHITECTURE.md)
- [docs/ko/HID.md](docs/ko/HID.md)
- [docs/ko/DRAG_MODE.md](docs/ko/DRAG_MODE.md)
- [docs/ko/TEST.md](docs/ko/TEST.md)

플랫폼 코드는 `Android/`와 `ios/`에 모읍니다. 공통 프로젝트 자산과 브리지 펌웨어는 `shared/`에 둡니다. 제품 문서는 기존 링크 안정성을 위해 `docs/`에 그대로 둡니다.

## Android Phase 0 앱

이 저장소에는 이제 Direct Bluetooth HID 경로를 검증하기 위한 네이티브 Kotlin Android 스파이크 앱이 포함되어 있습니다.

- 패키지: `com.tygb99.phonepad`
- 최소 Android: API 28
- 현재 타깃: Android 16 / API 36
- APK 출력: `Android/app/build/outputs/apk/debug/app-debug.apk`
- 런타임 권한: Bluetooth connect와 advertise만 사용하며 `INTERNET` 권한은 없습니다.
- 검증 환경: Windows 11 PC, macOS 26.4.1 개발 호스트, Android 16 Galaxy S23 Ultra.

기기 테스트 순서:

1. `권한`을 탭하고 Nearby devices를 허용합니다.
2. 앱이 HID 세션을 자동으로 준비하고 마지막 성공 PC로 조용히 재연결을 시도합니다.
3. 재연결이 실패하면 `목록 새로고침`을 탭하고 PC 후보를 고른 뒤 `선택 호스트 연결`을 탭합니다.
4. 새 PC는 `새 PC 연결`을 탭하고 검색 가능 모드를 허용한 뒤 표시되는 `PhonePad - {기기명}` 장치를 페어링합니다.
5. 오른쪽 터치패드 영역을 사용합니다. `스크롤 ↑` 또는 `스크롤 ↓`를 누르고 있으면 계속 스크롤됩니다.

연속 스크롤 버튼은 macOS의 호스트 측 스크롤 가속이 과하게 붙지 않도록 작은 wheel 리포트를 느린 간격으로 보냅니다.
호스트 목록은 페어링된 컴퓨터형 기기와 과거 연결 성공 이력이 있는 PC만 표시합니다.
앱을 여는 것만으로는 Android 검색 허용 팝업을 띄우지 않습니다.
`호스트 연결/전환`은 페어링된 여러 PC 사이 전환용입니다. 더블 탭 드래그는 기본 OFF 옵션으로 제공하며, 스크롤 버튼 속도는 느림/기본/빠름 중 선택할 수 있습니다.

로컬 빌드:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export ANDROID_SDK_ROOT="$ANDROID_HOME"
(cd Android && ./gradlew :app:assembleDebug)
```

Windows에서는 PowerShell 설정/빌드 스크립트를 사용합니다.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Android\scripts\setup-windows-dev.ps1
.\Android\scripts\build-windows.ps1 -Target all
```

JDK 17, Android SDK, APK 빌드, 기기 설치 절차는 [docs/ko/WINDOWS_DEV.md](docs/ko/WINDOWS_DEV.md)를 참고하세요.

연결된 Android 기기에 설치:

```bash
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
"$ANDROID_HOME/platform-tools/adb" install -r Android/app/build/outputs/apk/debug/app-debug.apk
```

## 저장소

원격 대상: https://github.com/Tygb99/phonepad.git

이 저장소는 문서 전용 상태에서 Phase 0 Android HID 스파이크로 이동했습니다. Android 16 Galaxy S23 Ultra와 Windows 11 PC 조합에서 실제 호스트 페어링 및 마우스 리포트 전달을 검증했습니다.

코드는 GPL-3.0-or-later로 배포됩니다. PhonePad 이름, 로고, 브랜드 자산은 혼동을 일으키는 상업적 재배포에 사용할 수 없습니다.
