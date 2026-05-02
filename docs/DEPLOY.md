# Deployment

## Purpose

Define how PhonePad is built, released, and described publicly without overpromising unsupported behavior.

## Current State

GitHub Releases is the first target. Play Store is a later target after compatibility and permission behavior are stable.

## Current Rules

- Do not ship release artifacts before Phase 0 proves Direct HID on real devices.
- Release notes must include tested devices, host OS versions, and known limitations.
- Do not include PRD drafts or chat exports in release artifacts or git commits.
- Do not publish Play Store copy claiming native trackpad behavior.

## GitHub Release Checklist

- CI builds a signed or clearly debug-labeled APK according to release stage.
- Manifest check confirms no `INTERNET`.
- README includes install instructions, support matrix, and limitations.
- Test report includes Windows 11 and macOS results.
- Known unsupported Android devices are listed.
- Drag Mode release test results are summarized.
- SHA256 checksum is attached or documented.

## Play Store v1.1 Checklist

- Privacy policy states no account, no ads, no analytics, no internet permission.
- Permission explanations match actual manifest.
- Foreground service declaration explains connected-device use.
- Store screenshots avoid unsupported claims.
- Copy says "standard Bluetooth input device", not native touchpad.
- Support contact and issue tracker are ready.

## Release Copy Rules

Use:

- "PC에 서버 앱 설치 없이 Bluetooth 입력장치처럼 사용"
- "Android 기기별 지원 여부를 앱에서 확인"
- "Drag Mode로 길게 누르지 않고 드래그"
- "코드 공개, 광고 없음, 인터넷 권한 없음"

Avoid:

- "모든 Android/PC에서 동작"
- "Magic Trackpad 대체"
- "BIOS 지원"
- "원클릭 연결"

## Related Docs

- [MOBILE_APP.md](MOBILE_APP.md)
- [SECURITY.md](SECURITY.md)
- [TEST.md](TEST.md)
- [ROADMAP.md](ROADMAP.md)
