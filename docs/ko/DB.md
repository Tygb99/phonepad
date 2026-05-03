# 로컬 데이터

> English original: [../DB.md](../DB.md)

## 목적

페어링된 호스트, 제스처 프로필, 제스처 매핑, 앱 설정을 위한 로컬 영속성을 정의합니다.

## 현재 상태

PhonePad에는 계정 시스템과 클라우드 동기화가 없습니다. v1.0에는 로컬 SQLite로 충분합니다.

## 현재 규칙

- 민감하지 않은 로컬 앱 상태만 저장합니다.
- 원시 포인터 입력 기록은 저장하지 않습니다.
- 런타임 연결 상태나 Drag Mode 상태는 저장하지 않습니다.
- 스키마 마이그레이션은 단순하게 유지하고 테스트로 커버합니다.
- 기본 로케일은 한국어이며 기본 테마는 변경 전까지 시스템을 따릅니다.

## 엔티티

| 엔티티 | 목적 |
|---|---|
| `paired_hosts` | 저장된 Bluetooth 호스트 기록과 기본 호스트 선택. |
| `gesture_profiles` | 내장 및 사용자 생성 매핑 프로필. |
| `gesture_mappings` | 프로필별 제스처-액션 규칙. |
| `settings` | 단일 행 앱 환경설정. |

## 스키마

```sql
CREATE TABLE paired_hosts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    bluetooth_address TEXT UNIQUE,
    display_name TEXT NOT NULL,
    os_type TEXT NOT NULL,
    last_connected_at INTEGER,
    is_default INTEGER DEFAULT 0,
    active_profile_id INTEGER,
    created_at INTEGER NOT NULL
);

CREATE TABLE gesture_profiles (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    target_os TEXT,
    is_builtin INTEGER DEFAULT 0,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE TABLE gesture_mappings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    profile_id INTEGER NOT NULL,
    gesture_type TEXT NOT NULL,
    action_type TEXT NOT NULL,
    action_payload TEXT,
    enabled INTEGER DEFAULT 1,
    UNIQUE(profile_id, gesture_type)
);

CREATE TABLE settings (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    cursor_sensitivity REAL DEFAULT 1.0,
    natural_scroll INTEGER DEFAULT 1,
    haptic_intensity INTEGER DEFAULT 2,
    keep_screen_on INTEGER DEFAULT 1,
    theme TEXT DEFAULT 'system',
    locale TEXT DEFAULT 'ko',
    drag_toggle_position TEXT DEFAULT 'bottom_right',
    drag_idle_auto_release_sec INTEGER DEFAULT 0
);
```

## 열거형

```text
compat_state:
supported, os_too_old, bluetooth_unavailable, hid_device_unavailable,
permission_missing, host_pairing_failed, unknown_error

connection_state:
idle, registering, advertising, paired, connecting, connected,
disconnected, reconnecting, failed

host_os:
windows, macos, linux, unknown

drag_mode_state:
off, enabling, on, release_pending, error

gesture_type:
tap_1, tap_2, scroll_2, swipe_up_3, swipe_left_3, swipe_right_3

action_type:
mouse_button, key_combo, media_key, none
```

## 관련 문서

- [ARCHITECTURE.md](ARCHITECTURE.md)
- [API.md](API.md)
- [SECURITY.md](SECURITY.md)
