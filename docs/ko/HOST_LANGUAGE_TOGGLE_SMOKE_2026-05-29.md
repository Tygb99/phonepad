# 호스트 한영 전환 스모크 테스트 - 2026-05-29

> English original: [../HOST_LANGUAGE_TOGGLE_SMOKE_2026-05-29.md](../HOST_LANGUAGE_TOGGLE_SMOKE_2026-05-29.md)

## 목적

이 문서는 PhonePad의 호스트별 keyboard HID 경로에서 macOS 한영 전환이 성공한 스모크 테스트 결과를 기록합니다.

## 결과

통과. macOS Bluetooth 페어링을 새로 만들어 호스트가 PhonePad를 mouse + keyboard HID 장치로 다시 인식한 뒤, PhonePad의 `한영` 버튼으로 Mac 입력 소스가 반복 전환됐습니다.

```text
ABC -> 2-Set Korean -> ABC -> 2-Set Korean
```

## 테스트 환경

| 항목 | 값 |
|---|---|
| 테스트 날짜 | 2026-05-29 |
| 문서화 날짜 | 2026-06-02 |
| 구현 커밋 | `7e77f4f fix: send mac language toggle as staged chord` |
| 포함 릴리스 | `v0.2.0-phase0` |
| Android 기기 | Samsung Galaxy S23 Ultra, `SM-S918N` |
| Android 버전 | Android 16 / API 36 |
| 호스트 | `yong의 Mac mini` |
| 호스트 OS 기준 | macOS 26.5 |
| PhonePad 호스트 preset | `Mac: Control + Space` |

## 확인 항목

| 확인 | 결과 | 근거 |
|---|---|---|
| macOS 로컬 단축키 기준선 | 통과 | Mac에서 직접 `Control + Space` 입력 시 입력 소스 전환 확인. |
| PhonePad keyboard report 경로 | 통과 | PhonePad가 Mac 한영 전환 stroke를 `language_toggle preset=mac`으로 전송. |
| staged chord 동작 | 통과 | Mac preset은 Control을 먼저 내리고, Control + Space를 보낸 뒤 keyboard key를 모두 해제. |
| descriptor 변경 뒤 새 페어링 | 필요 | 기존 macOS 페어링은 PhonePad를 mouse-only로 계속 볼 수 있음. macOS Bluetooth에서 PhonePad를 삭제하고 다시 페어링해 keyboard collection을 갱신. |
| 호스트 측 결과 | 통과 | macOS 입력 소스가 `ABC -> 2-Set Korean -> ABC -> 2-Set Korean`으로 전환됨. |

## 최종 동작

- macOS는 한 번의 동시 리포트가 아니라 staged `Control + Space` chord를 사용합니다.
- 순서는 modifier-down, modifier 포함 key-down, all-key-up입니다.
- PhonePad 로그에는 전송이 보이는데 Mac이 반응하지 않으면, macOS Bluetooth에서 PhonePad를 삭제하고 앱의 새 PC 연결 흐름으로 다시 페어링합니다.
- keyboard HID descriptor가 추가되기 전의 오래된 페어링은 호스트 쪽에서 mouse-only로 남을 수 있으므로 이 안내가 중요합니다.

## 범위 경계

- 이 문서는 macOS 한영 전환 성공 경로만 다룹니다.
- Windows 한영 전환 정책은 기본 `Keyboard LANG1`, 호스트별 fallback `Right Alt`입니다.
- Windows 호스트에서의 한영 전환 성공 여부는 별도 검증 시 별도 리포트로 기록합니다.

## 관련 문서

- [HID.md](HID.md)
- [TEST.md](TEST.md)
- [MOBILE_APP.md](MOBILE_APP.md)
