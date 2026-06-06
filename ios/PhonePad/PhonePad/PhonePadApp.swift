import SwiftUI

@main
struct PhonePadApp: App {
    @Environment(\.scenePhase) private var scenePhase
    @StateObject private var mouseController = BLEMouseController()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(mouseController)
        }
        .onChange(of: scenePhase) { _, phase in
            if phase != .active {
                mouseController.releaseAll()
            }
        }
    }
}
