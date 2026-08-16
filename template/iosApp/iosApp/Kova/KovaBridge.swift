import SwiftUI
import Shared
import KovaSwift

// The only file that knows both worlds: it adapts this app's Kotlin framework types to the
// framework-agnostic KovaSwift package. Copy it into any Kova app and adjust nothing else.

/// The multiplatform `androidx.lifecycle.ViewModel` exports under a mangled Objective-C
/// name — give it back its real one.
public typealias ViewModel = Lifecycle_viewmodelViewModel

extension ViewModelHolder {

    /// Creates and owns a Kova ViewModel with the Android-equivalent lifecycle:
    /// when this holder deinits, `onCleared()` runs and `viewModelScope` is cancelled.
    ///
    /// ```swift
    /// @StateObject private var holder = ViewModelHolder { ViewModels.shared.tasks() }
    /// ```
    @MainActor
    public convenience init(_ factory: () -> VM) where VM: ViewModel {
        let host = ViewModelHost()
        let viewModel = factory()
        host.attach(viewModel: viewModel)
        self.init(viewModel: viewModel, onClear: { host.clear() })
    }
}

extension Observing where T: AnyObject {

    /// Observes a Kotlin `NativeStateFlow`, rendering the first frame synchronously.
    @MainActor
    public init(
        _ flow: NativeStateFlow<T>,
        @ViewBuilder content: @escaping (T) -> Content
    ) {
        self.init(
            initial: flow.value,
            subscribe: { onEach in
                let subscription = flow.subscribe(onEach: onEach)
                return { subscription.cancel() }
            },
            content: content
        )
    }
}

/// Consumes a Kotlin `NativeFlow` as a Swift `AsyncStream`.
///
/// ```swift
/// .task {
///     for await action in stream(viewModel.actionsNative) { handle(action) }
/// }
/// ```
public func stream<T: AnyObject>(_ flow: NativeFlow<T>) -> AsyncStream<T> {
    asyncStream { onEach, onFinish in
        let subscription = flow.subscribe(
            onEach: onEach,
            onComplete: onFinish,
            onError: { _ in onFinish() }
        )
        return { subscription.cancel() }
    }
}
