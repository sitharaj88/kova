import SwiftUI

// KovaSwift is deliberately framework-agnostic: everything is expressed through closures and
// plain generics, never through your app's Kotlin framework types. Your app supplies a small
// bridge file with convenience initializers over `NativeStateFlow`/`NativeFlow` — see
// `KovaBridge.swift` in the Kova template.

/// A function that starts delivering values to `onEach` and returns a closure that stops it.
public typealias Subscription<T> = (_ onEach: @escaping (T) -> Void) -> () -> Void

// MARK: - ViewModel lifecycle

/// Owns a shared ViewModel for the lifetime of a SwiftUI view.
///
/// Create it as a `@StateObject` so SwiftUI keeps the ViewModel alive across re-renders,
/// exactly like `viewModel()` does on Android. When the view is torn down, `deinit` runs
/// `onClear` — the bridge wires this to `ViewModelHost.clear()`, so `onCleared()` fires and
/// `viewModelScope` is cancelled.
@MainActor
public final class ViewModelHolder<VM: AnyObject>: ObservableObject {
    public let viewModel: VM
    private let onClear: () -> Void

    public init(viewModel: VM, onClear: @escaping () -> Void) {
        self.viewModel = viewModel
        self.onClear = onClear
    }

    deinit {
        onClear()
    }
}

// MARK: - State observation

/// Bridges a stream with a synchronous initial value (a Kotlin `StateFlow`) into an
/// `ObservableObject` — SwiftUI renders the first frame with real state, no optionals.
@MainActor
public final class FlowState<T>: ObservableObject {
    @Published public private(set) var value: T
    private var cancel: (() -> Void)?

    public init(initial: T, subscribe: Subscription<T>) {
        value = initial
        cancel = subscribe { [weak self] newValue in
            self?.value = newValue
        }
    }

    deinit {
        cancel?()
    }
}

/// Renders content that re-evaluates whenever the underlying stream emits.
///
/// With the app-side bridge in place this reads:
/// ```swift
/// Observing(viewModel.stateNative) { state in
///     Text("\(state.remaining) left")
/// }
/// ```
public struct Observing<T, Content: View>: View {
    @StateObject private var state: FlowState<T>
    private let content: (T) -> Content

    @MainActor
    public init(
        initial: T,
        subscribe: @escaping Subscription<T>,
        @ViewBuilder content: @escaping (T) -> Content
    ) {
        _state = StateObject(wrappedValue: FlowState(initial: initial, subscribe: subscribe))
        self.content = content
    }

    public var body: some View {
        content(state.value)
    }
}

// MARK: - Streams as AsyncSequence

/// Adapts a subscription into an `AsyncStream` — ideal for one-shot action/effect channels
/// inside `.task { }`, which auto-cancels with the view.
public func asyncStream<T>(
    subscribe: (
        _ onEach: @escaping (T) -> Void,
        _ onFinish: @escaping () -> Void
    ) -> () -> Void
) -> AsyncStream<T> {
    AsyncStream { continuation in
        let cancel = subscribe(
            { continuation.yield($0) },
            { continuation.finish() }
        )
        continuation.onTermination = { _ in
            cancel()
        }
    }
}
