import UIKit
import XCTest
@testable import PhonePad

final class TouchPadUIViewTests: XCTestCase {
    @MainActor
    func testContinuousScrollRepeaterSendsInitialThenRepeatsUntilStopped() async throws {
        let repeater = ContinuousScrollRepeater(
            initialDelayNanoseconds: 20_000_000,
            repeatIntervalNanoseconds: 15_000_000
        )
        var amounts: [Int8] = []

        repeater.start(initialAmount: 2, repeatAmount: 1) { amount in
            amounts.append(amount)
        }

        XCTAssertEqual(amounts, [2])
        try await Task.sleep(nanoseconds: 70_000_000)
        XCTAssertGreaterThanOrEqual(amounts.count, 3)
        XCTAssertTrue(amounts.dropFirst().allSatisfy { $0 == 1 })

        repeater.stop()
        let stoppedCount = amounts.count
        try await Task.sleep(nanoseconds: 40_000_000)
        XCTAssertEqual(amounts.count, stoppedCount)
    }

    @MainActor
    func testContinuousScrollRepeaterStopsBeforeFirstRepeat() async throws {
        let repeater = ContinuousScrollRepeater(
            initialDelayNanoseconds: 30_000_000,
            repeatIntervalNanoseconds: 15_000_000
        )
        var amounts: [Int8] = []

        repeater.start(initialAmount: -2, repeatAmount: -1) { amount in
            amounts.append(amount)
        }
        repeater.stop()

        try await Task.sleep(nanoseconds: 60_000_000)
        XCTAssertEqual(amounts, [-2])
    }

    func testExternalButtonAndOneLocalTouchRoutesMovementToPointer() {
        let touchPad = TouchPadUIView(frame: CGRect(x: 0, y: 0, width: 200, height: 200))
        let externalButton = UIView(frame: CGRect(x: 0, y: 0, width: 40, height: 40))
        let localTouch = FakeTouch(phase: .began, location: CGPoint(x: 20, y: 20))
        let externalTouch = FakeTouch(phase: .stationary, location: CGPoint(x: 10, y: 10))
        let event = FakeEvent(
            allTouches: [localTouch, externalTouch],
            touchesByView: [
                ObjectIdentifier(touchPad): [localTouch],
                ObjectIdentifier(externalButton): [externalTouch],
            ]
        )
        let moved = expectation(description: "Local movement is routed to pointer callback")

        touchPad.onMove = { dx, dy in
            XCTAssertEqual(dx, 10, accuracy: 0.001)
            XCTAssertEqual(dy, 0, accuracy: 0.001)
            moved.fulfill()
        }

        touchPad.touchesBegan([localTouch], with: event)
        localTouch.fakePhase = .moved
        localTouch.fakeLocation = CGPoint(x: 30, y: 20)
        touchPad.touchesMoved([localTouch], with: event)

        wait(for: [moved], timeout: 0.1)
    }

    func testPointerMovementStopsWhenLastTouchEnds() {
        let touchPad = TouchPadUIView(frame: CGRect(x: 0, y: 0, width: 200, height: 200))
        let localTouch = FakeTouch(phase: .began, location: CGPoint(x: 20, y: 20))
        let event = FakeEvent(
            allTouches: [localTouch],
            touchesByView: [ObjectIdentifier(touchPad): [localTouch]]
        )
        let movementAfterTouchEnd = expectation(description: "Pointer movement stops after touch end")
        movementAfterTouchEnd.isInverted = true
        var movementCount = 0

        touchPad.onMove = { _, _ in
            movementCount += 1
            if movementCount == 2 {
                movementAfterTouchEnd.fulfill()
            }
        }

        touchPad.touchesBegan([localTouch], with: event)
        localTouch.fakePhase = .moved
        localTouch.fakeLocation = CGPoint(x: 150, y: 20)
        touchPad.touchesMoved([localTouch], with: event)
        localTouch.fakePhase = .ended
        touchPad.touchesEnded([localTouch], with: event)

        wait(for: [movementAfterTouchEnd], timeout: 0.15)
        XCTAssertEqual(movementCount, 1)
    }

    func testTwoLocalTouchesRouteMovementToScroll() {
        let touchPad = TouchPadUIView(frame: CGRect(x: 0, y: 0, width: 200, height: 200))
        let firstTouch = FakeTouch(phase: .began, location: CGPoint(x: 20, y: 20))
        let secondTouch = FakeTouch(phase: .began, location: CGPoint(x: 40, y: 20))
        let event = FakeEvent(
            allTouches: [firstTouch, secondTouch],
            touchesByView: [ObjectIdentifier(touchPad): [firstTouch, secondTouch]]
        )
        let scrolled = expectation(description: "Two local touches are routed to scroll callback")

        touchPad.onScroll = { ticks in
            XCTAssertEqual(ticks, -2)
            scrolled.fulfill()
        }

        touchPad.touchesBegan([firstTouch, secondTouch], with: event)
        firstTouch.fakePhase = .moved
        secondTouch.fakePhase = .moved
        firstTouch.fakeLocation = CGPoint(x: 20, y: 40)
        secondTouch.fakeLocation = CGPoint(x: 40, y: 40)
        touchPad.touchesMoved([firstTouch, secondTouch], with: event)

        wait(for: [scrolled], timeout: 0.1)
    }

    func testEndingLastLocalTouchEmitsTapWhenExternalTouchRemains() {
        let touchPad = TouchPadUIView(frame: CGRect(x: 0, y: 0, width: 200, height: 200))
        let externalButton = UIView(frame: CGRect(x: 0, y: 0, width: 40, height: 40))
        let localTouch = FakeTouch(phase: .began, location: CGPoint(x: 20, y: 20))
        let externalTouch = FakeTouch(phase: .stationary, location: CGPoint(x: 10, y: 10))
        let event = FakeEvent(
            allTouches: [localTouch, externalTouch],
            touchesByView: [
                ObjectIdentifier(touchPad): [localTouch],
                ObjectIdentifier(externalButton): [externalTouch],
            ]
        )
        let tapped = expectation(description: "Ending the last local touch emits tap")
        touchPad.onTap = { tapped.fulfill() }

        touchPad.touchesBegan([localTouch], with: event)
        localTouch.fakePhase = .ended
        touchPad.touchesEnded([localTouch], with: event)

        wait(for: [tapped], timeout: 0.5)
    }

    func testStationarySecondLocalTouchPreventsPrematureCompletion() {
        let touchPad = TouchPadUIView(frame: CGRect(x: 0, y: 0, width: 200, height: 200))
        let firstTouch = FakeTouch(phase: .began, location: CGPoint(x: 20, y: 20))
        let secondTouch = FakeTouch(phase: .began, location: CGPoint(x: 40, y: 20))
        let event = FakeEvent(
            allTouches: [firstTouch, secondTouch],
            touchesByView: [ObjectIdentifier(touchPad): [firstTouch, secondTouch]]
        )
        let prematureTap = expectation(description: "Stationary local touch blocks completion")
        prematureTap.isInverted = true
        touchPad.onTwoFingerTap = { prematureTap.fulfill() }

        touchPad.touchesBegan([firstTouch, secondTouch], with: event)
        firstTouch.fakePhase = .ended
        secondTouch.fakePhase = .stationary
        touchPad.touchesEnded([firstTouch], with: event)

        wait(for: [prematureTap], timeout: 0.1)

        let completedTap = expectation(description: "Last local touch completes gesture")
        touchPad.onTwoFingerTap = { completedTap.fulfill() }
        secondTouch.fakePhase = .ended
        touchPad.touchesEnded([secondTouch], with: event)

        wait(for: [completedTap], timeout: 0.1)
    }
}

private final class FakeTouch: UITouch {
    var fakePhase: UITouch.Phase
    var fakeLocation: CGPoint

    init(phase: UITouch.Phase, location: CGPoint) {
        fakePhase = phase
        fakeLocation = location
        super.init()
    }

    override var phase: UITouch.Phase {
        fakePhase
    }

    override func location(in view: UIView?) -> CGPoint {
        fakeLocation
    }
}

private final class FakeEvent: UIEvent {
    private let fakeAllTouches: Set<UITouch>
    private let touchesByView: [ObjectIdentifier: Set<UITouch>]

    init(allTouches: Set<UITouch>, touchesByView: [ObjectIdentifier: Set<UITouch>]) {
        fakeAllTouches = allTouches
        self.touchesByView = touchesByView
        super.init()
    }

    override var allTouches: Set<UITouch>? {
        fakeAllTouches
    }

    override func touches(for view: UIView) -> Set<UITouch>? {
        touchesByView[ObjectIdentifier(view)]
    }
}
