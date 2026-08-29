# 로드맵

> English original: [../ROADMAP.md](../ROADMAP.md)

## 목적

솔로/소규모 팀 빌드에서 구현 순서와 범위 경계를 명확하게 유지합니다.

## 현재 상태

*최종 점검: 2026-08-30. `main` 최신 커밋: `fb4ad41` (2026-08-07).*

Phase 0은 완료됐고 Phase 1은 대부분 완료됐습니다. 2026-08-07 이후 개발이 중단된 상태입니다.

**완료 및 실기기 검증됨**

- Phase 0 통과. `BluetoothHidDevice` proxy, `registerApp`, Windows 11·macOS 페어링, 커서 이동, 클릭, 스크롤, Drag Mode 리포트, foreground service 라이프사이클, 재연결을 Android 16 Galaxy S23 Ultra에서 검증했습니다. [PHASE0_TEST_REPORT_2026-05-27.md](PHASE0_TEST_REPORT_2026-05-27.md), [ANDROID12_COMPAT_SMOKE_2026-05-31.md](ANDROID12_COMPAT_SMOKE_2026-05-31.md), [WINDOWS_REPAIRING_RESET_SMOKE_2026-06-02.md](WINDOWS_REPAIRING_RESET_SMOKE_2026-06-02.md) 참고.
- Phase 1 대부분 반영됨: 페어링된 컴퓨터만 걸러낸 호스트 목록, 트랙패드 UI, 이동, 좌/우 클릭, 느림/기본/빠름 속도를 가진 연속 스크롤 버튼, 마지막 성공 호스트로의 조용한 자동 재연결, foreground service 수명 범위 제한.
- 계획보다 앞서 들어간 것 2가지: 키보드 HID 등록을 통한 호스트별 한/영 전환([HOST_LANGUAGE_TOGGLE_SMOKE_2026-05-29.md](HOST_LANGUAGE_TOGGLE_SMOKE_2026-05-29.md)), 그리고 Direct HID 대신 Bridge Dongle을 경유해 호스트에 붙는 iOS 앱.
- Bridge Dongle은 PoC 단계를 넘었습니다. ESP32-S3 실물 하드웨어가 있고, 협업자가 2026-07-06에 마우스와 영문 키보드 에뮬레이션 동작을 검증했습니다.
- 라이선스 확정: GPL-3.0-or-later.
- 저장소 구조 확정: `Android/`, `ios/`, `shared/`를 담은 단일 저장소.

**미해결 및 병목**

- `codex/bridge-dongle-v2-status`가 아직 미머지입니다. 협업자가 2026-07-06에 검증하고 머지에 동의했지만, 같은 브랜치를 대상으로 이슈 #3이 등록됐고 아직 열려 있습니다.
- 동글 펌웨어를 로컬에서 다시 플래시할 수 없습니다. 메인테이너의 Mac에서 ESP32-S3 업로드 포트가 뜨지 않아, v2 펌웨어와 BIOS/UEFI 동작을 메인테이너가 직접 검증하지 못한 상태입니다.
- 동글 경유 한글 입력이 미구현입니다(이슈 #4). 영문 키코드는 동작하고 한글은 되지 않습니다.
- BIOS/UEFI 동작은 여전히 미검증이며 주장해서는 안 됩니다.
- Phase 2는 미착수입니다. 세 손가락 제스처, gesture mapping screen이 없고 베타 테스터는 5명이 아니라 1명입니다.

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
