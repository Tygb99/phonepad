# Local Data

## Purpose

Define local persistence for paired hosts, gesture profiles, gesture mappings, and app settings.

## Current State

PhonePad has no account system and no cloud sync. Local SQLite is enough for v1.0.

## Current Rules

- Store only non-sensitive local app state.
- Do not store raw pointer input history.
- Do not persist runtime connection state or Drag Mode state.
- Keep schema migrations simple and covered by tests.
- Default locale is Korean and default theme follows system unless changed.

## Entities

| Entity | Purpose |
|---|---|
| `paired_hosts` | Saved Bluetooth host records and default host choice. |
| `gesture_profiles` | Built-in and user-created mapping profiles. |
| `gesture_mappings` | Gesture-to-action rules per profile. |
| `settings` | One-row app preferences. |

## Schema

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

## Enums

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

## Related Docs

- [ARCHITECTURE.md](ARCHITECTURE.md)
- [API.md](API.md)
- [SECURITY.md](SECURITY.md)
