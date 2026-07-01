import Combine
import CoreBluetooth
import Foundation

final class BLEMouseController: NSObject, ObservableObject {
    static let serviceUUID = CBUUID(string: "7c2d2b6a-8f3e-4c6f-8d6f-01b0f4dd1000")
    static let mouseCharacteristicUUID = CBUUID(string: "7c2d2b6a-8f3e-4c6f-8d6f-01b0f4dd1001")
    static let statusCharacteristicUUID = CBUUID(string: "7c2d2b6a-8f3e-4c6f-8d6f-01b0f4dd1002")
    private static let expectedDeviceName = "BLE Touch Mouse"
    private static let serviceScanFallbackDelay: TimeInterval = 5
    private static let packetReleaseAll: UInt8 = 0x10
    private static let packetKeyChord: UInt8 = 0x11
    private static let packetLanguageToggle: UInt8 = 0x12
    private static let packetExtendedMouse: UInt8 = 0x13

    @Published private(set) var statusText = "Bluetooth starting..."
    @Published private(set) var isScanning = false
    @Published private(set) var isConnected = false
    @Published private(set) var discoveredDevices: [BLEDevice] = []

    private var centralManager: CBCentralManager!
    private var connectedPeripheral: CBPeripheral?
    private var mouseCharacteristic: CBCharacteristic?
    private var statusCharacteristic: CBCharacteristic?
    private var currentButtons: UInt8 = 0
    private var shouldAutoConnect = true
    private var nextSequenceID: UInt8 = 0
    private var isFallbackScan = false
    private var nearbyPeripheralCount = 0
    private var fallbackScanTask: DispatchWorkItem?

    override init() {
        super.init()
        centralManager = CBCentralManager(delegate: self, queue: .main)
    }

    func scan() {
        guard centralManager.state == .poweredOn else {
            statusText = "Bluetooth is not ready"
            return
        }

        discoveredDevices.removeAll()
        nearbyPeripheralCount = 0
        startServiceFilteredScan()
    }

    private func startServiceFilteredScan() {
        fallbackScanTask?.cancel()
        isFallbackScan = false
        isScanning = true
        statusText = "Scanning for \(Self.expectedDeviceName)..."
        centralManager.scanForPeripherals(
            withServices: [Self.serviceUUID],
            options: [CBCentralManagerScanOptionAllowDuplicatesKey: false]
        )
        scheduleFallbackScan()
    }

    private func scheduleFallbackScan() {
        let task = DispatchWorkItem { [weak self] in
            guard let self, isScanning, !isConnected, discoveredDevices.isEmpty else { return }
            startFallbackScan()
        }
        fallbackScanTask = task
        DispatchQueue.main.asyncAfter(deadline: .now() + Self.serviceScanFallbackDelay, execute: task)
    }

    private func startFallbackScan() {
        centralManager.stopScan()
        isFallbackScan = true
        nearbyPeripheralCount = 0
        isScanning = true
        statusText = "No service ad yet. Scanning nearby BLE names..."
        centralManager.scanForPeripherals(
            withServices: nil,
            options: [CBCentralManagerScanOptionAllowDuplicatesKey: false]
        )
    }

    func stopScan() {
        fallbackScanTask?.cancel()
        fallbackScanTask = nil
        centralManager.stopScan()
        isScanning = false
        if !isConnected {
            statusText = "Scan stopped"
        }
    }

    func connect(to device: BLEDevice) {
        stopScan()
        statusText = "Connecting to \(device.name)..."
        centralManager.connect(device.peripheral)
    }

    func disconnect() {
        shouldAutoConnect = false
        releaseAll()
        guard let connectedPeripheral else { return }
        centralManager.cancelPeripheralConnection(connectedPeripheral)
    }

    func resumeAutoConnect() {
        shouldAutoConnect = true
        scan()
    }

    func move(dx: Double, dy: Double, sensitivity: Double) {
        sendPacket(
            dx: Self.clampInt8(dx * sensitivity),
            dy: Self.clampInt8(dy * sensitivity),
            buttons: currentButtons,
            wheel: 0
        )
    }

    func setButton(_ button: MouseButton, pressed: Bool) {
        if pressed {
            currentButtons |= button.bit
        } else {
            currentButtons &= ~button.bit
        }

        sendPacket(dx: 0, dy: 0, buttons: currentButtons, wheel: 0)
    }

    func wheel(_ amount: Int8) {
        sendPacket(dx: 0, dy: 0, buttons: currentButtons, wheel: amount)
    }

    func horizontalWheel(_ amount: Int8) {
        sendExtendedMousePacket(dx: 0, dy: 0, buttons: currentButtons, wheel: 0, pan: amount)
    }

    func click(_ button: MouseButton = .left) {
        setButton(button, pressed: true)
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.04) { [weak self] in
            self?.setButton(button, pressed: false)
        }
    }

    func doubleClick(_ button: MouseButton = .left) {
        click(button)
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.12) { [weak self] in
            self?.click(button)
        }
    }

    func tapKeyboardKey(_ key: KeyboardKeyCode) {
        writeKeyboardPacket(Data([0x03, 0x00, key.rawValue]))
    }

    func pressKeyboardKey(_ key: KeyboardKeyCode) {
        writeKeyboardPacket(Data([0x03, 0x01, key.rawValue]))
    }

    func releaseKeyboardKey(_ key: KeyboardKeyCode) {
        writeKeyboardPacket(Data([0x03, 0x02, key.rawValue]))
    }

    func releaseAll() {
        currentButtons = 0
        writeControlPacket(Data([Self.packetReleaseAll, nextSequence()]))
    }

    func keyChord(modifiers: KeyboardModifier = [], keys: [KeyboardKeyCode]) {
        let limitedKeys = Array(keys.prefix(6))
        guard !modifiers.isEmpty || !limitedKeys.isEmpty else {
            releaseAll()
            return
        }

        var packet = Data([Self.packetKeyChord, nextSequence(), modifiers.rawValue, UInt8(limitedKeys.count)])
        packet.append(contentsOf: limitedKeys.map(\.rawValue))
        writeKeyboardPacket(packet)
    }

    func languageToggle(for hostProfile: HostProfile) {
        writeControlPacket(Data([Self.packetLanguageToggle, nextSequence(), hostProfile.rawValue]))
    }

    private func sendPacket(dx: Int8, dy: Int8, buttons: UInt8, wheel: Int8) {
        let packet = Data([
            UInt8(bitPattern: dx),
            UInt8(bitPattern: dy),
            buttons & 0x1F,
            UInt8(bitPattern: wheel)
        ])

        writeBleData(packet)
    }

    private func sendExtendedMousePacket(dx: Int8, dy: Int8, buttons: UInt8, wheel: Int8, pan: Int8) {
        let packet = Data([
            Self.packetExtendedMouse,
            nextSequence(),
            UInt8(bitPattern: dx),
            UInt8(bitPattern: dy),
            buttons & 0x1F,
            UInt8(bitPattern: wheel),
            UInt8(bitPattern: pan)
        ])

        writeBleData(packet, preferResponse: true)
    }

    private func writeBleData(_ packet: Data, preferResponse: Bool = false) {
        guard let connectedPeripheral, let mouseCharacteristic else { return }

        let properties = mouseCharacteristic.properties
        let writeType: CBCharacteristicWriteType
        if preferResponse && properties.contains(.write) {
            writeType = .withResponse
        } else if properties.contains(.writeWithoutResponse) {
            writeType = .withoutResponse
        } else {
            writeType = .withResponse
        }

        connectedPeripheral.writeValue(packet, for: mouseCharacteristic, type: writeType)
    }

    private func writeKeyboardPacket(_ packet: Data) {
        var safePacket = packet
        if safePacket.count == 4 {
            safePacket.append(0)
        }
        writeBleData(safePacket, preferResponse: true)
    }

    private func writeControlPacket(_ packet: Data) {
        var safePacket = packet
        if safePacket.count == 4 {
            safePacket.append(0)
        }
        writeBleData(safePacket, preferResponse: true)
    }

    private func nextSequence() -> UInt8 {
        defer { nextSequenceID &+= 1 }
        return nextSequenceID
    }

    private static func clampInt8(_ value: Double) -> Int8 {
        let rounded = Int(value.rounded())
        return Int8(max(-127, min(127, rounded)))
    }

    private func resetConnectionState(status: String) {
        connectedPeripheral = nil
        mouseCharacteristic = nil
        statusCharacteristic = nil
        currentButtons = 0
        isConnected = false
        statusText = status
    }
}

extension BLEMouseController: CBCentralManagerDelegate {
    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        switch central.state {
        case .poweredOn:
            statusText = "Ready to scan"
            if shouldAutoConnect {
                scan()
            }
        case .poweredOff:
            resetConnectionState(status: "Bluetooth is off")
        case .unauthorized:
            resetConnectionState(status: "Bluetooth permission is needed")
        case .unsupported:
            resetConnectionState(status: "Bluetooth LE is unsupported")
        case .resetting:
            resetConnectionState(status: "Bluetooth is resetting")
        case .unknown:
            resetConnectionState(status: "Bluetooth state unknown")
        @unknown default:
            resetConnectionState(status: "Bluetooth unavailable")
        }
    }

    func centralManager(
        _ central: CBCentralManager,
        didDiscover peripheral: CBPeripheral,
        advertisementData: [String: Any],
        rssi RSSI: NSNumber
    ) {
        let advertisedName = peripheral.name
            ?? advertisementData[CBAdvertisementDataLocalNameKey] as? String
        let advertisesTargetService = Self.advertisementIncludesTargetService(advertisementData)
        let isNamedCandidate = advertisedName?.localizedCaseInsensitiveContains(Self.expectedDeviceName) == true

        if isFallbackScan && !advertisesTargetService && !isNamedCandidate {
            nearbyPeripheralCount += 1
            statusText = "Scanning nearby BLE names... ignored \(nearbyPeripheralCount)"
            return
        }

        let name = advertisedName ?? Self.expectedDeviceName

        guard !discoveredDevices.contains(where: { $0.id == peripheral.identifier }) else {
            return
        }

        discoveredDevices.append(BLEDevice(peripheral: peripheral, name: name, rssi: RSSI.intValue))
        statusText = "Found \(name), connecting..."

        if shouldAutoConnect && connectedPeripheral == nil {
            connect(to: BLEDevice(peripheral: peripheral, name: name, rssi: RSSI.intValue))
        }
    }

    private static func advertisementIncludesTargetService(_ advertisementData: [String: Any]) -> Bool {
        let serviceUUIDs = advertisementData[CBAdvertisementDataServiceUUIDsKey] as? [CBUUID] ?? []
        let overflowUUIDs = advertisementData[CBAdvertisementDataOverflowServiceUUIDsKey] as? [CBUUID] ?? []
        return (serviceUUIDs + overflowUUIDs).contains(serviceUUID)
    }

    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        connectedPeripheral = peripheral
        peripheral.delegate = self
        statusText = "Discovering services..."
        peripheral.discoverServices([Self.serviceUUID])
    }

    func centralManager(_ central: CBCentralManager, didFailToConnect peripheral: CBPeripheral, error: Error?) {
        resetConnectionState(status: error?.localizedDescription ?? "Connection failed")
        if shouldAutoConnect {
            scan()
        }
    }

    func centralManager(_ central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral, error: Error?) {
        resetConnectionState(status: error?.localizedDescription ?? "Disconnected")
        if shouldAutoConnect {
            scan()
        }
    }
}

extension BLEMouseController: CBPeripheralDelegate {
    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        if let error {
            resetConnectionState(status: error.localizedDescription)
            return
        }

        guard let service = peripheral.services?.first(where: { $0.uuid == Self.serviceUUID }) else {
            resetConnectionState(status: "Mouse service not found")
            return
        }

        peripheral.discoverCharacteristics([Self.mouseCharacteristicUUID, Self.statusCharacteristicUUID], for: service)
    }

    func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
        if let error {
            resetConnectionState(status: error.localizedDescription)
            return
        }

        guard let characteristics = service.characteristics,
              let characteristic = characteristics.first(where: { $0.uuid == Self.mouseCharacteristicUUID }) else {
            resetConnectionState(status: "Mouse characteristic not found")
            return
        }

        mouseCharacteristic = characteristic
        if let statusCharacteristic = characteristics.first(where: { $0.uuid == Self.statusCharacteristicUUID }) {
            self.statusCharacteristic = statusCharacteristic
            if statusCharacteristic.properties.contains(.notify) {
                peripheral.setNotifyValue(true, for: statusCharacteristic)
            }
            if statusCharacteristic.properties.contains(.read) {
                peripheral.readValue(for: statusCharacteristic)
            }
        }
        isConnected = true
        isScanning = false
        statusText = "Connected to \(peripheral.name ?? "BLE Touch Mouse")"
    }

    func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
        if let error {
            statusText = error.localizedDescription
            return
        }
        guard characteristic.uuid == Self.statusCharacteristicUUID,
              let data = characteristic.value,
              let dongleStatus = Self.describeDongleStatus(data) else {
            return
        }
        statusText = dongleStatus
    }

    private static func describeDongleStatus(_ data: Data) -> String? {
        let bytes = [UInt8](data)
        guard bytes.count >= 4 else { return nil }

        let status = bytes[0]
        let packetType = bytes[1]
        let sequence = bytes[2]
        let detail = bytes[3]
        let packetName = describePacketType(packetType)
        switch status {
        case 0x01:
            return "Dongle ready"
        case 0x02:
            return "Dongle BLE connected"
        case 0x03:
            return "Dongle BLE disconnected"
        case 0x10:
            return "Dongle OK \(packetName) #\(sequence)"
        case 0x11:
            return "Dongle ignored duplicate \(packetName) #\(sequence)"
        case 0x80:
            return "Dongle rejected malformed \(packetName) (\(detail) bytes)"
        case 0x81:
            return "Dongle ignored unknown packet 0x\(String(format: "%02X", packetType))"
        default:
            return "Dongle status 0x\(String(format: "%02X", status))"
        }
    }

    private static func describePacketType(_ packetType: UInt8) -> String {
        switch packetType {
        case 0x00:
            return "system"
        case 0x03:
            return "key"
        case 0x10:
            return "release"
        case 0x11:
            return "chord"
        case 0x12:
            return "language"
        case 0x13:
            return "mouse+"
        default:
            return "0x\(String(format: "%02X", packetType))"
        }
    }
}

struct BLEDevice: Identifiable {
    let peripheral: CBPeripheral
    let name: String
    let rssi: Int

    var id: UUID {
        peripheral.identifier
    }
}

enum MouseButton {
    case left
    case right
    case middle
    case backward
    case forward

    var bit: UInt8 {
        switch self {
        case .left:
            return 0x01
        case .right:
            return 0x02
        case .middle:
            return 0x04
        case .backward:
            return 0x08
        case .forward:
            return 0x10
        }
    }
}

enum HostProfile: UInt8, CaseIterable, Identifiable {
    case mac = 0x01
    case windowsLang1 = 0x02
    case windowsRightAlt = 0x03

    var id: UInt8 {
        rawValue
    }

    var label: String {
        switch self {
        case .mac:
            return "Mac"
        case .windowsLang1:
            return "Win 한/영"
        case .windowsRightAlt:
            return "Win RAlt"
        }
    }
}

struct KeyboardModifier: OptionSet {
    let rawValue: UInt8

    static let leftControl = KeyboardModifier(rawValue: 0x01)
    static let leftShift = KeyboardModifier(rawValue: 0x02)
    static let leftAlt = KeyboardModifier(rawValue: 0x04)
    static let leftGui = KeyboardModifier(rawValue: 0x08)
    static let rightControl = KeyboardModifier(rawValue: 0x10)
    static let rightShift = KeyboardModifier(rawValue: 0x20)
    static let rightAlt = KeyboardModifier(rawValue: 0x40)
    static let rightGui = KeyboardModifier(rawValue: 0x80)
}

struct KeyboardKeyCode: RawRepresentable, Equatable {
    let rawValue: UInt8

    static let backspace = KeyboardKeyCode(rawValue: 0xB2)
    static let tab = KeyboardKeyCode(rawValue: 0xB3)
    static let `return` = KeyboardKeyCode(rawValue: 0xB0)
    static let escape = KeyboardKeyCode(rawValue: 0xB1)
    static let delete = KeyboardKeyCode(rawValue: 0xD4)
    static let home = KeyboardKeyCode(rawValue: 0xD2)
    static let end = KeyboardKeyCode(rawValue: 0xD5)
    static let pageUp = KeyboardKeyCode(rawValue: 0xD3)
    static let pageDown = KeyboardKeyCode(rawValue: 0xD6)
    static let arrowRight = KeyboardKeyCode(rawValue: 0xD7)
    static let arrowLeft = KeyboardKeyCode(rawValue: 0xD8)
    static let arrowDown = KeyboardKeyCode(rawValue: 0xD9)
    static let arrowUp = KeyboardKeyCode(rawValue: 0xDA)
    static let space = KeyboardKeyCode(rawValue: 0x20)
    static let capsLock = KeyboardKeyCode(rawValue: 0xC1)
    static let rightAlt = KeyboardKeyCode(rawValue: 0x86)
    static let lang1 = KeyboardKeyCode(rawValue: 0x90)
    static let f1 = KeyboardKeyCode(rawValue: 0xC2)
    static let f2 = KeyboardKeyCode(rawValue: 0xC3)
    static let f3 = KeyboardKeyCode(rawValue: 0xC4)
    static let f4 = KeyboardKeyCode(rawValue: 0xC5)
    static let f5 = KeyboardKeyCode(rawValue: 0xC6)
    static let f6 = KeyboardKeyCode(rawValue: 0xC7)
    static let f7 = KeyboardKeyCode(rawValue: 0xC8)
    static let f8 = KeyboardKeyCode(rawValue: 0xC9)
    static let f9 = KeyboardKeyCode(rawValue: 0xCA)
    static let f10 = KeyboardKeyCode(rawValue: 0xCB)
    static let f11 = KeyboardKeyCode(rawValue: 0xCC)
    static let f12 = KeyboardKeyCode(rawValue: 0xCD)

    init(rawValue: UInt8) {
        self.rawValue = rawValue
    }

    init?(character: Character) {
        guard let scalar = String(character).unicodeScalars.first, scalar.value <= 0x7F else {
            return nil
        }
        self.rawValue = UInt8(scalar.value)
    }
}
