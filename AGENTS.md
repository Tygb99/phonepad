# PROJECT KNOWLEDGE BASE

**Generated:** 2026-07-10 21:19:16 KST
**Commit:** `ea1a6a6`
**Branch:** `main`

## OVERVIEW

PhonePad는 휴대폰을 표준 Bluetooth HID 입력장치로 쓰는 프로젝트다. 현재 주 경로는 네이티브 Kotlin Android Direct HID이며, iOS는 ESP32-S3 Bridge Dongle로 BLE 명령을 보내는 별도 스파이크다. 정적 GitHub Pages 랜딩과 영·한 쌍의 제품 문서도 같은 저장소에서 관리한다.

## STRUCTURE

```text
phonepad/
├── Android/                         # v1 주 경로: 독립 Gradle 루트와 Kotlin Direct HID 앱
├── ios/PhonePad/                    # SwiftUI/CoreBluetooth 동글 송신 앱과 Xcode 프로젝트
├── shared/firmware/bridge-dongle/   # XIAO ESP32-S3 BLE→USB HID 펌웨어
├── shared/assets/brand/             # 추적되는 공용 브랜드 원본
├── docs/                            # 영문 계약·증거 문서; 한국어 대응본은 docs/ko/
├── site/                            # 빌드 단계 없는 정적 GitHub Pages 번들
└── .github/workflows/               # Android APK/Release와 Pages 배포
```

## WHERE TO LOOK

| 작업 | 위치 | 기준 |
|---|---|---|
| 공개 릴리스·설치 상태 | `README.md`, `README.ko.md` | 릴리스 기준으로 사용; UI 배치·동작은 현재 코드와 Android 하위 지침 우선 |
| Android HID/UI/연결 | `Android/AGENTS.md` | 하위 지침을 먼저 읽는다 |
| Android 직렬화 | `Android/app/src/main/java/com/tygb99/phonepad/MainActivity.kt` | HID descriptor와 release payload의 실행 기준 |
| 호스트 OS·한영 전환 | `HostInputPolicy.kt`, 대응 테스트 | Mac Control+Space, Windows LANG1/RightAlt |
| iOS 앱 진입·UI | `PhonePadApp.swift`, `ContentView.swift` | scene 비활성화 때 `releaseAll()` 유지 |
| iOS BLE 상태·패킷 | `BLEMouseController.swift` | UUID, write mode, reconnect의 실행 기준 |
| 동글 수신·USB HID | `shared/firmware/bridge-dongle/BLETouchMouse/BLETouchMouse.ino` | 현재 체크아웃의 실제 지원 범위만 설명 |
| 동글 wire contract | `docs/DONGLE_PROTOCOL.md`, `docs/ko/DONGLE_PROTOCOL.md` | `docs/API.md`의 옛 초안보다 우선 |
| 제품·보안·테스트 계약 | `docs/INDEX.md`, `HID.md`, `DRAG_MODE.md`, `SECURITY.md`, `TEST.md` | “현재”와 “계획”을 코드로 대조 |
| 랜딩·배포 | `site/`, `.github/workflows/pages.yml` | `site/`를 그대로 Pages artifact로 올리며 download CTA는 GitHub Releases 유지 |
| Android 배포 | `.github/workflows/android-apk.yml`, `docs/DEPLOY.md` | 현재 산출물은 debug-labeled APK |

## CODE MAP

| 심볼/진입점 | 형식 | 위치 | 연결 | 역할 |
|---|---|---|---|---|
| `MainActivity` | Android `Activity` | `Android/.../MainActivity.kt` | 100+ 심볼의 중심 | HID 등록, 연결, UI, 제스처, report 전송 |
| `HostInputPolicy` | Kotlin object | `Android/.../HostInputPolicy.kt` | Activity + 8 정책 테스트 | 호스트 추론과 키 입력 결정 |
| `HidForegroundServicePolicy` | Kotlin object | `Android/.../HidForegroundServicePolicy.kt` | Activity + 정책 테스트 | 서비스 실행 조건 |
| `PhonePadApp` | SwiftUI `App` | `ios/PhonePad/PhonePad/PhonePadApp.swift` | `ContentView`, controller | iOS 진입·lifecycle release |
| `ContentView` | SwiftUI/UIKit bridge | `ios/PhonePad/PhonePad/ContentView.swift` | `BLEMouseController` 호출 | 터치·버튼·키보드 UI |
| `BLEMouseController` | CoreBluetooth controller | `ios/PhonePad/PhonePad/BLEMouseController.swift` | UI → BLE characteristic | 스캔, 연결, 패킷 인코딩 |
| `MouseCommandCallbacks` | BLE write callback | `shared/.../BLETouchMouse.ino` | GATT write → handlers | 패킷 분기와 USB HID 출력 |
| `site/app.js` | browser script | `site/app.js` | `index.html` | GitHub star 조회와 fallback `1` |

## CONVENTIONS

- Android Direct HID와 iOS Bridge Dongle은 서로 다른 전송 방식이다. 동작 의미는 공유해도 Android USB HID usage와 Arduino key code, raw byte layout을 섞지 않는다.
- 동글 프로토콜 변경은 Swift sender, 펌웨어, `DONGLE_PROTOCOL.md`, `docs/ko/DONGLE_PROTOCOL.md`, 관련 iOS README를 함께 갱신한다.
- BLE v1 mouse packet은 정확히 4바이트 `dx, dy, buttons, wheel`이다. 다른 명령은 절대 정확히 4바이트가 되면 안 된다.
- 안전 해제는 P0 계약이다. Android의 정확한 해제 시점은 `releaseAllMouseButtons()`와 `releaseAllKeyboardKeys()` 호출부를 기준으로 보존하며 모든 gesture/lifecycle 경로가 같은 동작이라고 일반화하지 않는다. iOS scene 비활성화·수동 disconnect와 동글 BLE disconnect는 `releaseAll` 경로를 보존한다.
- UI 문구는 한국어 우선이다. 영문 문서를 만들거나 수정하면 기존 관례에 따라 한국어 대응본도 같은 변경에서 갱신한다: 루트/iOS는 인접 `.ko.md`, 제품 문서는 `docs/ko/` 미러다.
- 새로 만들거나 갱신하는 문서 산출물은 `YYYY-MM-DD-HH-mm-ss-` 접두사를 사용한다. 도구 고정 이름인 `AGENTS.md`, `README.md`, 설정·테스트 fixture·데이터 CSV는 예외다.
- npm으로 패키지를 설치하거나 삭제하지 않는다. `site/`는 package manager와 build step이 없는 HTML/CSS/JS 번들로 유지한다.
- AI 추론, TTS, 전사, 이미지·영상 생성, 렌더링·인코딩은 GPU/하드웨어 경로를 먼저 확인한다. macOS 영상은 CPU 최종 경로 전에 VideoToolbox를 검토한다.

## ANTI-PATTERNS (THIS PROJECT)

- `INTERNET`, 계정, 광고, 분석, 추적, 자동 로그 업로드를 추가하지 않는다. `BLUETOOTH_SCAN`도 실제 실패 근거 없이 추가하지 않는다.
- v1에 desktop helper/server, remote desktop, mirroring, file/clipboard transfer를 넣지 않는다.
- PhonePad를 native Precision/Magic Trackpad, 모든 장치 호환, BIOS/iOS 지원, 출시된 스토어 앱으로 과장하지 않는다.
- Drag Mode를 시작 시 복원하거나 release 없이 host/disconnect/lifecycle 상태를 바꾸지 않는다.
- `gpt의견/`, `prd-*.md`, 채팅·미디어, `backups/`, `.omo/`, `.codegraph/`, `build/`, Gradle/Kotlin cache를 현재 소스나 AGENTS 배치 근거로 쓰지 않는다.
- 개인 `DEVELOPMENT_TEAM`, `xcuserdata`, DerivedData, local signing state를 커밋하지 않는다.
- simulator/compile 성공을 실제 Bluetooth, USB HID, latency, BIOS 동작의 증거로 표현하지 않는다.
- 다른 브랜치나 이전 세션의 `0x13`, status `...1002` 기능을 현재 `main`에 구현된 것으로 간주하지 않는다.

## UNIQUE STYLES

- iOS UI는 SwiftUI 화면 안에 UIKit text/touch bridge를 포함한다.
- `shared/`는 공용 코드 모듈이 아니라 브랜드 자산과 선택형 동글 펌웨어 묶음이다.
- 날짜가 붙은 smoke/test report는 그 버전·commit의 증거 스냅샷이다. 최신 상태에 맞춰 덮어쓰지 말고 새 쌍을 만든다.

## COMMANDS

```bash
# Android: JDK 17과 로컬 SDK 경로를 명시
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export ANDROID_SDK_ROOT="$ANDROID_HOME"
(cd Android && ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --stacktrace)

# iOS compile-only; 실제 BLE 검증이 아님
xcodebuild -project ios/PhonePad/PhonePad.xcodeproj -scheme PhonePad \
  -destination "generic/platform=iOS Simulator" CODE_SIGNING_ALLOWED=NO build

# firmware compile-only; upload는 별도 명시 요청과 실제 보드/포트 확인 후
arduino-cli compile --fqbn esp32:esp32:XIAO_ESP32S3 \
  shared/firmware/bridge-dongle/BLETouchMouse

# 정적 사이트
node --check site/app.js
python3 -m http.server 4190 --bind 127.0.0.1 --directory site

git diff --check
```

## NOTES

- Kotlin LSP는 설치되어 있지 않다. Android 정적 구조는 현재 소스와 codegraph 도구의 조회 결과로 탐색하되 raw `.codegraph/` 파일을 근거로 삼지 않고 Gradle로 검증한다.
- SourceKit 단독 진단은 Xcode SDK context 부족으로 오탐이 날 수 있다. iOS correctness gate는 `xcodebuild`다.
- Android CI는 현재 unit test를 실행하지 않는다. 로컬 전체 gate에는 `:app:testDebugUnitTest`를 포함한다.
- iOS와 펌웨어에는 자동화된 테스트/CI가 없다. 실제 iPhone + flashed dongle + USB host 검증을 별도로 기록한다.
- `CLAUDE.md`, 일부 architecture/API/DB/roadmap 문서는 구현보다 오래된 계획을 섞고 있다. 현재 코드와 README를 먼저 대조한다.
- iOS 일반 문자 인코더는 현재 ASCII physical key만 지원한다. 다국어 입력은 Issue #4의 미구현 범위이며 지원된다고 쓰지 않는다.
- Issue #3의 iOS drag/scroll 보고는 `codex/bridge-dongle-v2-status` 대상이다. `main`에도 동일하다고 가정하지 말고 재현부터 한다.
