# Phase 0 테스트 리포트 - 2026-05-27

> English original: [../PHASE0_TEST_REPORT_2026-05-27.md](../PHASE0_TEST_REPORT_2026-05-27.md)

## 요약

이 문서는 2026-05-27에 실행한 ADB 기반 Phase 0 검증 결과를 기록합니다.

결과: 테스트한 Galaxy S23 Ultra와 페어링된 macOS 호스트 조합에서 ADB로 확인 가능한 Drag Mode, 명시적 재연결, 라이프사이클 복구 항목은 통과했습니다. 단, ADB는 PC 화면을 직접 볼 수 없으므로 실제 호스트 커서 이동, 파일/창/텍스트 드래그 유지 여부는 PC에서 사람이 확인해야 합니다.

## 환경

| 항목 | 값 |
|---|---|
| Android 기기 | Samsung Galaxy S23 Ultra, `SM-S918N` |
| Android 버전 | Android 16, API 36 |
| 앱 패키지 | `com.tygb99.phonepad` |
| 앱 버전 | `0.1.16-phase0`, `versionCode=17` |
| 앱 마지막 업데이트 | `2026-05-07 22:20:48` |
| 저장소 브랜치 | `main` |
| 저장소 커밋 | `c253b11` (`c253b119e1eb1c8dd17f58d2593566a293bd2a4b`) |
| 페어링된 호스트 표시명 | `yong의 Mac mini` |
| ADB transport | Wireless ADB, `adb-R3CW10EXD5J-fyjrId._adb-tls-connect._tcp` |

## 범위

ADB로 확인한 항목:

- `uiautomator` 기반 PhonePad 포그라운드 실행 및 UI 상태.
- UI 문구와 `PhonePad` logcat 기반 HID 연결 상태.
- `adb shell input` 기반 버튼/터치패드 조작.
- Drag Mode 최종 상태와 앱 내부 전송 카운터.
- 로컬 타이밍과 `callback_state_연결됨` 로그 기반 재연결 시간.
- 백그라운드, 화면 꺼짐, 프로세스 종료 후 복구.

ADB로 확인하지 못한 항목:

- 실제 호스트 커서 위치.
- 실제 호스트의 파일, 창, 텍스트 선택 드래그 유지 여부.
- Android 쪽 release/UI/log 증거를 넘어선 PC 데스크톱의 버튼 고착 여부.

## 결과

| 시나리오 | 방법 | 결과 | 근거 |
|---|---|---|---|
| 초기 앱 복구 | `am force-stop`, 앱 재실행, HID 재연결 대기 | 통과 | UI가 HID 등록 거절 상태에서 `호스트와 연결됐습니다: yong의 Mac mini`로 복구됐고 Drag 버튼이 활성화됨. |
| Drag Mode 30회 반복 | `Drag` 탭, 짧은 터치패드 swipe, `Dragging` OFF 탭을 30회 반복 | 통과 | `전송 성공: 296`, `전송 실패: 0`, 최종 버튼 `Drag`, 마지막 리포트 `release_all reason=drag_off`. |
| 명시적 재연결 20회 반복 | `연결 해제` 탭, 연결 해제 callback 대기, `호스트 연결/전환` 탭, 연결됨 callback 대기 | 통과 | `20/20`회 재연결 성공, 실패 `0`회. |
| Drag ON 상태 백그라운드 복구 | Drag ON, HOME, 대기, 앱 재실행 | 통과 | `3583ms`에 복구, 최종 `dragging_after=false`, 연결됨 callback 수신. |
| Drag ON 상태 화면 꺼짐 복구 | Drag ON, 화면 끔, 대기, 깨우기, keyguard 해제, 앱 재실행 | 주의 포함 통과 | `19298ms`에 복구. 첫 auto reconnect 요청은 거절됐고 두 번째 시도에서 연결됨. 최종 `dragging_after=false`. |
| 프로세스 종료 복구 | Drag OFF 확인, `am force-stop`, 앱 재실행 | 통과 | `3581ms`에 복구, 연결됨 callback 수신, 새 프로세스에서 전송 카운터 0으로 초기화. |

## 재연결 지표

명시적 연결 해제/재연결 반복 횟수: 20회.

| 지표 | 값 |
|---|---:|
| 성공 횟수 | 20 |
| 실패 횟수 | 0 |
| 성공률 | 100% |
| 최소 | 726ms |
| 중앙값 | 1646ms |
| P95 | 2251ms |
| 최대 | 2966ms |

샘플, 밀리초 단위:

```text
2206, 1583, 1696, 726, 809, 1248, 2966, 866, 846, 1751,
790, 1215, 1646, 2251, 1983, 1646, 1175, 1863, 1748, 1784
```

명시적 재연결 반복 테스트는 Phase 0 기준인 성공률 90% 이상, 성공 케이스 P95 10초 이내를 만족했습니다.

## 라이프사이클 노트

- 백그라운드 복구는 Drag ON 안전 시나리오에서 통과했습니다. 앱 복귀 후 Drag Mode는 꺼져 있었고 HID는 연결됐습니다.
- 화면 꺼짐 복구는 통과했지만, 첫 번째 `connect request rejected reason=auto_reconnect` 이후 다음 재연결 시도에서 성공했습니다. 이 경로는 회귀 테스트에 계속 포함해야 합니다.
- 프로세스 종료 복구는 Drag OFF 상태에서 통과했습니다. Drag가 실제로 눌린 상태에서 OS가 프로세스를 강제 종료하는 경우는 프로세스가 마지막 release report를 보장하기 전에 종료될 수 있으므로 ADB만으로 완전한 안전을 입증할 수 없습니다.

## 게이트 상태

| 게이트 | 상태 | 비고 |
|---|---|---|
| Drag Mode 30회 ADB 반복 | 통과 | 앱 내부 전송 실패 없음, 최종 상태 `Drag`. |
| 명시적 재연결 20회 P95 | 통과 | P95 `2251ms`, 성공 `20/20`. |
| 백그라운드 복구 sanity check | 통과 | ADB 라이프사이클 1회 실행. |
| 화면 꺼짐 복구 sanity check | 주의 포함 통과 | ADB 라이프사이클 1회 실행, 재연결 거절 1회로 지연 발생. |
| 프로세스 종료 복구 sanity check | 통과 | Drag OFF 상태에서 ADB 라이프사이클 1회 실행. |
| 호스트 커서/실제 드래그 검증 | ADB 범위 밖 | 페어링된 PC에서 육안/수동 확인 필요. |

## 후속 작업

- 화면 꺼짐 프로토콜은 release gate로 삼기 전에 30초 대기 조건의 20회 반복 테스트로 다시 실행합니다.
- Windows 11과 추가 Android 기기에서도 같은 형식의 리포트를 남깁니다.
- 실제 드래그 동작은 Android 로그만으로 PC 데스크톱 효과를 증명할 수 없으므로 호스트 화면 육안 확인을 유지합니다.
