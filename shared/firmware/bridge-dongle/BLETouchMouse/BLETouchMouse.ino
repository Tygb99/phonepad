#include <Arduino.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <USB.h>
#include <USBHIDKeyboard.h>
#include <USBHIDMouse.h>

/*
  BLE Touch Mouse for Seeed Studio XIAO ESP32-S3

  Board role:
    - BLE peripheral that receives PhonePad input packets from the iPhone app.
    - USB HID mouse and keyboard connected to the target computer.

  Mouse packet format, 4 bytes:
    byte 0: signed int8 dx
    byte 1: signed int8 dy
    byte 2: buttons bitmask, bit 0 left, bit 1 right, bit 2 middle
    byte 3: signed int8 wheel

  Keyboard key packet:
    byte 0: 0x03
    byte 1: action, 0 tap, 1 press, 2 release
    byte 2: Arduino USBHIDKeyboard key code

  Protocol v2 safety packets never use exactly 4 bytes:
    0x10, seq: release all mouse buttons and keyboard keys
    0x11, seq, modifier mask, key count, key...: key chord
    0x12, seq, host profile: language toggle

  Arduino IDE notes:
    - Select an ESP32-S3/XIAO ESP32S3 board.
    - Enable USB CDC on boot if you want Serial logs.
    - The target computer must be connected to the XIAO's native USB port.
*/

static const char *DEVICE_NAME = "BLE Touch Mouse";
static const char *SERVICE_UUID = "7c2d2b6a-8f3e-4c6f-8d6f-01b0f4dd1000";
static const char *MOUSE_CHAR_UUID = "7c2d2b6a-8f3e-4c6f-8d6f-01b0f4dd1001";

USBHIDMouse Mouse;
USBHIDKeyboard Keyboard;

BLEServer *bleServer = nullptr;
BLECharacteristic *mouseCharacteristic = nullptr;

bool bleConnected = false;
uint8_t currentButtons = 0;

static const uint8_t PACKET_KEYBOARD_KEY = 0x03;
static const uint8_t PACKET_RELEASE_ALL = 0x10;
static const uint8_t PACKET_KEY_CHORD = 0x11;
static const uint8_t PACKET_LANGUAGE_TOGGLE = 0x12;
static const uint8_t KEY_ACTION_TAP = 0x00;
static const uint8_t KEY_ACTION_PRESS = 0x01;
static const uint8_t KEY_ACTION_RELEASE = 0x02;
static const uint8_t HOST_PROFILE_MAC = 0x01;
static const uint8_t HOST_PROFILE_WINDOWS_LANG1 = 0x02;
static const uint8_t HOST_PROFILE_WINDOWS_RIGHT_ALT = 0x03;

static const uint8_t MOD_LEFT_CONTROL = 0x01;
static const uint8_t MOD_LEFT_SHIFT = 0x02;
static const uint8_t MOD_LEFT_ALT = 0x04;
static const uint8_t MOD_LEFT_GUI = 0x08;
static const uint8_t MOD_RIGHT_CONTROL = 0x10;
static const uint8_t MOD_RIGHT_SHIFT = 0x20;
static const uint8_t MOD_RIGHT_ALT = 0x40;
static const uint8_t MOD_RIGHT_GUI = 0x80;

static const uint8_t ARDUINO_KEY_LEFT_CONTROL = 0x80;
static const uint8_t ARDUINO_KEY_LEFT_SHIFT = 0x81;
static const uint8_t ARDUINO_KEY_LEFT_ALT = 0x82;
static const uint8_t ARDUINO_KEY_LEFT_GUI = 0x83;
static const uint8_t ARDUINO_KEY_RIGHT_CONTROL = 0x84;
static const uint8_t ARDUINO_KEY_RIGHT_SHIFT = 0x85;
static const uint8_t ARDUINO_KEY_RIGHT_ALT = 0x86;
static const uint8_t ARDUINO_KEY_RIGHT_GUI = 0x87;
static const uint8_t ARDUINO_KEY_SPACE = 0x20;
static const uint8_t ARDUINO_KEY_LANG1 = 0x90;

void applyButtons(uint8_t buttons) {
  if ((buttons & 0x01) != (currentButtons & 0x01)) {
    if (buttons & 0x01) {
      Mouse.press(MOUSE_LEFT);
    } else {
      Mouse.release(MOUSE_LEFT);
    }
  }

  if ((buttons & 0x02) != (currentButtons & 0x02)) {
    if (buttons & 0x02) {
      Mouse.press(MOUSE_RIGHT);
    } else {
      Mouse.release(MOUSE_RIGHT);
    }
  }

  if ((buttons & 0x04) != (currentButtons & 0x04)) {
    if (buttons & 0x04) {
      Mouse.press(MOUSE_MIDDLE);
    } else {
      Mouse.release(MOUSE_MIDDLE);
    }
  }

  currentButtons = buttons & 0x07;
}

void releaseAllInputs() {
  Mouse.release(MOUSE_LEFT);
  Mouse.release(MOUSE_RIGHT);
  Mouse.release(MOUSE_MIDDLE);
  currentButtons = 0;
  Keyboard.releaseAll();
}

void tapKey(uint8_t key) {
  Keyboard.press(key);
  delay(8);
  Keyboard.release(key);
}

void pressModifierKeys(uint8_t modifiers) {
  if (modifiers & MOD_LEFT_CONTROL) {
    Keyboard.press(ARDUINO_KEY_LEFT_CONTROL);
  }
  if (modifiers & MOD_LEFT_SHIFT) {
    Keyboard.press(ARDUINO_KEY_LEFT_SHIFT);
  }
  if (modifiers & MOD_LEFT_ALT) {
    Keyboard.press(ARDUINO_KEY_LEFT_ALT);
  }
  if (modifiers & MOD_LEFT_GUI) {
    Keyboard.press(ARDUINO_KEY_LEFT_GUI);
  }
  if (modifiers & MOD_RIGHT_CONTROL) {
    Keyboard.press(ARDUINO_KEY_RIGHT_CONTROL);
  }
  if (modifiers & MOD_RIGHT_SHIFT) {
    Keyboard.press(ARDUINO_KEY_RIGHT_SHIFT);
  }
  if (modifiers & MOD_RIGHT_ALT) {
    Keyboard.press(ARDUINO_KEY_RIGHT_ALT);
  }
  if (modifiers & MOD_RIGHT_GUI) {
    Keyboard.press(ARDUINO_KEY_RIGHT_GUI);
  }
}

void releaseModifierKeys(uint8_t modifiers) {
  if (modifiers & MOD_LEFT_CONTROL) {
    Keyboard.release(ARDUINO_KEY_LEFT_CONTROL);
  }
  if (modifiers & MOD_LEFT_SHIFT) {
    Keyboard.release(ARDUINO_KEY_LEFT_SHIFT);
  }
  if (modifiers & MOD_LEFT_ALT) {
    Keyboard.release(ARDUINO_KEY_LEFT_ALT);
  }
  if (modifiers & MOD_LEFT_GUI) {
    Keyboard.release(ARDUINO_KEY_LEFT_GUI);
  }
  if (modifiers & MOD_RIGHT_CONTROL) {
    Keyboard.release(ARDUINO_KEY_RIGHT_CONTROL);
  }
  if (modifiers & MOD_RIGHT_SHIFT) {
    Keyboard.release(ARDUINO_KEY_RIGHT_SHIFT);
  }
  if (modifiers & MOD_RIGHT_ALT) {
    Keyboard.release(ARDUINO_KEY_RIGHT_ALT);
  }
  if (modifiers & MOD_RIGHT_GUI) {
    Keyboard.release(ARDUINO_KEY_RIGHT_GUI);
  }
}

class ServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer *server) override {
    bleConnected = true;
    Serial.println("BLE connected");
  }

  void onDisconnect(BLEServer *server) override {
    bleConnected = false;
    releaseAllInputs();
    Serial.println("BLE disconnected, advertising restarted");
    server->getAdvertising()->start();
  }
};

template <typename PacketValue>
void handleMousePacket(const PacketValue &value) {
  const int8_t dx = static_cast<int8_t>(value[0]);
  const int8_t dy = static_cast<int8_t>(value[1]);
  const uint8_t buttons = static_cast<uint8_t>(value[2]);
  const int8_t wheel = static_cast<int8_t>(value[3]);

  applyButtons(buttons);

  if (dx != 0 || dy != 0 || wheel != 0) {
    Mouse.move(dx, dy, wheel);
  }
}

template <typename PacketValue>
void handleKeyboardKeyPacket(const PacketValue &value) {
  if (value.length() < 3) {
    return;
  }

  const uint8_t action = static_cast<uint8_t>(value[1]);
  const uint8_t key = static_cast<uint8_t>(value[2]);

  switch (action) {
    case KEY_ACTION_TAP:
      tapKey(key);
      break;
    case KEY_ACTION_PRESS:
      Keyboard.press(key);
      break;
    case KEY_ACTION_RELEASE:
      Keyboard.release(key);
      break;
    default:
      break;
  }
}

template <typename PacketValue>
void handleKeyChordPacket(const PacketValue &value) {
  if (value.length() < 4) {
    return;
  }

  const uint8_t modifiers = static_cast<uint8_t>(value[2]);
  uint8_t keyCount = static_cast<uint8_t>(value[3]);
  if (keyCount > 6) {
    keyCount = 6;
  }
  if (value.length() < 4 + keyCount) {
    return;
  }

  pressModifierKeys(modifiers);
  delay(4);
  for (uint8_t index = 0; index < keyCount; index++) {
    tapKey(static_cast<uint8_t>(value[4 + index]));
  }
  delay(4);
  releaseModifierKeys(modifiers);
  Keyboard.releaseAll();
}

template <typename PacketValue>
void handleLanguageTogglePacket(const PacketValue &value) {
  if (value.length() < 3) {
    return;
  }

  const uint8_t hostProfile = static_cast<uint8_t>(value[2]);
  switch (hostProfile) {
    case HOST_PROFILE_MAC:
      pressModifierKeys(MOD_LEFT_CONTROL);
      delay(4);
      tapKey(ARDUINO_KEY_SPACE);
      delay(4);
      releaseModifierKeys(MOD_LEFT_CONTROL);
      break;
    case HOST_PROFILE_WINDOWS_LANG1:
      tapKey(ARDUINO_KEY_LANG1);
      break;
    case HOST_PROFILE_WINDOWS_RIGHT_ALT:
      tapKey(ARDUINO_KEY_RIGHT_ALT);
      break;
    default:
      break;
  }
  Keyboard.releaseAll();
}

class MouseCommandCallbacks : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic *characteristic) override {
    auto value = characteristic->getValue();

    if (value.length() >= 2 && value.length() != 4) {
      const uint8_t packetType = static_cast<uint8_t>(value[0]);
      switch (packetType) {
        case PACKET_KEYBOARD_KEY:
          handleKeyboardKeyPacket(value);
          return;
        case PACKET_RELEASE_ALL:
          releaseAllInputs();
          return;
        case PACKET_KEY_CHORD:
          handleKeyChordPacket(value);
          return;
        case PACKET_LANGUAGE_TOGGLE:
          handleLanguageTogglePacket(value);
          return;
        default:
          break;
      }
    }

    if (value.length() >= 4) {
      handleMousePacket(value);
    }
  }
};

void setupBle() {
  BLEDevice::init(DEVICE_NAME);

  bleServer = BLEDevice::createServer();
  bleServer->setCallbacks(new ServerCallbacks());

  BLEService *service = bleServer->createService(SERVICE_UUID);

  mouseCharacteristic = service->createCharacteristic(
    MOUSE_CHAR_UUID,
    BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_WRITE_NR
  );
  mouseCharacteristic->setCallbacks(new MouseCommandCallbacks());

  service->start();

  BLEAdvertising *advertising = BLEDevice::getAdvertising();
  advertising->addServiceUUID(SERVICE_UUID);
  advertising->setScanResponse(true);
  advertising->setMinPreferred(0x06);
  advertising->setMinPreferred(0x12);
  BLEDevice::startAdvertising();
}

void setup() {
  Serial.begin(115200);
  delay(1000);

  Mouse.begin();
  Keyboard.begin();
  USB.begin();

  setupBle();

  Serial.println("BLE Touch Mouse ready");
}

void loop() {
  delay(20);
}
