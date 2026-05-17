# 배포

> English original: [../DEPLOY.md](../DEPLOY.md)

## 목적

지원되지 않는 동작을 과장하지 않으면서 PhonePad를 빌드, 릴리스, 공개 설명하는 방식을 정의합니다.

## 현재 상태

첫 대상은 GitHub Releases입니다. Play Store는 호환성과 권한 동작이 안정된 뒤의 이후 대상입니다.

## 현재 규칙

- Phase 0이 실제 기기에서 Direct HID를 입증하기 전에는 릴리스 산출물을 배포하지 않습니다.
- 릴리스 노트에는 테스트한 기기, 호스트 OS 버전, 알려진 제한사항을 포함해야 합니다.
- PRD 초안이나 채팅 내보내기를 릴리스 산출물 또는 git 커밋에 포함하지 않습니다.
- 네이티브 트랙패드 동작을 주장하는 Play Store 문구를 게시하지 않습니다.

## GitHub Release 체크리스트

- CI가 릴리스 단계에 맞게 서명된 APK 또는 명확히 debug 라벨이 붙은 APK를 빌드합니다.
- Manifest 확인에서 `INTERNET` 없음이 확인됩니다.
- README에 설치 안내, 지원 매트릭스, 제한사항이 포함됩니다.
- 테스트 리포트에 Windows 11과 macOS 결과가 포함됩니다.
- 알려진 미지원 Android 기기가 나열됩니다.
- Drag Mode 해제 테스트 결과가 요약됩니다.
- SHA256 checksum이 첨부되거나 문서화됩니다.

## GitHub Actions APK 흐름

- push와 pull request에서 `:app:assembleDebug`와 `:app:lintDebug`를 실행합니다.
- CI는 source manifest와 빌드된 APK 모두에 `INTERNET` 선언이 없는지 확인합니다.
- 성공한 실행마다 debug 라벨 APK artifact와 `SHA256SUMS.txt`를 업로드합니다.
- `v*` 태그를 push하면 GitHub Release를 만들거나 갱신하고 APK와 checksum을 첨부합니다.
- 현재 Phase 0 산출물은 호환성 테스트용 debug APK이며 Play Store 서명 빌드가 아닙니다.

## Play Store v1.1 체크리스트

- 개인정보처리방침은 계정 없음, 광고 없음, 분석 없음, 인터넷 권한 없음을 명시합니다.
- 권한 설명은 실제 manifest와 일치합니다.
- Foreground service 선언은 connected-device 사용을 설명합니다.
- 스토어 스크린샷은 미지원 주장을 피합니다.
- 문구는 네이티브 터치패드가 아니라 "standard Bluetooth input device"라고 말합니다.
- 지원 연락처와 이슈 트래커가 준비됩니다.

## 릴리스 문구 규칙

사용:

- "PC에 서버 앱 설치 없이 Bluetooth 입력장치처럼 사용"
- "Android 기기별 지원 여부를 앱에서 확인"
- "Drag Mode로 길게 누르지 않고 드래그"
- "코드 공개, 광고 없음, 인터넷 권한 없음"

피함:

- "모든 Android/PC에서 동작"
- "Magic Trackpad 대체"
- "BIOS 지원"
- "원클릭 연결"

## 관련 문서

- [MOBILE_APP.md](MOBILE_APP.md)
- [SECURITY.md](SECURITY.md)
- [TEST.md](TEST.md)
- [ROADMAP.md](ROADMAP.md)
