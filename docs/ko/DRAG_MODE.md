# Drag Mode

> English original: [../DRAG_MODE.md](../DRAG_MODE.md)

## 목적

휴대폰 터치 표면에서 드래그 동작을 안정적으로 만들고 호스트에 마우스 버튼 고착 상태를 남기지 않도록 명시적인 Drag Mode 토글을 정의합니다.

## 현재 상태

길게 누르기 드래그는 기본값이 아닙니다. PhonePad는 켤 때 left-button-down을 보내고 끌 때 left-button-up을 보내는 보이는 Drag 토글을 사용합니다.

## 현재 규칙

- Drag Mode 상태는 런타임 전용이며 SQLite에 저장하면 안 됩니다.
- 앱 시작은 항상 Drag Mode OFF로 시작합니다.
- Drag Mode ON은 한 손가락 tap-to-click과 세 손가락 제스처 인식을 비활성화합니다.
- 모든 안전 이벤트는 호스트 변경, 등록 해제, 종료 전에 all-buttons-up을 시도합니다.
- 릴리스 테스트에서 왼쪽 버튼 고착 건수는 0이어야 합니다.

## 상태 머신

```text
off
  -> enabling
  -> on
  -> release_pending
  -> off

any state
  -> error
  -> off after user recovery or reconnect
```

## 사용자 흐름

| 단계 | 사용자 액션 | 시스템 액션 |
|---|---|---|
| 1 | `Drag` 탭 | 상태를 `enabling`으로 설정하고 left-button-down 전송. |
| 2 | 한 손가락 이동 | 왼쪽 버튼 bit가 설정된 이동 리포트 전송. |
| 3 | `Dragging` 탭 | 상태를 `release_pending`으로 설정하고 all-buttons-up 전송. |
| 4 | 해제 성공 | 상태를 `off`로 설정. |
| 5 | 해제가 실패했을 수 있음 | 경고와 복구 안내 표시. |

## 안전 이벤트

다음 상황에서는 항상 `releaseAllMouseButtons()`를 호출합니다.

- 호스트 전환.
- Bluetooth 연결 해제.
- HID 등록 해제.
- 연결 유지가 불가능한 앱 백그라운드 전환.
- 화면 잠금.
- Android가 허용하는 프로세스 종료 훅.
- Foreground service 중지.

## UI 상태

| 상태 | 시각 요소 | 햅틱 | 입력 처리 |
|---|---|---|---|
| OFF | Outline `Drag` 컨트롤 | 없음 | 일반 트랙패드 입력. |
| ON | Filled `Dragging` 컨트롤과 작은 상단 배너 | 짧은 이중 진동 | 이동 시 왼쪽 버튼 유지. |
| Release pending | 진행 표시가 있는 비활성 컨트롤 | 없음 | 새 드래그 액션 무시. |
| Error | 빨간 경고 상태 | 강한 진동 | 재연결 또는 수동 클릭/Escape 안내. |

## 승인 기준

1. ON은 left-button-down 리포트를 1회 보냅니다.
2. ON 상태의 이동은 왼쪽 버튼 bit를 유지합니다.
3. OFF는 최소 1회의 all-buttons-up 리포트를 보냅니다.
4. 안전 이벤트는 all-buttons-up을 보냅니다.
5. 앱을 다시 열어도 Drag Mode ON을 복원하지 않습니다.
6. 반복 ON/OFF 사이클이 테스트 기기/호스트 쌍마다 최소 30회 통과합니다.

## 관련 문서

- [HID.md](HID.md)
- [TEST.md](TEST.md)
- [DESIGN.md](DESIGN.md)
- [API.md](API.md)
