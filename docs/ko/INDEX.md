# 문서 색인

> English original: [../INDEX.md](../INDEX.md)

## 목적

PhonePad 엔지니어링, 디자인, 테스트, 릴리스, 협업 문서의 탐색 허브입니다.

## 현재 상태

PhonePad는 문서화와 Phase 0 스파이크 단계입니다. 비공개 기획 파일을 커밋하지 않고도 구현을 시작할 수 있도록 문서를 구성했습니다.

## 현재 규칙

- `README.md`와 `CLAUDE.md`는 저장소 루트에 둡니다.
- 제품, 아키텍처, 테스트, 릴리스 문서는 `docs/` 아래에 둡니다.
- 참고 템플릿과 AI 프롬프트는 `docs/ref/` 아래에 둡니다.
- PRD 초안과 채팅 내보내기는 로컬에만 두고 git에서 제외합니다.

## 핵심 문서

| 문서 | 사용할 때 |
|---|---|
| [QUICK_REF.md](QUICK_REF.md) | 짧은 제품 계약이 필요할 때. |
| [ARCHITECTURE.md](ARCHITECTURE.md) | 앱 레이어, 라이프사이클, 네이티브 연동을 바꿀 때. |
| [HID.md](HID.md) | Bluetooth HID, 디스크립터, 페어링, 권한을 작업할 때. |
| [DRAG_MODE.md](DRAG_MODE.md) | 드래그 동작이나 라이프사이클 안전 해제를 건드릴 때. |
| [DB.md](DB.md) | 로컬 설정, 호스트 기록, 제스처 매핑을 추가할 때. |
| [API.md](API.md) | Flutter와 Android 네이티브 또는 동글 패킷을 연결할 때. |
| [DESIGN.md](DESIGN.md) | 화면, 컴포넌트, 상태, 문구를 만들 때. |
| [MOBILE_APP.md](MOBILE_APP.md) | Android 빌드, 권한, 서비스, 스토어 릴리스를 바꿀 때. |
| [SECURITY.md](SECURITY.md) | 권한, 로깅, 텔레메트리, 데이터 처리를 추가할 때. |
| [TEST.md](TEST.md) | 호환성, 드래그, 재연결, 릴리스 게이트를 검증할 때. |
| [ROADMAP.md](ROADMAP.md) | 단계 순서와 범위 경계가 필요할 때. |
| [DEPLOY.md](DEPLOY.md) | GitHub Releases 또는 Play Store를 준비할 때. |
| [CONTRIBUTING.md](CONTRIBUTING.md) | GitHub 이슈, PR, 스파이크로 협업할 때. |
| [BRIDGE_DONGLE.md](BRIDGE_DONGLE.md) | 하드웨어 Plan B를 검증할 때. |

## 관련 문서

- [../../README.ko.md](../../README.ko.md)
- [../../CLAUDE.ko.md](../../CLAUDE.ko.md)
- [ref/README.md](ref/README.md)
