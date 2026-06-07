# PhonePad iOS 송신기

> English original: [README.md](README.md)

PhonePad iOS는 Bridge Dongle 경로를 위한 SwiftUI + CoreBluetooth 송신기입니다.

```text
iPhone 앱 -> BLE GATT 쓰기 -> ESP32-S3 브리지 동글 -> USB HID -> 호스트 컴퓨터
```

이 경로는 Android Direct HID MVP와 별개입니다. iOS Simulator에서는 앱을 컴파일할 수 있지만 Bluetooth LE 입력 전달은 검증할 수 없습니다. 실제 동작 검증에는 다음이 필요합니다.

- Xcode에서 선택된 실제 iPhone.
- 로컬에 설정된 Apple 개발자 팀 서명.
- `shared/firmware/bridge-dongle/BLETouchMouse/BLETouchMouse.ino`로 플래시한 ESP32-S3 동글.
- 동글에 USB로 연결된 Mac, Windows PC, 또는 BIOS/UEFI 대상.

체크인된 프로젝트는 개인 `DEVELOPMENT_TEAM`, `xcuserdata`, 로컬 서명 상태를 의도적으로 제외합니다. 실제 iPhone에 설치하기 전 Xcode에서 서명을 로컬로 설정하고, 개인 서명 변경사항은 커밋하지 마세요.

## 빌드

다음을 엽니다.

```text
ios/PhonePad/PhonePad.xcodeproj
```

`PhonePad` scheme을 사용합니다.

## 현재 입력 범위

- 포인터 이동, 클릭, 더블 클릭, 오른쪽 클릭, 가운데 클릭, 스크롤.
- 단일 키 키보드 패킷.
- `ReleaseAll`, `KeyChord`, 호스트 프로필별 언어 전환을 위한 protocol v2 안전 패킷.
- Mac 언어 전환은 `Control + Space`를 사용합니다.
- Windows 언어 전환 후보는 `LANG1`과 `RightAlt`입니다.

여기에는 완전한 한국어 텍스트 입력이 구현되어 있지 않습니다. 이후 한국어 입력은 BLE로 조합된 한글 음절을 보내는 방식이 아니라, 호스트 IME가 켜진 상태에서 물리 QWERTY 키코드를 보내는 방식이어야 합니다.
