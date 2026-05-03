# 빠른 참조

> English original: [../QUICK_REF.md](../QUICK_REF.md)

## 목적

빠른 구현 판단을 위해 제품 계약을 눈에 보이게 유지합니다.

## 현재 상태

PhonePad v1.0은 Windows/macOS용 Android Direct Bluetooth HID 앱입니다. 원격 데스크톱 제품이 아니며 데스크톱 서버 앱이 필요하지 않습니다.

## 현재 규칙

- 기본 경로: Android `BluetoothHidDevice`.
- 최소 Android: API 28.
- 대상 호스트 OS: 최신 Windows 11과 최신 macOS.
- 호스트 정체성: 표준 Bluetooth HID 마우스/키보드 복합 장치.
- 주 입력 표면: 전체 화면 트랙패드 UI.
- 드래그 동작: 길게 누르기가 아닌 명시적 토글.
- 네트워크 자세: 인터넷 권한 없음.
- 비즈니스 모델: 무료, 오픈소스, 선택적 후원.

## v1.0에 반드시 포함

- 호환성 확인과 미지원 기기 설명.
- 페어링 플로우와 호스트 목록, 저장 호스트 최대 5개와 활성 호스트 1개.
- 커서 이동, 왼쪽 클릭, 오른쪽 클릭, 스크롤.
- 연결 해제, 앱 종료, 화면 잠금, 등록 해제, 호스트 전환 시 안전 해제되는 Drag Mode 토글.
- macOS/Windows 단축키에 매핑되는 세 손가락 스와이프 3개.
- 민감도, 자연 스크롤, 햅틱, 화면 켜짐 유지, Drag 토글 위치, OS 프리셋 설정.
- 한국어 UI와 영어 README.
- CI로 빌드되는 GitHub Releases APK.
- Bridge Dongle 스파이크 결과 문서화.

## 약속하지 말 것

- "모든 Android에서 동작".
- "네이티브 Magic Trackpad" 또는 "Precision Touchpad".
- Direct Bluetooth 모드의 "BIOS/UEFI 지원".
- iPhone을 Direct Bluetooth HID 마우스로 사용.
- Bluetooth 페어링에 여전히 OS 단계가 필요한 경우의 원클릭 설정.

## 성공 지표

- Android 기기 2대 이상이 HID 등록과 입력 테스트를 통과합니다.
- Windows 11과 macOS가 헬퍼 앱 없이 페어링됩니다.
- 재연결 성공률 90% 이상, P95 재연결 시간 10초 이내.
- 정의된 테스트 매트릭스에서 Drag Mode 왼쪽 버튼 고착 사고 0건.
- Manifest에 `INTERNET` 권한 없음.

## 관련 문서

- [HID.md](HID.md)
- [DRAG_MODE.md](DRAG_MODE.md)
- [TEST.md](TEST.md)
- [ROADMAP.md](ROADMAP.md)
