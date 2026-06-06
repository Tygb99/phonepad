# Bridge Dongle

> English original: [../BRIDGE_DONGLE.md](../BRIDGE_DONGLE.md)

## 목적

선택적 하드웨어 Plan B인 phone to BLE dongle to USB HID host input을 추적합니다.

## 현재 상태

Bridge Dongle은 v1.0 앱 전용 MVP에 필요하지 않습니다. 이후 BIOS/UEFI, iOS 송신자, 더 안정적인 호스트 호환성을 열 수 있는 Phase 0 병렬 스파이크입니다.

## 현재 규칙

- 동글 작업은 Direct HID 릴리스 범위와 분리합니다.
- 앱 전용 MVP를 하드웨어에 막히게 하지 않습니다.
- 펌웨어, BLE 커스텀 프로토콜, 인클로저/생산은 스파이크 이후 작업으로 취급합니다.
- 스파이크가 입증하기 전에는 BIOS 또는 iOS 지원을 주장하지 않습니다.

## 후보 아키텍처

```text
Phone App
  Touch UI, Drag Mode, gesture mapping
      |
      | BLE custom packets
      v
Bridge Dongle
  BLE receiver, USB HID mouse/keyboard firmware
      |
      | USB HID
      v
Windows / macOS / possible BIOS or UEFI
```

## Prior Art: ESPRemoteControl

참고 링크:

- Reddit discussion: [Turn your iPhone into a wireless BLE keyboard + trackpad for any device with a USB port](https://www.reddit.com/r/esp32/comments/1qmqez2/turn_your_iphone_into_a_wireless_ble_keyboard/?tl=ko)
- GitHub repo: [KoStard/ESPRemoteControl](https://github.com/KoStard/ESPRemoteControl)

관찰된 아키텍처:

```text
iPhone SwiftUI app
  Keyboard input, trackpad area, gestures
      |
      | Custom Bluetooth LE command protocol
      v
ESP32-S3 firmware
  BLE receiver, command translation
      |
      | TinyUSB USB HID
      v
Target device
  Standard USB keyboard and mouse
```

PhonePad에 중요한 이유:

- phone -> BLE -> ESP32-S3 -> USB HID bridge 패턴을 검증합니다.
- ESP32-S3 native USB, TinyUSB, NimBLE을 사용하며 스파이크 출발점으로 유용합니다.
- PhonePad가 신경 쓰는 동일한 넓은 입력 범주를 보여줍니다: 키보드 입력, 커서 이동, 왼쪽 클릭, 오른쪽 클릭, 스크롤, 기능 키, 키 조합.
- 앱 전용 Bluetooth HID가 충분하지 않은 기기를 USB HID로 커버할 수 있다는 제품 가정을 뒷받침합니다.
- 드롭인 의존성보다는 prior art와 스파이크 참고 자료로 다루는 편이 낫습니다. 해당 repo는 초기 단계로 보이며 릴리스 산출물이 없고 iOS 우선입니다.

PhonePad 전용 후속 확인:

- 커스텀 BLE command format이 Drag Mode button-hold semantics에 충분한지 확인.
- 참고 프로토콜이 안전 해제를 이미 보장하지 않는다면 명시적인 `release_all_buttons` control command 추가.
- PhonePad 같은 포인터 이동으로 BLE-to-USB HID 지연과 report-rate 안정성 측정.
- 최종 동글 방향에 따라 Android가 같은 BLE peripheral/central role을 깔끔하게 사용할 수 있는지 확인.
- BIOS/UEFI 또는 iOS 지원을 주장하기 전에 보드, 펌웨어 스택, 호스트 OS, USB HID 디스크립터 상세를 기록.

## 스파이크 성공 기준

| 영역 | 성공 기준 |
|---|---|
| Board selection | MCU 보드 1개 선택 및 문서화. |
| USB HID sample | Windows/macOS가 드라이버 없이 mouse/keyboard 인식. |
| BLE link | phone to dongle이 5미터에서 안정적. |
| Mouse move | 휴대폰 입력이 호스트 커서를 움직임. |
| Report rate | 60Hz report path가 가능함. |
| Latency | 단순 테스트에서 P95 30ms 미만. |
| BIOS/UEFI | 이를 주장한다면 최소 PC 1대가 USB mouse 인식. |
| iOS possibility | iPhone BLE packet 가능성에 대한 별도 노트. |

## 패킷 초안

BLE GATT UUID, v1 마우스 호환 패킷, v2 `ReleaseAll`, `KeyChord`, host language-toggle 패킷은 [DONGLE_PROTOCOL.md](DONGLE_PROTOCOL.md)를 참고합니다.

## 결정 게이트

다음 중 하나 이상이 참일 때만 스파이크 이후로 진행합니다.

- Direct Android HID가 너무 많은 대상 기기에서 실패.
- BIOS/UEFI 사용이 명확한 제품 요구사항이 됨.
- iOS sender mode가 하드웨어를 정당화할 만큼 중요해짐.
- 협업자 펌웨어 역량이 v1.0을 늦추지 않고 가능함.

## 관련 문서

- [API.md](API.md)
- [DONGLE_PROTOCOL.md](DONGLE_PROTOCOL.md)
- [HID.md](HID.md)
- [ROADMAP.md](ROADMAP.md)
- [TEST.md](TEST.md)
