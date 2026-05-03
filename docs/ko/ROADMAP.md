# 로드맵

> English original: [../ROADMAP.md](../ROADMAP.md)

## 목적

솔로/소규모 팀 빌드에서 구현 순서와 범위 경계를 명확하게 유지합니다.

## 현재 상태

PhonePad는 문서 준비가 되어 있으며 전체 제품 구현 전에 Phase 0 기술 스파이크부터 시작해야 합니다.

## 현재 규칙

- Direct HID 경로가 동작하기 전에 v1.1 편의 기능을 만들지 않습니다.
- Bridge Dongle은 별도 결정 게이트가 있는 병렬 스파이크로 유지합니다.
- Play Store 출시는 GitHub 우선 검증 이후로 둡니다.
- 핵심 안정성을 직접 개선하지 않는 한 접근성 전용 대형 작업은 v1.0 밖에 둡니다.

## Phase 0: 제품 및 기술 스파이크, 1-3주차

- 최소 Kotlin Android 앱.
- `BluetoothHidDevice` profile proxy.
- 최소 디스크립터로 `registerApp`.
- Windows 11 페어링과 커서 이동.
- Drag Mode 리포트 테스트.
- macOS 페어링과 핵심 입력.
- Foreground service와 라이프사이클 해제 확인.
- 재연결 측정.
- 협업자 여력이 있으면 Bridge Dongle PoC 병렬 진행.
- Go/No-Go 결정.

## Phase 1: Direct HID Mouse Alpha, 4-6주차

- 안정적인 mouse HID descriptor.
- 페어링과 호스트 목록.
- Trackpad UI v0.
- 이동, 왼쪽 클릭, 오른쪽 클릭, 스크롤.
- Drag Toggle UI와 안전 해제.
- Foreground notification.
- 연결 및 재연결 로그.

## Phase 2: Gesture Pad Beta, 7-9주차

- Keyboard composite report.
- 세 손가락 up/left/right 스와이프 인식.
- macOS와 Windows 프리셋.
- Gesture mapping screen v0.
- 민감도, 스크롤, 햅틱, Drag 설정.
- 베타 테스터 5명.

## Phase 3: Release Candidate, 10-11주차

- UI polish.
- Diagnostics copy flow.
- 한국어 UX 완성.
- 영어와 한국어 README 커버리지.
- GitHub Releases APK와 재현 가능한 CI.
- 데모 GIF 또는 영상.

## Phase 4: v1.0 Release, 12주차

- 베타 피드백 수정.
- 지원 및 미지원 기기 목록.
- iOS, BIOS/UEFI, 네이티브 터치패드 제한 FAQ.
- 공개 GitHub 릴리스.
- Bridge Dongle 스파이크 부록.

## v1.1+

- 전체 gesture mapping editor.
- JSON import/export.
- 네 손가락 제스처.
- Pinch zoom.
- 추가 OS 프리셋.
- Play Store 릴리스.
- F-Droid 평가.
- 선택적 diagnostics sharing.

## 관련 문서

- [QUICK_REF.md](QUICK_REF.md)
- [TEST.md](TEST.md)
- [DEPLOY.md](DEPLOY.md)
- [BRIDGE_DONGLE.md](BRIDGE_DONGLE.md)
