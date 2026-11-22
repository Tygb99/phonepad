import SwiftUI
import UIKit

// Look follows the Android app (MainActivity.kt COLOR_* values): dark panels, flat rounded
// buttons with text labels, touchpad on the left and a two-column control stack on the right.
private enum PadTheme {
    static let background = rgb(10, 12, 15)
    static let panel = rgb(23, 27, 34)
    static let info = rgb(35, 41, 50)
    static let stroke = rgb(72, 83, 100)
    static let trackpad = rgb(15, 19, 25)
    static let trackpadStroke = rgb(94, 111, 134)
    static let trackpadLabel = rgb(136, 149, 169)
    static let trackpadReady = rgb(223, 232, 244)
    static let muted = rgb(166, 177, 194)
    static let accent = rgb(132, 189, 177)
    static let button = rgb(46, 55, 68)
    static let buttonPressed = rgb(62, 74, 91)
    static let buttonStroke = rgb(96, 112, 134)
    static let lockActive = rgb(169, 57, 68)
    static let lockStroke = rgb(236, 118, 130)
    static let inputActive = rgb(58, 106, 92)
    static let inputStroke = rgb(132, 207, 178)
    static let cornerRadius: CGFloat = 8
    static let buttonFont = Font.system(size: 15, weight: .semibold)

    private static func rgb(_ red: Double, _ green: Double, _ blue: Double) -> Color {
        Color(red: red / 255, green: green / 255, blue: blue / 255)
    }
}

private extension View {
    func padSurface(_ fill: Color, stroke: Color) -> some View {
        background(
            RoundedRectangle(cornerRadius: PadTheme.cornerRadius, style: .continuous).fill(fill)
        )
        .overlay(
            RoundedRectangle(cornerRadius: PadTheme.cornerRadius, style: .continuous)
                .strokeBorder(stroke, lineWidth: 1)
        )
    }

    func padButtonLabel() -> some View {
        font(PadTheme.buttonFont)
            .foregroundStyle(.white)
            .lineLimit(1)
            .minimumScaleFactor(0.6)
            .padding(.horizontal, 6)
            .frame(maxWidth: .infinity, minHeight: 36, maxHeight: .infinity)
    }
}

private struct PadButtonStyle: ButtonStyle {
    @Environment(\.isEnabled) private var isEnabled
    var fill = PadTheme.button
    var stroke = PadTheme.buttonStroke

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .padButtonLabel()
            .padSurface(configuration.isPressed ? PadTheme.buttonPressed : fill, stroke: stroke)
            .contentShape(Rectangle())
            .opacity(isEnabled ? 1 : 0.5)
    }
}

struct ContentView: View {
    @EnvironmentObject private var mouseController: BLEMouseController
    @State private var pointerSensitivity = 1.0
    @State private var scrollSensitivity = 1.0
    @State private var scrollRepeater = ContinuousScrollRepeater()
    @State private var isKeyboardPresented = false

    var body: some View {
        GeometryReader { proxy in
            let isLandscape = proxy.size.width > proxy.size.height

            Group {
                if isLandscape {
                    HStack(spacing: 14) {
                        touchpadColumn
                        controlColumn
                            .frame(width: min(300, proxy.size.width * 0.40))
                    }
                } else {
                    VStack(spacing: 12) {
                        touchpadColumn
                        controlColumn
                            .frame(height: min(300, proxy.size.height * 0.42))
                    }
                }
            }
            .padding(14)
            .padSurface(PadTheme.panel, stroke: PadTheme.stroke)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .frame(width: proxy.size.width, height: proxy.size.height)
        }
        .background(PadTheme.background.ignoresSafeArea())
        .preferredColorScheme(.dark)
        .sheet(isPresented: $isKeyboardPresented) {
            KeyboardSheet()
                .environmentObject(mouseController)
                .preferredColorScheme(.dark)
        }
    }

    private var touchpadColumn: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("터치패드 모드")
                .font(.system(size: 22, weight: .bold))
                .foregroundStyle(.white)
                .lineLimit(1)
                .minimumScaleFactor(0.7)

            Text("한 손가락 이동 · 두 손가락 스크롤 · 탭 클릭")
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(PadTheme.muted)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
                .padding(.top, 2)
                .padding(.bottom, 10)

            touchSurface

            HStack(spacing: 6) {
                SliderRow(title: "포인터", value: $pointerSensitivity, range: 0.25...3.0)
                SliderRow(title: "스크롤", value: $scrollSensitivity, range: 0.25...3.0)
            }
            .padding(.top, 10)
        }
    }

    private var controlColumn: some View {
        VStack(spacing: 6) {
            statusChip
                .frame(maxHeight: .infinity)

            Button(connectionButtonTitle) {
                if mouseController.isConnected {
                    mouseController.disconnect()
                } else {
                    mouseController.resumeAutoConnect()
                }
            }
            .buttonStyle(PadButtonStyle(
                fill: mouseController.isConnected ? PadTheme.button : PadTheme.inputActive,
                stroke: mouseController.isConnected ? PadTheme.buttonStroke : PadTheme.inputStroke
            ))
            .disabled(!mouseController.isConnected && mouseController.isScanning)
            .frame(maxHeight: .infinity)

            HStack(spacing: 6) {
                Button("키보드") {
                    isKeyboardPresented = true
                }
                .buttonStyle(PadButtonStyle())
                MouseButtonView(title: "가운데 클릭", button: .middle)
            }
            .frame(maxHeight: .infinity)

            HStack(spacing: 6) {
                MouseButtonView(title: "왼쪽 클릭", button: .left)
                MouseButtonView(title: "오른쪽 클릭", button: .right)
            }
            .frame(maxHeight: .infinity)

            HStack(spacing: 6) {
                ScrollHoldButton(
                    title: "스크롤 ↑",
                    initialAmount: 2,
                    repeatAmount: 1,
                    repeater: scrollRepeater
                ) { amount in
                    mouseController.wheel(amount)
                }

                ScrollHoldButton(
                    title: "스크롤 ↓",
                    initialAmount: -2,
                    repeatAmount: -1,
                    repeater: scrollRepeater
                ) { amount in
                    mouseController.wheel(amount)
                }
            }
            .frame(maxHeight: .infinity)
        }
    }

    private var connectionButtonTitle: String {
        if mouseController.isConnected {
            return "연결 해제"
        }
        return mouseController.isScanning ? "찾는 중" : "연결!"
    }

    private var statusChip: some View {
        let connected = mouseController.isConnected
        return VStack(spacing: 2) {
            Text(connected ? "연결됨" : "자동 연결 중")
                .font(.system(size: 14, weight: .bold))
                .foregroundStyle(.white)
            Text(mouseController.statusText)
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(connected ? PadTheme.trackpadReady : PadTheme.muted)
        }
        .lineLimit(1)
        .minimumScaleFactor(0.6)
        .padding(.horizontal, 8)
        .frame(maxWidth: .infinity, minHeight: 36, maxHeight: .infinity)
        .padSurface(
            connected ? PadTheme.inputActive : PadTheme.info,
            stroke: connected ? PadTheme.inputStroke : PadTheme.stroke
        )
    }

    private var touchSurface: some View {
        TouchPadView(
            scrollSensitivity: scrollSensitivity,
            onMove: { dx, dy in
                mouseController.move(
                    dx: dx * pointerSensitivity,
                    dy: dy * pointerSensitivity
                )
            },
            onScroll: { amount in
                mouseController.wheel(amount)
            },
            onTap: {
                mouseController.click(.left)
            },
            onDoubleTap: {
                mouseController.doubleClick(.left)
            },
            onTwoFingerTap: {
                mouseController.click(.right)
            }
        )
        .overlay {
            Text(mouseController.isConnected
                 ? "터치패드 활성 · 한 손가락 이동 / 두 손가락 스크롤 / 탭 클릭"
                 : "호스트 연결 대기 중")
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(mouseController.isConnected ? PadTheme.trackpadReady : PadTheme.trackpadLabel)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 18)
                .allowsHitTesting(false)
        }
        .background(PadTheme.trackpad)
        .clipShape(RoundedRectangle(cornerRadius: PadTheme.cornerRadius, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: PadTheme.cornerRadius, style: .continuous)
                .strokeBorder(PadTheme.trackpadStroke, lineWidth: 1)
        }
        .frame(minHeight: 140)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct KeyboardSheet: View {
    @EnvironmentObject private var mouseController: BLEMouseController
    @Environment(\.dismiss) private var dismiss
    @State private var isTextKeyboardActive = false
    @State private var hostProfile = HostProfile.mac

    private let functionKeys: [KeyboardButtonSpec] = [
        .init("Esc", .escape),
        .init("F1", .f1),
        .init("F2", .f2),
        .init("F3", .f3),
        .init("F4", .f4),
        .init("F5", .f5),
        .init("F6", .f6),
        .init("F7", .f7),
        .init("F8", .f8),
        .init("F9", .f9),
        .init("F10", .f10),
        .init("F11", .f11),
        .init("F12", .f12)
    ]

    private let navigationKeys: [KeyboardButtonSpec] = [
        .init("⌫", .backspace),
        .init("Tab", .tab),
        .init("Del", .delete),
        .init("Home", .home),
        .init("End", .end),
        .init("PgUp", .pageUp),
        .init("PgDn", .pageDown),
        .init("Up", .arrowUp),
        .init("Left", .arrowLeft),
        .init("Down", .arrowDown),
        .init("Right", .arrowRight),
        .init("Return", .return),
        .init("Space", .space)
    ]

    var body: some View {
        NavigationStack {
            GeometryReader { proxy in
                ScrollView {
                    VStack(spacing: 16) {
                        iosKeyboardControls
                        languageTogglePanel
                        directKeyPanel(columns: proxy.size.width > 620 ? 8 : 4)
                    }
                    .padding()
                    .frame(maxWidth: .infinity)
                }
                .overlay(alignment: .topLeading) {
                    TextKeyboardCaptureView(
                        isActive: isTextKeyboardActive,
                        onText: sendTextInput,
                        onBackspace: {
                            mouseController.tapKeyboardKey(.backspace)
                        },
                        onReturn: {
                            mouseController.tapKeyboardKey(.return)
                        }
                    )
                    .frame(width: 1, height: 1)
                    .opacity(0.01)
                    .accessibilityHidden(true)
                }
                .background(PadTheme.background.ignoresSafeArea())
            }
            .navigationTitle("키보드")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("닫기") {
                        isTextKeyboardActive = false
                        dismiss()
                    }
                }
            }
        }
    }

    private var iosKeyboardControls: some View {
        HStack(spacing: 6) {
            Button("iOS 키보드") {
                isTextKeyboardActive = true
            }
            .buttonStyle(PadButtonStyle(fill: PadTheme.inputActive, stroke: PadTheme.inputStroke))

            if isTextKeyboardActive {
                Button {
                    isTextKeyboardActive = false
                } label: {
                    Image(systemName: "keyboard.chevron.compact.down")
                }
                .buttonStyle(PadButtonStyle())
                .frame(width: 56)
            }
        }
        .frame(height: 44)
    }

    private var languageTogglePanel: some View {
        VStack(spacing: 10) {
            Picker("Host", selection: $hostProfile) {
                ForEach(HostProfile.allCases) { profile in
                    Text(profile.label).tag(profile)
                }
            }
            .pickerStyle(.segmented)

            Button("한영") {
                mouseController.languageToggle(for: hostProfile)
            }
            .buttonStyle(PadButtonStyle())
            .frame(height: 44)
        }
        .padding(14)
        .padSurface(PadTheme.panel, stroke: PadTheme.stroke)
    }

    private func sendTextInput(_ text: String) {
        for character in text {
            guard let keyCode = KeyboardKeyCode(character: character) else {
                continue
            }
            mouseController.tapKeyboardKey(keyCode)
        }
    }

    private func directKeyPanel(columns: Int) -> some View {
        VStack(spacing: 14) {
            keyGrid(functionKeys, columns: columns)
            keyGrid(navigationKeys, columns: columns)
        }
    }

    private func keyGrid(_ keys: [KeyboardButtonSpec], columns: Int) -> some View {
        LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 6), count: columns), spacing: 6) {
            ForEach(keys) { key in
                Button(key.title) {
                    mouseController.tapKeyboardKey(key.keyCode)
                }
                .buttonStyle(PadButtonStyle())
                .frame(height: 44)
            }
        }
    }
}

private struct KeyboardButtonSpec: Identifiable {
    let title: String
    let keyCode: KeyboardKeyCode

    var id: String {
        title
    }

    init(_ title: String, _ keyCode: KeyboardKeyCode) {
        self.title = title
        self.keyCode = keyCode
    }
}

private struct TextKeyboardCaptureView: UIViewRepresentable {
    let isActive: Bool
    let onText: (String) -> Void
    let onBackspace: () -> Void
    let onReturn: () -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator(onText: onText, onBackspace: onBackspace, onReturn: onReturn)
    }

    func makeUIView(context: Context) -> UITextView {
        let textView = UITextView()
        textView.delegate = context.coordinator
        textView.backgroundColor = .clear
        textView.autocorrectionType = .no
        textView.autocapitalizationType = .none
        textView.spellCheckingType = .no
        textView.keyboardType = .default
        textView.returnKeyType = .default
        textView.text = " "
        return textView
    }

    func updateUIView(_ uiView: UITextView, context: Context) {
        context.coordinator.onText = onText
        context.coordinator.onBackspace = onBackspace
        context.coordinator.onReturn = onReturn

        if isActive && !uiView.isFirstResponder {
            DispatchQueue.main.async {
                uiView.becomeFirstResponder()
            }
        } else if !isActive && uiView.isFirstResponder {
            DispatchQueue.main.async {
                uiView.resignFirstResponder()
            }
        }
    }

    final class Coordinator: NSObject, UITextViewDelegate {
        var onText: (String) -> Void
        var onBackspace: () -> Void
        var onReturn: () -> Void

        init(onText: @escaping (String) -> Void, onBackspace: @escaping () -> Void, onReturn: @escaping () -> Void) {
            self.onText = onText
            self.onBackspace = onBackspace
            self.onReturn = onReturn
        }

        func textView(
            _ textView: UITextView,
            shouldChangeTextIn range: NSRange,
            replacementText text: String
        ) -> Bool {
            if text.isEmpty {
                onBackspace()
            } else if text == "\n" {
                onReturn()
            } else {
                onText(text)
            }

            textView.text = " "
            return false
        }
    }
}


@MainActor
final class ContinuousScrollRepeater {
    private let initialDelayNanoseconds: UInt64
    private let repeatIntervalNanoseconds: UInt64
    private var repeatTask: Task<Void, Never>?

    init(
        initialDelayNanoseconds: UInt64 = 260_000_000,
        repeatIntervalNanoseconds: UInt64 = 135_000_000
    ) {
        self.initialDelayNanoseconds = initialDelayNanoseconds
        self.repeatIntervalNanoseconds = repeatIntervalNanoseconds
    }

    func start(initialAmount: Int8, repeatAmount: Int8, action: @escaping (Int8) -> Void) {
        stop()
        action(initialAmount)

        let initialDelayNanoseconds = initialDelayNanoseconds
        let repeatIntervalNanoseconds = repeatIntervalNanoseconds
        repeatTask = Task { @MainActor in
            do {
                try await Task.sleep(nanoseconds: initialDelayNanoseconds)
                while !Task.isCancelled {
                    action(repeatAmount)
                    try await Task.sleep(nanoseconds: repeatIntervalNanoseconds)
                }
            } catch {
                return
            }
        }
    }

    func stop() {
        repeatTask?.cancel()
        repeatTask = nil
    }

    deinit {
        repeatTask?.cancel()
    }
}

private struct ScrollHoldButton: View {
    @Environment(\.scenePhase) private var scenePhase
    @State private var isPressed = false

    let title: String
    let initialAmount: Int8
    let repeatAmount: Int8
    let repeater: ContinuousScrollRepeater
    let onScroll: (Int8) -> Void

    var body: some View {
        Text(title)
            .padButtonLabel()
            .padSurface(isPressed ? PadTheme.buttonPressed : PadTheme.button, stroke: PadTheme.buttonStroke)
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { _ in
                        guard !isPressed else { return }
                        isPressed = true
                        repeater.start(
                            initialAmount: initialAmount,
                            repeatAmount: repeatAmount,
                            action: onScroll
                        )
                    }
                    .onEnded { _ in
                        stopScrolling()
                    }
            )
        .accessibilityElement(children: .combine)
        .accessibilityLabel(title)
        .accessibilityAddTraits(.isButton)
        .accessibilityAction {
            onScroll(initialAmount)
        }
        .onChange(of: scenePhase) { _, newPhase in
            if newPhase != .active {
                stopScrolling()
            }
        }
        .onDisappear {
            stopScrolling()
        }
    }

    private func stopScrolling() {
        isPressed = false
        repeater.stop()
    }
}

private struct SliderRow: View {
    let title: String
    @Binding var value: Double
    let range: ClosedRange<Double>

    var body: some View {
        HStack(spacing: 8) {
            Text(title)
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(PadTheme.muted)
                .lineLimit(1)
                .fixedSize()
            Slider(value: $value, in: range, step: 0.05)
                .tint(PadTheme.accent)
        }
        .padding(.horizontal, 10)
        .frame(maxWidth: .infinity)
        .frame(height: 40)
        .padSurface(PadTheme.info, stroke: PadTheme.stroke)
    }
}

private struct MouseButtonView: View {
    @EnvironmentObject private var mouseController: BLEMouseController
    @Environment(\.scenePhase) private var scenePhase
    @State private var isPressed = false
    @State private var isLocked = false
    @State private var didLockDuringCurrentPress = false
    @State private var lockTask: DispatchWorkItem?

    let title: String
    let button: MouseButton

    var body: some View {
        HStack(spacing: 4) {
            if isLocked {
                Image(systemName: "lock.fill")
            }
            Text(title)
        }
            .padButtonLabel()
            .padSurface(buttonBackground, stroke: isLocked ? PadTheme.lockStroke : PadTheme.buttonStroke)
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { _ in
                        guard !isPressed && !isLocked else { return }
                        pressMomentarily()
                    }
                    .onEnded { _ in
                        if isLocked {
                            if didLockDuringCurrentPress {
                                finishLockingPress()
                            } else {
                                unlock()
                            }
                        } else {
                            releaseMomentaryPress()
                        }
                    }
            )
            .onChange(of: scenePhase) { _, phase in
                if phase != .active { unlock() }
            }
            .onChange(of: mouseController.isConnected) { _, connected in
                if !connected { unlock() }
            }
            .onDisappear { unlock() }
    }

    private var buttonBackground: Color {
        if isLocked {
            return PadTheme.lockActive
        }

        if isPressed {
            return PadTheme.buttonPressed
        }

        return PadTheme.button
    }

    private func pressMomentarily() {
        isPressed = true
        mouseController.setButton(button, pressed: true)

        let task = DispatchWorkItem {
            guard isPressed && !isLocked else { return }
            isLocked = true
            didLockDuringCurrentPress = true
        }
        lockTask = task
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0, execute: task)
    }

    private func releaseMomentaryPress() {
        lockTask?.cancel()
        lockTask = nil
        isPressed = false
        didLockDuringCurrentPress = false
        mouseController.setButton(button, pressed: false)
    }

    private func finishLockingPress() {
        lockTask?.cancel()
        lockTask = nil
        isPressed = false
        didLockDuringCurrentPress = false
    }

    private func unlock() {
        lockTask?.cancel()
        lockTask = nil
        isLocked = false
        isPressed = false
        didLockDuringCurrentPress = false
        mouseController.setButton(button, pressed: false)
    }
}

private struct TouchPadView: UIViewRepresentable {
    let scrollSensitivity: Double
    let onMove: (Double, Double) -> Void
    let onScroll: (Int8) -> Void
    let onTap: () -> Void
    let onDoubleTap: () -> Void
    let onTwoFingerTap: () -> Void

    func makeUIView(context: Context) -> TouchPadUIView {
        let view = TouchPadUIView()
        view.onMove = onMove
        view.onScroll = onScroll
        view.onTap = onTap
        view.onDoubleTap = onDoubleTap
        view.onTwoFingerTap = onTwoFingerTap
        view.scrollSensitivity = scrollSensitivity
        return view
    }

    func updateUIView(_ uiView: TouchPadUIView, context: Context) {
        uiView.onMove = onMove
        uiView.onScroll = onScroll
        uiView.onTap = onTap
        uiView.onDoubleTap = onDoubleTap
        uiView.onTwoFingerTap = onTwoFingerTap
        uiView.scrollSensitivity = scrollSensitivity
    }
}

final class TouchPadUIView: UIView {
    var onMove: ((Double, Double) -> Void)?
    var onScroll: ((Int8) -> Void)?
    var onTap: (() -> Void)?
    var onDoubleTap: (() -> Void)?
    var onTwoFingerTap: (() -> Void)?
    var scrollSensitivity = 1.0

    private enum Mode {
        case none
        case pointer
        case scroll
    }

    private var mode = Mode.none
    private var lastCentroid: CGPoint?
    private var tapTravel: CGVector?
    private var tapPathLength: CGFloat = 0
    private var touchStartCount = 0
    private var touchStartTime = CACurrentMediaTime()
    private var lastMoveTime = CACurrentMediaTime()
    private var scrollVelocity = 0.0
    private var lastScrollMoveTime = 0.0
    private var scrollRemainder = 0.0
    private var scrollInertiaLink: CADisplayLink?
    private var lastTapTime = 0.0
    private var pendingSingleTap: DispatchWorkItem?

    override init(frame: CGRect) {
        super.init(frame: frame)
        isMultipleTouchEnabled = true
        backgroundColor = .clear
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        isMultipleTouchEnabled = true
        backgroundColor = .clear
    }

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        stopScrollInertia()

        guard let event else { return }
        let localTouches = event.touches(for: self) ?? touches
        guard let centroid = centroid(for: localTouches) else { return }
        touchStartCount = localTouches.count
        mode = touchStartCount >= 2 ? .scroll : .pointer
        lastCentroid = centroid
        tapTravel = .zero
        tapPathLength = 0
        touchStartTime = CACurrentMediaTime()
        lastMoveTime = touchStartTime
        scrollVelocity = 0
        scrollRemainder = 0
    }

    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let event else { return }
        let localTouches = (event.touches(for: self) ?? touches).filter {
            $0.phase != .ended && $0.phase != .cancelled
        }
        guard let centroid = centroid(for: localTouches), let lastCentroid else { return }

        let now = CACurrentMediaTime()
        let dt = max(0.001, now - lastMoveTime)
        let dx = centroid.x - lastCentroid.x
        let dy = centroid.y - lastCentroid.y

        if localTouches.count >= 2 {
            mode = .scroll
            scroll(by: (-dy / 10.0) * scrollSensitivity)
            scrollVelocity = (-Double(dy) / dt / 7.5) * scrollSensitivity
            lastScrollMoveTime = now
        } else if mode == .pointer {
            onMove?(dx, dy)
        }

        tapTravel?.dx += dx
        tapTravel?.dy += dy
        tapPathLength += hypot(dx, dy)
        self.lastCentroid = centroid
        lastMoveTime = now
    }

    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        let localTouches = event?.touches(for: self) ?? touches
        let allTouchesEnded = localTouches.allSatisfy { $0.phase == .ended || $0.phase == .cancelled }
        guard allTouchesEnded else {
            lastCentroid = centroid(for: localTouches.filter {
                $0.phase != .ended && $0.phase != .cancelled
            })
            scrollRemainder = 0
            lastMoveTime = CACurrentMediaTime()
            return
        }

        if mode == .pointer, isTapCandidate() {
            handleTap()
        } else if mode == .scroll {
            if touchStartCount >= 2, isTapCandidate() {
                onTwoFingerTap?()
            } else {
                startScrollInertiaIfNeeded()
            }
        }

        mode = .none
        lastCentroid = nil
        tapTravel = nil
        touchStartCount = 0
    }

    override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        mode = .none
        lastCentroid = nil
        tapTravel = nil
        touchStartCount = 0
        stopScrollInertia()
    }

    private func centroid(for touches: Set<UITouch>) -> CGPoint? {
        guard !touches.isEmpty else { return nil }

        let points = touches.map { $0.location(in: self) }
        let x = points.reduce(0) { $0 + $1.x } / CGFloat(points.count)
        let y = points.reduce(0) { $0 + $1.y } / CGFloat(points.count)
        return CGPoint(x: x, y: y)
    }

    // Same rule as Android TapPolicy: the whole travelled path must stay inside the slop, and a
    // touch that ends noticeably away from its start after a held moment was a cursor nudge.
    private static let tapSlop: CGFloat = 12
    private static let tapMaxDuration: CFTimeInterval = 0.28
    private static let smallMoveNet: CGFloat = 4
    private static let smallMoveMinDuration: CFTimeInterval = 0.15

    private func isTapCandidate() -> Bool {
        guard let tapTravel else { return false }

        let net = hypot(tapTravel.dx, tapTravel.dy)
        let duration = CACurrentMediaTime() - touchStartTime
        let deliberateSmallMove = net >= Self.smallMoveNet && duration >= Self.smallMoveMinDuration
        return tapPathLength < Self.tapSlop && duration < Self.tapMaxDuration && !deliberateSmallMove
    }

    private func handleTap() {
        let now = CACurrentMediaTime()
        if now - lastTapTime < 0.32 {
            pendingSingleTap?.cancel()
            pendingSingleTap = nil
            lastTapTime = 0
            onDoubleTap?()
            return
        }

        lastTapTime = now
        let tap = DispatchWorkItem { [weak self] in
            self?.onTap?()
            self?.pendingSingleTap = nil
        }
        pendingSingleTap = tap
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.24, execute: tap)
    }

    private func scroll(by amount: Double) {
        scrollRemainder += amount

        let wholeTicks = Int(scrollRemainder.rounded(.towardZero))
        guard wholeTicks != 0 else { return }

        scrollRemainder -= Double(wholeTicks)
        onScroll?(clampInt8(Double(wholeTicks)))
    }

    private func startScrollInertiaIfNeeded() {
        guard abs(scrollVelocity) > 7, CACurrentMediaTime() - lastScrollMoveTime < 0.15 else { return }

        scrollInertiaLink = CADisplayLink(target: self, selector: #selector(stepScrollInertia))
        scrollInertiaLink?.add(to: .main, forMode: .common)
    }

    @objc private func stepScrollInertia(_ link: CADisplayLink) {
        let dt = min(link.duration, 1.0 / 30.0)
        scroll(by: scrollVelocity * dt)
        scrollVelocity *= 0.94

        if abs(scrollVelocity) < 0.35 {
            stopScrollInertia()
            scrollRemainder = 0
        }
    }

    private func stopScrollInertia() {
        scrollInertiaLink?.invalidate()
        scrollInertiaLink = nil
    }

    private func clampInt8(_ value: Double) -> Int8 {
        let rounded = Int(value.rounded())
        return Int8(max(-127, min(127, rounded)))
    }
}
