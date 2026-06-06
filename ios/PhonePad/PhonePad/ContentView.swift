import SwiftUI
import UIKit

struct ContentView: View {
    @EnvironmentObject private var mouseController: BLEMouseController
    @State private var pointerSensitivity = 1.2
    @State private var scrollSensitivity = 1.0
    @State private var isKeyboardPresented = false

    var body: some View {
        NavigationStack {
            GeometryReader { proxy in
                let isLandscape = proxy.size.width > proxy.size.height

                VStack(spacing: 14) {
                    connectionPanel

                    if isLandscape {
                        HStack(spacing: 14) {
                            touchSurface
                            controlColumn
                                .frame(width: min(360, proxy.size.width * 0.36))
                        }
                    } else {
                        VStack(spacing: 16) {
                            touchSurface
                            controlColumn
                        }
                    }
                }
                .padding()
                .frame(width: proxy.size.width, height: proxy.size.height)
                .background(Color(.systemGroupedBackground))
            }
            .navigationTitle("PhonePad")
            .navigationBarTitleDisplayMode(.inline)
        }
        .sheet(isPresented: $isKeyboardPresented) {
            KeyboardSheet()
                .environmentObject(mouseController)
        }
    }

    private var controlColumn: some View {
        VStack(spacing: 14) {
            buttonGrid
            keyboardButton
            settings
            Spacer(minLength: 0)
        }
    }

    private var connectionPanel: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .center, spacing: 12) {
                Image(systemName: mouseController.isConnected ? "checkmark.circle.fill" : "dot.radiowaves.left.and.right")
                    .font(.title3)
                    .foregroundStyle(mouseController.isConnected ? .green : .secondary)
                    .frame(width: 28)

                VStack(alignment: .leading, spacing: 3) {
                    Text(mouseController.isConnected ? "Connected" : "Auto connecting")
                        .font(.headline)
                    Text(mouseController.statusText)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                }

                Spacer()

                if mouseController.isConnected {
                    Button("Disconnect") {
                        mouseController.disconnect()
                    }
                    .buttonStyle(.bordered)
                } else {
                    Button(mouseController.isScanning ? "Scanning" : "Scan") {
                        mouseController.resumeAutoConnect()
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(mouseController.isScanning)
                }
            }
        }
        .padding()
        .background(Color(.secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
    }

    private var keyboardButton: some View {
        Button {
            isKeyboardPresented = true
        } label: {
            Label("Keyboard", systemImage: "keyboard")
                .frame(maxWidth: .infinity)
                .frame(height: 46)
        }
        .buttonStyle(.borderedProminent)
    }

    private var touchSurface: some View {
        TouchPadView(
            pointerSensitivity: pointerSensitivity,
            scrollSensitivity: scrollSensitivity,
            onMove: { dx, dy in
                mouseController.move(dx: dx, dy: dy, sensitivity: pointerSensitivity)
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
            ZStack {
                GridBackground()
                    .stroke(Color.secondary.opacity(0.22), lineWidth: 1)

                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .strokeBorder(Color.secondary.opacity(0.45), style: StrokeStyle(lineWidth: 1, dash: [5, 5]))
                    .padding(18)

                Image(systemName: "hand.point.up.left")
                    .font(.system(size: 38, weight: .regular))
                    .foregroundStyle(.secondary.opacity(0.38))
            }
            .allowsHitTesting(false)
        }
        .background(Color.teal.opacity(0.12))
        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .stroke(Color.secondary.opacity(0.5), lineWidth: 1)
        }
        .frame(minHeight: 180)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var buttonGrid: some View {
        HStack(spacing: 10) {
            MouseButtonView(title: "Left", systemImage: "cursorarrow.click", button: .left)
            MouseButtonView(title: "Middle", systemImage: "circle", button: .middle)
            MouseButtonView(title: "Right", systemImage: "contextualmenu.and.cursorarrow", button: .right)
        }
    }

    private var settings: some View {
        VStack(spacing: 12) {
            SliderRow(title: "Pointer", value: $pointerSensitivity, range: 0.25...3.0)
            SliderRow(title: "Scroll", value: $scrollSensitivity, range: 0.25...3.0)
        }
        .padding()
        .background(Color(.secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
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
                .background(Color(.systemGroupedBackground))
            }
            .navigationTitle("Keyboard")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") {
                        isTextKeyboardActive = false
                        dismiss()
                    }
                }
            }
        }
    }

    private var iosKeyboardControls: some View {
        HStack(spacing: 10) {
            Button {
                isTextKeyboardActive = true
            } label: {
                Label("iOS Keyboard", systemImage: "keyboard")
                    .frame(maxWidth: .infinity)
                    .frame(height: 44)
            }
            .buttonStyle(.borderedProminent)

            if isTextKeyboardActive {
                Button {
                    isTextKeyboardActive = false
                } label: {
                    Image(systemName: "keyboard.chevron.compact.down")
                        .frame(width: 46, height: 44)
                }
                .buttonStyle(.bordered)
            }
        }
    }

    private var languageTogglePanel: some View {
        VStack(spacing: 10) {
            Picker("Host", selection: $hostProfile) {
                ForEach(HostProfile.allCases) { profile in
                    Text(profile.label).tag(profile)
                }
            }
            .pickerStyle(.segmented)

            Button {
                mouseController.languageToggle(for: hostProfile)
            } label: {
                Label("한영", systemImage: "globe")
                    .frame(maxWidth: .infinity)
                    .frame(height: 44)
            }
            .buttonStyle(.bordered)
        }
        .padding()
        .background(Color(.secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
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
        LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: columns), spacing: 8) {
            ForEach(keys) { key in
                Button {
                    mouseController.tapKeyboardKey(key.keyCode)
                } label: {
                    Text(key.title)
                        .font(.body.weight(.medium))
                        .frame(maxWidth: .infinity)
                        .frame(height: 44)
                }
                .buttonStyle(.bordered)
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


private struct SliderRow: View {
    let title: String
    @Binding var value: Double
    let range: ClosedRange<Double>

    var body: some View {
        HStack(spacing: 10) {
            Text(title)
                .frame(width: 58, alignment: .leading)
            Slider(value: $value, in: range, step: 0.05)
            Text(value, format: .number.precision(.fractionLength(2)))
                .font(.caption.monospacedDigit())
                .foregroundStyle(.secondary)
                .frame(width: 42, alignment: .trailing)
        }
    }
}

private struct MouseButtonView: View {
    @EnvironmentObject private var mouseController: BLEMouseController
    @State private var isPressed = false
    @State private var isLocked = false
    @State private var didLockDuringCurrentPress = false
    @State private var lockTask: DispatchWorkItem?

    let title: String
    let systemImage: String
    let button: MouseButton

    var body: some View {
        Label(title, systemImage: isLocked ? "lock.fill" : systemImage)
            .font(.body.weight(.medium))
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(buttonBackground)
            .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .stroke(isLocked ? Color.accentColor : Color.secondary.opacity(0.45), lineWidth: 1)
            }
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
    }

    private var buttonBackground: Color {
        if isLocked {
            return Color.accentColor.opacity(0.30)
        }

        if isPressed {
            return Color.accentColor.opacity(0.22)
        }

        return Color(.secondarySystemGroupedBackground)
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
    let pointerSensitivity: Double
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

private final class TouchPadUIView: UIView {
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
    private var startPoint: CGPoint?
    private var touchStartCount = 0
    private var touchStartTime = CACurrentMediaTime()
    private var lastMoveTime = CACurrentMediaTime()
    private var velocity = CGPoint.zero
    private var scrollVelocity = 0.0
    private var scrollRemainder = 0.0
    private var inertiaLink: CADisplayLink?
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
        stopInertia()
        stopScrollInertia()

        guard let event, let centroid = centroid(for: event.allTouches ?? touches) else { return }
        touchStartCount = event.allTouches?.count ?? touches.count
        mode = touchStartCount >= 2 ? .scroll : .pointer
        lastCentroid = centroid
        startPoint = centroid
        touchStartTime = CACurrentMediaTime()
        lastMoveTime = touchStartTime
        velocity = .zero
        scrollVelocity = 0
        scrollRemainder = 0
    }

    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard let event, let centroid = centroid(for: event.allTouches ?? touches), let lastCentroid else { return }

        let now = CACurrentMediaTime()
        let dt = max(0.001, now - lastMoveTime)
        let dx = centroid.x - lastCentroid.x
        let dy = centroid.y - lastCentroid.y

        if (event.allTouches?.count ?? touches.count) >= 2 {
            mode = .scroll
            scroll(by: (-dy / 10.0) * scrollSensitivity)
            scrollVelocity = (-Double(dy) / dt / 7.5) * scrollSensitivity
        } else if mode == .pointer {
            onMove?(dx, dy)
            velocity = CGPoint(x: dx / dt, y: dy / dt)
        }

        self.lastCentroid = centroid
        lastMoveTime = now
    }

    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        let allTouchesEnded = event?.allTouches?.allSatisfy { $0.phase == .ended || $0.phase == .cancelled } ?? true
        guard allTouchesEnded else { return }

        if mode == .pointer, isTapCandidate() {
            handleTap()
        } else if mode == .pointer {
            startInertiaIfNeeded()
        } else if mode == .scroll {
            if touchStartCount >= 2, isTapCandidate() {
                onTwoFingerTap?()
            } else {
                startScrollInertiaIfNeeded()
            }
        }

        mode = .none
        lastCentroid = nil
        startPoint = nil
        touchStartCount = 0
    }

    override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        mode = .none
        lastCentroid = nil
        startPoint = nil
        touchStartCount = 0
        stopInertia()
        stopScrollInertia()
    }

    private func centroid(for touches: Set<UITouch>) -> CGPoint? {
        guard !touches.isEmpty else { return nil }

        let points = touches.map { $0.location(in: self) }
        let x = points.reduce(0) { $0 + $1.x } / CGFloat(points.count)
        let y = points.reduce(0) { $0 + $1.y } / CGFloat(points.count)
        return CGPoint(x: x, y: y)
    }

    private func isTapCandidate() -> Bool {
        guard let startPoint, let lastCentroid else { return false }

        let distance = hypot(lastCentroid.x - startPoint.x, lastCentroid.y - startPoint.y)
        let duration = CACurrentMediaTime() - touchStartTime
        return distance < 12 && duration < 0.28
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

    private func startInertiaIfNeeded() {
        let speed = hypot(velocity.x, velocity.y)
        guard speed > 320 else { return }

        inertiaLink = CADisplayLink(target: self, selector: #selector(stepInertia))
        inertiaLink?.add(to: .main, forMode: .common)
    }

    @objc private func stepInertia(_ link: CADisplayLink) {
        let dt = min(link.duration, 1.0 / 30.0)
        let dx = Double(velocity.x * dt)
        let dy = Double(velocity.y * dt)

        onMove?(dx, dy)
        velocity.x *= 0.90
        velocity.y *= 0.90

        if hypot(velocity.x, velocity.y) < 18 {
            stopInertia()
        }
    }

    private func stopInertia() {
        inertiaLink?.invalidate()
        inertiaLink = nil
    }

    private func scroll(by amount: Double) {
        scrollRemainder += amount

        let wholeTicks = Int(scrollRemainder.rounded(.towardZero))
        guard wholeTicks != 0 else { return }

        scrollRemainder -= Double(wholeTicks)
        onScroll?(clampInt8(Double(wholeTicks)))
    }

    private func startScrollInertiaIfNeeded() {
        guard abs(scrollVelocity) > 7 else { return }

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

private struct GridBackground: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        let step: CGFloat = 36

        stride(from: rect.minX, through: rect.maxX, by: step).forEach { x in
            path.move(to: CGPoint(x: x, y: rect.minY))
            path.addLine(to: CGPoint(x: x, y: rect.maxY))
        }

        stride(from: rect.minY, through: rect.maxY, by: step).forEach { y in
            path.move(to: CGPoint(x: rect.minX, y: y))
            path.addLine(to: CGPoint(x: rect.maxX, y: y))
        }

        return path
    }
}
