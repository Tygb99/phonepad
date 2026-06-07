# Windows Development

> Korean version: [ko/WINDOWS_DEV.md](ko/WINDOWS_DEV.md)

## Purpose

Make PhonePad buildable from a clean Windows PowerShell environment without requiring macOS/Homebrew paths.

## Requirements

- Windows 10 or Windows 11.
- PowerShell 5.1 or newer.
- JDK 17.
- Android SDK with:
  - `platforms;android-36`
  - `build-tools;36.0.0`
  - `platform-tools`

## Quick Setup

From the repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Android\scripts\setup-windows-dev.ps1
```

If JDK 17 or Android SDK command-line tools are missing, run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Android\scripts\setup-windows-dev.ps1 -InstallMissing -AcceptAndroidLicenses
```

Add `-PersistUserEnv` if you want the script to save `JAVA_HOME`, `ANDROID_HOME`, and `ANDROID_SDK_ROOT` for future PowerShell windows:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Android\scripts\setup-windows-dev.ps1 -InstallMissing -AcceptAndroidLicenses -PersistUserEnv
```

The setup script writes `Android/local.properties` with your local Android SDK path. That file is ignored by git.

## Build

Run all local development checks:

```powershell
.\Android\scripts\build-windows.ps1 -Target all
```

Run a narrower target:

```powershell
.\Android\scripts\build-windows.ps1 -Target test
.\Android\scripts\build-windows.ps1 -Target debug
.\Android\scripts\build-windows.ps1 -Target lint
```

The debug APK is written to:

```text
Android\app\build\outputs\apk\debug\app-debug.apk
```

## Install On Device

Connect an Android device with USB debugging enabled, then run:

```powershell
& "$env:ANDROID_HOME\platform-tools\adb.exe" install -r Android\app\build\outputs\apk\debug\app-debug.apk
```

## Notes

- If Android Studio is already installed, the default SDK path is usually `%LOCALAPPDATA%\Android\Sdk`.
- If the setup script installs command-line tools directly, it also installs the required API 36 platform, build tools, and platform tools through `sdkmanager`.
- During Windows lint runs, `build-windows.ps1` temporarily hides the ignored `local.properties` file and supplies the SDK path through `ANDROID_HOME` to avoid Android Lint flagging the local Windows SDK path.
- The build script keeps the Phase 0 release gate by checking that `android.permission.INTERNET` is absent from source XML and the built debug APK.
