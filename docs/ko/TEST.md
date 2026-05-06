# 테스트 계획

> English original: [../TEST.md](../TEST.md)

## 목적

PhonePad v1.0을 신뢰하기 전에 필요한 호환성, 입력, 라이프사이클, Drag Mode, 릴리스 테스트를 정의합니다.

## 현재 상태

Android Bluetooth HID 지원은 기기와 제조사에 따라 다르므로 테스트는 Phase 0 스파이크부터 시작합니다.

## 현재 규칙

- 에뮬레이터만이 아니라 실제 Android 하드웨어에서 테스트합니다.
- 기기 모델, Android 버전, 호스트 OS 버전, Bluetooth 어댑터, 앱 커밋을 기록합니다.
- 모든 Drag Mode 실패는 무해하다고 입증될 때까지 P0입니다.
- 재연결 지표는 단발성 성공이 아니라 반복 실행을 사용해야 합니다.
- 릴리스 산출물은 `INTERNET`이 없는 깨끗한 manifest 확인이 필요합니다.

## Direct HID 매트릭스

| 영역 | 최소 대상 | 필수 확인 |
|---|---|---|
| Android | Pixel 기기 1대 | HID profile, register, sendReport. |
| Android | Samsung Galaxy 기기 1대 | 위와 동일. |
| Android | 저가형/타 제조사 기기 1대 | 성공 또는 명확한 미지원 UX. |
| Windows | 최신 Windows 11 | 페어링, 이동, 클릭, 스크롤, 드래그, 단축키. |
| Windows | Windows 10 22H2 | Best-effort 노트. |
| macOS | 최신 안정 버전 | 페어링, 이동, 클릭, 스크롤, 드래그, 단축키. |
| Bluetooth | 내장 + USB 어댑터 1개 | 안정성 노트. |
| Lifecycle | 화면 꺼짐, 백그라운드, 포그라운드, 프로세스 종료 | 해제와 재연결. |

## Drag Mode 프로토콜

각 Android/호스트 쌍마다 최소 30회 반복:

1. 호스트에 연결합니다.
2. Drag Mode를 ON으로 전환합니다.
3. 3초 동안 이동합니다.
4. 파일/창/텍스트 선택 드래그가 유지되는지 확인합니다.
5. Drag Mode를 OFF로 전환합니다.
6. 왼쪽 버튼 고착 상태가 없는지 확인합니다.
7. 안전 이벤트를 반복합니다: 연결 해제, 호스트 전환, 앱 백그라운드, 화면 꺼짐.

통과 기준:

- ON/OFF 성공률 95% 이상.
- 왼쪽 버튼 고착: 0건.
- 안전 이벤트 고착 상태: 0건.
- Drag ON 시각 상태 불일치: 0건.

## 재연결 프로토콜

각 Android/호스트 쌍마다 20회 반복:

1. 연결합니다.
2. 화면을 끕니다.
3. 30초 대기합니다.
4. 포그라운드로 돌아옵니다.
5. 재연결 성공 여부와 시간을 기록합니다.

통과 기준:

- 성공률 90% 이상.
- 성공한 재연결의 P95 시간이 10초 이내.

## Phase 0 피드백 확인

- 새 PC 흐름: 새 bonded host가 앱 복귀 후 전환 대상이 되고, 추가 수동 탭 없이 연결 시도를 진행하는지 확인합니다.
- 멀티 페어링: 이전/다음은 전환 대상만 바꾸고, 실제 전환은 `호스트 연결/전환`에서만 일어나는지 확인합니다.
- 더블 탭 드래그: 기본 OFF, 설정 저장, 탭-탭-홀드 이동, finger-up/cancel 시 all-buttons-up을 확인합니다.
- 스크롤 속도: 느림/기본/빠름 프리셋을 확인하고 기본값이 macOS hold-scroll 과가속을 다시 만들지 않는지 확인합니다.
- Windows 실패 로그: Windows에서 연결 중에 머물거나 연결 안 됨이 되면 logcat의 `PhonePad` `host_diag`, `connect_request_*`, `connect_timeout_*` 라인을 수집합니다.

## 릴리스 게이트 체크리스트

- Manifest에 `INTERNET` 없음.
- 필수 권한 문서화.
- GitHub Actions가 release APK 생성.
- README에 테스트 및 미지원 기기 목록.
- Drag Mode 테스트 통과.
- Windows 11과 macOS가 핵심 입력 테스트 통과.
- 알려진 제한사항 문서화.

## 관련 문서

- [HID.md](HID.md)
- [DRAG_MODE.md](DRAG_MODE.md)
- [MOBILE_APP.md](MOBILE_APP.md)
- [DEPLOY.md](DEPLOY.md)
