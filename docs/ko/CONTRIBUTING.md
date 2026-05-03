# 기여하기

> English original: [../CONTRIBUTING.md](../CONTRIBUTING.md)

## 목적

제품, Android HID, 선택적 동글 작업이 병렬로 진행되는 초기 PhonePad 작업의 협업 규칙을 정합니다.

## 현재 상태

프로젝트는 GitHub 협업의 시작 단계입니다. 초기 기여는 입력 경로를 입증하고 근거를 기록하는 데 집중해야 합니다.

## 현재 규칙

- 하나의 주요 목적을 가진 작은 PR을 엽니다.
- Bluetooth/HID 변경에는 기기와 호스트 OS 근거를 포함합니다.
- 로컬 PRD 초안, 채팅 내보내기, secrets, 생성된 비공개 기획 노트를 커밋하지 않습니다.
- v1.0 범위에 데스크톱 헬퍼 앱을 추가하지 않습니다.
- 동글 작업은 별도 스파이크 경계 뒤에 둡니다.

## 좋은 첫 작업

- 최소 Kotlin HID 등록 스파이크.
- 마우스 이동 리포트 증명.
- Windows 11 페어링 노트.
- macOS 페어링 노트.
- Drag Mode ON/OFF 리포트 테스트.
- Android 최소 권한 테스트.
- README 지원 매트릭스 템플릿.

## PR 체크리스트

- 어떤 기기와 호스트 OS를 테스트했나요?
- 이 변경이 Drag Mode 해제 동작에 영향을 주나요?
- Manifest가 여전히 `INTERNET`을 피하나요?
- 권한이 문서화되어 있나요?
- 새 로그가 로컬 전용이며 원시 입력 기록을 포함하지 않나요?
- 동작이나 범위가 바뀔 때 문서가 업데이트되었나요?

## 이슈 라벨

| 라벨 | 의미 |
|---|---|
| `hid` | Bluetooth HID 등록, 디스크립터, 리포트. |
| `drag-mode` | Drag 토글, 안전 해제, 버튼 고착 위험. |
| `android` | Android 라이프사이클, 서비스, 권한. |
| `windows` | Windows 호스트 동작. |
| `macos` | macOS 호스트 동작. |
| `dongle-spike` | Bridge Dongle 조사. |
| `docs` | 문서 전용. |

## 관련 문서

- [ARCHITECTURE.md](ARCHITECTURE.md)
- [HID.md](HID.md)
- [DRAG_MODE.md](DRAG_MODE.md)
- [BRIDGE_DONGLE.md](BRIDGE_DONGLE.md)
