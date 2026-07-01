# 동글 프로토콜

> English original: [../DONGLE_PROTOCOL.md](../DONGLE_PROTOCOL.md)

## 목적

선택적 PhonePad Bridge Dongle 경로의 BLE 패킷 계약을 정의합니다.

```text
PhonePad app -> BLE GATT write -> ESP32-S3 dongle -> USB HID mouse/keyboard
```

이 프로토콜은 Android Direct HID MVP를 대체하지 않습니다. iOS 송신자와 하드웨어 스파이크 경로를 지원합니다.

## BLE GATT

Service UUID:

```text
7c2d2b6a-8f3e-4c6f-8d6f-01b0f4dd1000
```

Writable characteristic UUID:

```text
7c2d2b6a-8f3e-4c6f-8d6f-01b0f4dd1001
```

Status characteristic UUID:

```text
7c2d2b6a-8f3e-4c6f-8d6f-01b0f4dd1002
```

Status characteristic은 read와 notify를 지원합니다. 4바이트 값은 다음 형식입니다.

```text
byte 0: status code
byte 1: packet type
byte 2: sequence id
byte 3: detail
```

Status code:

| Value | 의미 |
|---|---|
| `0x01` | Ready |
| `0x02` | BLE connected |
| `0x03` | BLE disconnected |
| `0x10` | Packet processed |
| `0x11` | Duplicate sequenced packet ignored |
| `0x80` | Malformed packet rejected |
| `0x81` | Unknown packet rejected |

마우스 이동은 `writeWithoutResponse`를 사용할 수 있습니다. 키보드, chord, release/safety 명령은 characteristic이 지원한다면 `writeWithResponse`를 우선 사용합니다.

## v1 마우스 패킷

기존 마우스 패킷은 호환성을 위해 정확히 4바이트로 유지합니다.

```text
byte 0: dx, int8
byte 1: dy, int8
byte 2: buttons bitmask, bit 0 left, bit 1 right, bit 2 middle, bit 3 back, bit 4 forward
byte 3: wheel, int8
```

정확히 4바이트 값은 항상 v1 마우스 입력으로 처리합니다. 알 수 없는 4바이트가 아닌 명령은 마우스 입력으로 흘려보내지 않고 status characteristic으로 거부 상태를 보고합니다.

## v1 키보드 단일 키 패킷

단일 키 keyboard packet은 같은 writable characteristic을 사용합니다.

```text
byte 0: 0x03
byte 1: action, 0 tap, 1 press, 2 release
byte 2: Arduino USBHIDKeyboard key code
```

iOS 앱은 이 패킷이 정확히 4바이트가 되면 padding을 붙여 충돌을 피합니다.

## v2 Safety And Chord 패킷

### ReleaseAll

```text
byte 0: 0x10
byte 1: sequence id
```

펌웨어 동작:

- left, right, middle mouse button을 모두 해제합니다.
- keyboard `releaseAll()`을 호출합니다.
- BLE disconnect, app background, manual disconnect, host switch, chord 실패 복구에 사용합니다.

### KeyChord

```text
byte 0: 0x11
byte 1: sequence id
byte 2: modifier mask
byte 3: key count, max 6
byte 4...: Arduino USBHIDKeyboard key code list
```

Modifier mask:

| Bit | Modifier |
|---|---|
| `0x01` | Left Control |
| `0x02` | Left Shift |
| `0x04` | Left Alt |
| `0x08` | Left GUI |
| `0x10` | Right Control |
| `0x20` | Right Shift |
| `0x40` | Right Alt |
| `0x80` | Right GUI |

펌웨어 동작:

1. modifier를 누릅니다.
2. 나열된 key를 차례로 tap합니다.
3. modifier를 해제합니다.
4. 마지막 안전 정리로 keyboard `releaseAll()`을 호출합니다.

### LanguageToggle

```text
byte 0: 0x12
byte 1: sequence id
byte 2: host profile
```

Host profile:

| Value | Host profile | Sent behavior |
|---|---|---|
| `0x01` | Mac | `Control + Space` |
| `0x02` | Windows LANG1 | `LANG1` key code `0x90` |
| `0x03` | Windows Right Alt | Right Alt key code `0x86` |

Mac `Control + Space`는 PhonePad 호스트 한영 전환 스모크 테스트에서 확인된 로컬 기준입니다. Windows는 Korean IME 설정 차이가 있으므로 호스트별로 `LANG1`과 `RightAlt`를 모두 검증해야 합니다.

### ExtendedMouse

```text
byte 0: 0x13
byte 1: sequence id
byte 2: dx, int8
byte 3: dy, int8
byte 4: buttons bitmask, bit 0 left, bit 1 right, bit 2 middle, bit 3 back, bit 4 forward
byte 5: wheel, int8
byte 6: pan, int8
```

이 패킷은 기존 v1 4바이트 마우스 패킷 호환성을 유지하면서 horizontal scroll(`pan`)과 추가 mouse button 2개를 엽니다.

Sequenced v2 packet(`0x10`, `0x11`, `0x12`, `0x13`)은 packet type과 sequence id가 같은 즉시 중복 패킷을 무시합니다.

## 검증 체크리스트

- iPhone이 ESP32-S3 service를 scan/connect한다.
- 포인터 이동은 v1 4바이트 패킷으로 계속 동작한다.
- left click, right click, middle click, back, forward, vertical scroll, horizontal scroll이 송신자가 노출하는 범위에서 계속 동작한다.
- Status read/notify가 ready, connected, processed, duplicate, malformed, unknown-packet 상태를 보고한다.
- `ReleaseAll`이 모든 mouse button과 keyboard key를 해제한다.
- app background 또는 manual disconnect 전에 가능한 경우 `ReleaseAll`을 보낸다.
- Mac profile이 `Control + Space`로 입력 소스를 전환한다.
- Windows profile은 `LANG1`과 `RightAlt`를 모두 테스트한다.
- 동글의 BLE disconnect도 같은 release cleanup을 호출한다.
