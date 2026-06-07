# Windows 개발

> English version: [../WINDOWS_DEV.md](../WINDOWS_DEV.md)

## 목적

macOS/Homebrew 경로 없이 Windows PowerShell 환경에서도 PhonePad를 빌드하고 테스트할 수 있게 합니다.

## 요구 사항

- Windows 10 또는 Windows 11
- PowerShell 5.1 이상
- JDK 17
- Android SDK
  - `platforms;android-36`
  - `build-tools;36.0.0`
  - `platform-tools`

## 빠른 설정

저장소 루트에서 실행합니다.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Android\scripts\setup-windows-dev.ps1
```

JDK 17 또는 Android SDK command-line tools가 없으면 다음 명령으로 설치까지 진행할 수 있습니다.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Android\scripts\setup-windows-dev.ps1 -InstallMissing -AcceptAndroidLicenses
```

새 PowerShell 창에서도 환경 변수를 계속 쓰고 싶으면 `-PersistUserEnv`를 추가합니다.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Android\scripts\setup-windows-dev.ps1 -InstallMissing -AcceptAndroidLicenses -PersistUserEnv
```

설정 스크립트는 로컬 Android SDK 경로를 담은 `Android/local.properties`를 생성합니다. 이 파일은 git에 커밋되지 않습니다.

## 빌드

전체 로컬 개발 검증을 실행합니다.

```powershell
.\Android\scripts\build-windows.ps1 -Target all
```

필요한 대상만 좁혀 실행할 수도 있습니다.

```powershell
.\Android\scripts\build-windows.ps1 -Target test
.\Android\scripts\build-windows.ps1 -Target debug
.\Android\scripts\build-windows.ps1 -Target lint
```

디버그 APK는 다음 위치에 생성됩니다.

```text
Android\app\build\outputs\apk\debug\app-debug.apk
```

## 기기에 설치

USB 디버깅이 켜진 Android 기기를 연결한 뒤 실행합니다.

```powershell
& "$env:ANDROID_HOME\platform-tools\adb.exe" install -r Android\app\build\outputs\apk\debug\app-debug.apk
```

## 참고

- Android Studio가 이미 설치되어 있다면 기본 SDK 경로는 보통 `%LOCALAPPDATA%\Android\Sdk`입니다.
- 설정 스크립트가 command-line tools를 직접 설치할 경우 `sdkmanager`로 API 36 platform, build tools, platform tools도 함께 설치합니다.
- Windows lint 실행 중에는 Android Lint가 로컬 Windows SDK 경로를 문제로 보지 않도록 `build-windows.ps1`이 git에서 제외된 `local.properties`를 잠시 숨기고 `ANDROID_HOME`으로 SDK 경로를 제공합니다.
- 빌드 스크립트는 Phase 0 릴리스 게이트를 유지하기 위해 source XML과 빌드된 debug APK에 `android.permission.INTERNET`이 없는지 확인합니다.
