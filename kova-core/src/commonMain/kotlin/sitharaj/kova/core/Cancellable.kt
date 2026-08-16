package sitharaj.kova.core

/**
 * A handle to a running subscription or task that can be cancelled from any platform.
 *
 * On iOS this maps to a simple object with a `cancel()` method, making it easy to store
 * in Swift and cancel in `deinit` / `onDisappear`.
 */
public fun interface Cancellable {
    public fun cancel()
}
