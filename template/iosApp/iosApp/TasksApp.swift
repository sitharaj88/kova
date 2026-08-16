import SwiftUI
import Shared

@main
struct TasksApp: App {
    init() {
        AppModuleKt.doInitKoin(config: nil)
    }

    var body: some Scene {
        WindowGroup {
            TasksScreen()
        }
    }
}
