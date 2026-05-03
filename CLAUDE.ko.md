# CLAUDE.md

> English original: [CLAUDE.md](CLAUDE.md)

## 목적

이 파일은 AI 코딩 에이전트가 비공개 기획 원본을 다시 열지 않고도 구현할 수 있도록 PhonePad의 현재 제품 규칙을 제공합니다.

## 현재 상태

- 제품: Android 휴대폰을 Windows/macOS용 Bluetooth HID 트랙패드형 입력 장치로 사용.
- 주 경로: Android Direct `BluetoothHidDevice`.
- Plan B: Bridge Dongle 스파이크, 별도 문서화.
- 현재 저장소 단계: 문서 스캐폴드와 Phase 0 구현 계획.
- git 위생: PRD 초안과 KakaoTalk 내보내기는 로컬 기획 원본이며 커밋하면 안 됩니다.

## 현재 규칙

- v1.0은 데스크톱 쪽에서 서버 없이 유지합니다. Windows/macOS 헬퍼 앱을 추가하지 않습니다.
- Android 인터넷 권한을 요청하지 않습니다.
- 계정, 광고, 분석, 추적 SDK를 추가하지 않습니다.
- Drag Mode 안전 해제를 P0 동작으로 취급합니다.
- 네이티브 OS 터치패드 동작이라고 주장하지 않습니다. "Bluetooth HID mouse + keyboard shortcut mapping"을 사용합니다.
- 넓은 Flutter UI 작업 전에 Phase 0에서는 Kotlin proof-of-concept를 우선합니다.
- Android 기기, 호스트 OS, Bluetooth 전송/프로필 관찰, 실패 모드를 문서에 기록합니다.
- 한국어 우선 UX 문구를 보존합니다. 오픈소스 기여자에게 도움이 되는 경우 영어를 사용합니다.

## 구현 우선순위

1. `BluetoothHidDevice`를 얻는 최소 Kotlin Android 앱.
2. 마우스 전용 HID 앱 등록 및 이동 리포트 전송.
3. Windows 11을 먼저 페어링/테스트하고, 그다음 macOS를 테스트.
4. 클릭, 스크롤, Drag Mode ON/OFF 리포트, 안전 해제 훅 추가.
5. 네이티브 경로가 입증된 뒤 MethodChannel로 Flutter UI 연결.

## 강제 승인 게이트

- Android 기기 최소 2대에서 HID 등록에 성공하거나 명확한 미지원 기기 UX를 보여줍니다.
- Windows 11과 macOS가 데스크톱 서버 소프트웨어 없이 페어링됩니다.
- Drag Mode OFF 및 라이프사이클 안전 이벤트가 왼쪽 버튼 고착 상태를 남기지 않습니다.
- Manifest에 `INTERNET` 권한이 없습니다.
- CI가 재현 가능한 GitHub Releases APK를 만들 수 있습니다.

## 관련 문서

- [docs/ko/INDEX.md](docs/ko/INDEX.md)
- [docs/ko/HID.md](docs/ko/HID.md)
- [docs/ko/DRAG_MODE.md](docs/ko/DRAG_MODE.md)
- [docs/ko/TEST.md](docs/ko/TEST.md)
- [docs/ko/ref/prompts/README.md](docs/ko/ref/prompts/README.md)
