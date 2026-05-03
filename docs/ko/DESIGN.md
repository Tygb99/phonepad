# 디자인

> English original: [../DESIGN.md](../DESIGN.md)

## 목적

한국어 우선 트랙패드 유틸리티를 위한 사용자 대면 화면, 상호작용 상태, 문구 규칙을 정의합니다.

## 현재 상태

PhonePad는 마케팅 앱이 아니라 조용한 도구처럼 느껴져야 합니다. 첫 사용 경험은 페어링과 트랙패드 제어입니다.

## 현재 규칙

- 다크 모드 우선, 라이트 모드 지원.
- 커스텀 트랙패드 표면에 특수 처리가 필요한 경우를 제외하고 Material 3 컴포넌트 사용.
- 트랙패드 메인 화면은 조밀하고 안정적이며 오탭이 어렵게 유지.
- 일반 도구 액션에는 아이콘을, Drag Mode 같은 핵심 안전 액션에는 텍스트 라벨을 사용.
- 네이티브 트랙패드 동작을 과장하는 문구를 쓰지 않음.
- 온보딩 장벽을 피함. 권한이나 페어링 단계가 필요한 순간에만 설명.

## 화면

| 화면 | 목적 | v1.0 |
|---|---|---|
| First Run / Compatibility | Android, Bluetooth, HID 지원 확인. | Yes |
| Permission Guide | Nearby devices와 notification 타이밍 설명. | Yes |
| Pairing | 호스트 OS Bluetooth 페어링 안내. | Yes |
| Host List | 페어링된 호스트와 OS 프리셋 관리. | Yes |
| Trackpad Main | 전체 화면 입력 표면, 연결 상태, Drag 토글. | Yes |
| Settings | 민감도, 스크롤, 햅틱, 화면 켜짐, 토글 위치. | Yes |
| Gesture Mapping | 대표 제스처 3개 편집. | Yes |
| Diagnostics | 호환성 및 연결 로그 복사. | v1.1 |
| About / Open Source | GitHub, 라이선스, 후원 링크. | Yes |

## Trackpad Main 레이아웃

```text
Connected: MacBook Pro      status dot
Dragging banner if active

[ full-height touch surface ]

Host      Settings      Drag / Dragging
```

## 핵심 컴포넌트

| 컴포넌트 | 규칙 |
|---|---|
| 연결 상태 | 메인 화면에서 항상 보임. |
| Drag 토글 | 가장자리 배치에서도 안정적으로 누를 수 있을 만큼 크고, 좌/우 하단 위치 지원. |
| 호스트 전환기 | 전환 전에 버튼을 해제함. |
| 제스처 행 | 제스처 이름, 대상 OS, 매핑된 단축키를 표시함. |
| 권한 카드 | 액션이 막혔을 때만 나타남. |
| 경고 배너 | 버튼 해제 실패 가능성이나 미지원 HID 프로필에 사용. |

## 상호작용 상태

- Connected, reconnecting, disconnected, unsupported, permission missing.
- Drag OFF, Drag ON, release pending, release error.
- Pairing ready, pairing in progress, pairing failed, paired.
- Gesture enabled, gesture disabled, shortcut invalid.

## 문구 규칙

사용:

- "표준 Bluetooth 입력장치처럼 사용"
- "OS별 단축키 기반 제스처"
- "Drag Mode를 켜고 끄며 드래그"
- "이 기기에서는 Android HID Device를 사용할 수 없습니다"

피함:

- "모든 PC에서 작동"
- "진짜 Magic Trackpad"
- "Precision Touchpad"
- "BIOS에서도 사용 가능"
- "원클릭 연결"

## 관련 문서

- [DRAG_MODE.md](DRAG_MODE.md)
- [MOBILE_APP.md](MOBILE_APP.md)
- [SECURITY.md](SECURITY.md)
- [TEST.md](TEST.md)
