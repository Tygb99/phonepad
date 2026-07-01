# PhonePad iOS Sender

> Korean version: [README.ko.md](README.ko.md)

PhonePad iOS is a SwiftUI + CoreBluetooth sender for the Bridge Dongle path:

```text
iPhone app -> BLE GATT write -> ESP32-S3 bridge dongle -> USB HID -> host computer
```

This is separate from the Android Direct HID MVP. The iOS Simulator can compile the app, but it cannot validate Bluetooth LE input delivery. Real behavior requires:

- A physical iPhone selected in Xcode.
- A locally configured Apple developer team for signing.
- An ESP32-S3 dongle flashed with `shared/firmware/bridge-dongle/BLETouchMouse/BLETouchMouse.ino`.
- A Mac, Windows PC, or BIOS/UEFI target connected to the dongle over USB.

The checked-in project intentionally omits personal `DEVELOPMENT_TEAM`, `xcuserdata`, and local signing state. Set signing locally in Xcode before installing on a real iPhone, and do not commit personal signing changes.

## Build

Open:

```text
ios/PhonePad/PhonePad.xcodeproj
```

Use the `PhonePad` scheme.

## Current Input Scope

- Pointer movement, click, double-click, right-click, middle-click, and vertical scroll.
- Single-key keyboard packets.
- Protocol v2 safety packets for `ReleaseAll`, `KeyChord`, host-profile language toggle, extended mouse input, and dongle status read/notify.
- Mac language toggle uses `Control + Space`.
- Windows language toggle candidates are `LANG1` and `RightAlt`.

The firmware protocol also supports horizontal scroll and back/forward mouse buttons, but the current SwiftUI screen does not expose dedicated controls for them yet.

Full Korean text input is not implemented here. Future Korean input should send physical QWERTY keycodes while the host IME is active, not composed Hangul syllables over BLE.
