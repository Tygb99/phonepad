# OVERVIEW

대문자 `Android/`는 JDK 17, Gradle 8.14.4, AGP 8.13.2, Kotlin 2.2.21, SDK 28–36을 쓰는 네이티브 Kotlin Direct HID 앱의 독립 Gradle 루트이며 단일 `:app` 모듈을 가진다.

# WHERE TO LOOK

- `app/src/main/java/com/tygb99/phonepad/MainActivity.kt`: HID 등록, 호스트 상태, 터치 입력, programmatic UI, 리포트 전송.
- `app/src/main/java/com/tygb99/phonepad/HidSessionService.kt`: connected-device foreground service와 지속 알림.
- `app/src/main/java/com/tygb99/phonepad/HostInputPolicy.kt`: 호스트 OS 추정, 한영 전환 키, 길게 누르기 정책.
- `app/src/main/java/com/tygb99/phonepad/HidForegroundServicePolicy.kt`: 서비스 실행 조건.
- `app/src/main/AndroidManifest.xml`: Bluetooth 및 foreground-service 권한, 가로 화면, 서비스 선언.
- `app/src/test/`: Android 프레임워크와 분리된 JVM 정책 테스트.
- `scripts/`: Windows JDK/SDK 설정과 test/debug/lint wrapper.

# CODE MAP

- `MainActivity`는 2,352줄이며 파일 상단의 `SIZE_OK` 예외가 의도적으로 유지된다.
- `buildTouchpadPanel()`은 왼쪽 터치패드와 오른쪽 300dp 컨트롤 열을 만든다.
- 연결/페어링 도구는 430dp 왼쪽 drawer로 분리한다. 터치 표면을 scrollable 연결 UI와 섞지 않는다.
- 마우스 report ID는 1, 키보드 report ID는 2다. 복합 HID descriptor와 payload 형상을 함께 검토한다.
- `sendKeyboardStroke()`는 modifier-down → chord → release를 지연 전송한다. Mac Control+Space를 단일 combined report로 축약하지 않는다.
- 호스트별 OS 설정, 최근 성공 호스트, 드래그/스크롤 설정은 `SharedPreferences`에 저장한다.

# CONVENTIONS

- Gradle 명령은 저장소 루트가 아니라 `Android/`에서 `./gradlew`로 실행한다.
- `MainActivity` 분리는 별도 behavior-preserving 작업으로만 계획한다. 작은 기능 수정에 끼워 넣지 않는다.
- 입력은 실제 connected host와 등록된 HID 앱이 있을 때만 전송한다. 화면의 ready 표시만으로 성공을 판단하지 않는다.
- foreground service는 연결 중, 연결 시도 중, 새 PC 검색 흐름 중에만 유지한다.
- 앱 시작만으로 discoverable prompt를 열지 않는다. `새 PC 연결` 동작 이후 HID 등록 완료를 기다린다.
- 새 PC 외부 pairing에서 돌아오면 후보만 선택하고 사용자가 `호스트 연결/전환`을 눌러 연결한다. 외부 흐름의 330초 cleanup grace를 줄이지 않는다.
- 호스트 전환은 기존 호스트의 disconnect callback 뒤 새 호스트 연결을 시작하는 순차 흐름을 보존한다.
- API 33+ drawer back callback은 drawer가 준비된 뒤 등록한다. Activity 초기화 중 앞당기지 않는다.
- Bluetooth 이름은 `PhonePad - {기기명}` 형태와 길이 제한을 유지한다.

# ANTI-PATTERNS

- 요청 없이 Compose, AndroidX, XML layout, 새 UI 프레임워크로 옮기지 않는다.
- 2,352줄이라는 이유만으로 `MainActivity`를 기계적으로 쪼개지 않는다.
- 연결되지 않은 상태에서 report count나 UI 변화만 보고 터치패드가 동작한다고 결론 내리지 않는다.
- HID descriptor를 바꾸고 기존 Windows/Android pairing cache로만 회귀 검증하지 않는다.
- `.gradle/`, `.kotlin/`, `app/build/`, `build/`, `local.properties`, APK를 커밋하지 않는다.

# COMMANDS

- debug APK: `./gradlew :app:assembleDebug`
- APK 위치: `app/build/outputs/apk/debug/app-debug.apk`
- 권한 확인: `"$ANDROID_HOME/build-tools/36.0.0/aapt" dump permissions app/build/outputs/apk/debug/app-debug.apk`
- 서명 확인: `"$ANDROID_HOME/build-tools/36.0.0/apksigner" verify --verbose app/build/outputs/apk/debug/app-debug.apk`
- Windows 전체 게이트: `powershell -NoProfile -File .\scripts\build-windows.ps1 -Target all`; 실행 정책이 막으면 임의 우회하지 말고 사용자 확인을 받는다.

# NOTES

- `kotlin.jvmToolchain(17)`을 사용한다. 빌드 전 `java -version`과 `JAVA_HOME`이 JDK 17을 가리키는지 확인한다.
- 현재 자동 테스트는 정책 객체 2개에 집중한다. `MainActivity`, Bluetooth callback, service, UI에는 `androidTest`가 없다.
- 릴리스 판단용 실기기 검증은 사용자가 사용을 승인한 Android·Windows 11·macOS에서만 수행한다. 장치가 없으면 pairing, movement, click, scroll, drag, key release를 미검증으로 남긴다.
- HID descriptor 변경 뒤 stale pairing cache 가능성을 보고한다. Bluetooth 기록 삭제와 재페어링은 명시적 사용자 승인 뒤에만 수행한다.
