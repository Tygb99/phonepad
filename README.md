# PhonePad

> Korean version: [README.ko.md](README.ko.md)

PhonePad is an Android-first input app that turns a phone screen into a Bluetooth trackpad-style controller for Windows and macOS.

The v1.0 path is Android Direct Bluetooth HID Device mode: the host computer should see the phone as a standard Bluetooth mouse/keyboard composite device, without installing a desktop server app.

## Product Scope

- Android API 28+ compatibility check for `BluetoothHidDevice`.
- Pairing with Windows 11 and current macOS as a standard Bluetooth HID input device.
- Cursor movement, left click, right click, vertical and horizontal scroll.
- Explicit Drag Mode toggle instead of long-press drag.
- Three core three-finger gestures mapped to OS-specific keyboard shortcuts.
- No account, no ads, no tracking SDK, no internet permission.
- Korean-first app UI with English README and public GitHub release artifacts.
- Bridge Dongle is a Phase 0 spike, not the v1.0 default path.

## Non-Goals

- iOS Direct Bluetooth HID sender mode.
- Desktop helper/server app.
- Screen mirroring, file transfer, clipboard sync, or remote desktop.
- Native Precision Touchpad or Magic Trackpad emulation in v1.0.
- BIOS/UEFI support in Direct Bluetooth mode.

## Documentation

Start with:

- [docs/INDEX.md](docs/INDEX.md)
- [docs/QUICK_REF.md](docs/QUICK_REF.md)
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- [docs/HID.md](docs/HID.md)
- [docs/DRAG_MODE.md](docs/DRAG_MODE.md)
- [docs/TEST.md](docs/TEST.md)

Planning sources such as PRD drafts and exported chat logs are intentionally ignored by git. Keep product docs in `docs/` and source code in the normal Flutter/Android project tree.

## Android Phase 0 App

This repository now includes a native Kotlin Android spike app for the Direct Bluetooth HID path.

- Package: `com.tygb99.phonepad`
- Minimum Android: API 28
- Current target: Android 16 / API 36
- APK output: `app/build/outputs/apk/debug/app-debug.apk`
- Runtime permissions: Bluetooth connect and advertise only; no `INTERNET` permission.

Device test order:

1. Tap `권한` and allow Nearby devices.
2. Tap `HID 등록`.
3. Tap `검색 허용`.
4. Remove any old `PhonePad` Bluetooth device from the PC, then pair `PhonePad` again. The app sets the phone Bluetooth name to `PhonePad` while entering discoverable mode.
5. Tap `목록 새로고침`.
6. Select the paired host and tap `선택 호스트 연결`.
7. Use the right-side touchpad area.

Build locally:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export ANDROID_SDK_ROOT="$ANDROID_HOME"
./gradlew :app:assembleDebug
```

Install on a connected Android device:

```bash
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
"$ANDROID_HOME/platform-tools/adb" install -r app/build/outputs/apk/debug/app-debug.apk
```

## Repository

Remote target: https://github.com/Tygb99/phonepad.git

The repository has moved from documentation-only into the Phase 0 Android HID spike. Real host pairing and report delivery still need to be proven on physical Android devices and Windows/macOS hosts.
