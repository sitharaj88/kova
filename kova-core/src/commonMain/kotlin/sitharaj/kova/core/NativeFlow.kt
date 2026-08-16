package sitharaj.kova.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * A Swift/Objective-C friendly wrapper around a [Flow].
 *
 * Kotlin's [Flow] is a suspending API and cannot be consumed idiomatically from Swift.
 * [NativeFlow] exposes the same stream through closures, delivering every emission on the
 * main thread so values can be assigned straight to UI state.
 *
 * From Swift:
 * ```swift
 * let cancellable = viewModel.itemsNative.subscribe { items in
 *     // main thread, ready for UI
 * }
 * // later
 * cancellable.cancel()
 * ```
 *
 * The subscription is also tied to the [scope] used to create this wrapper (usually
 * `viewModelScope`), so it is cancelled automatically when the owning ViewModel is cleared,
 * even if the Swift side forgets to cancel.
 */
public open class NativeFlow<T : Any>(
    private val source: Flow<T>,
    private val scope: CoroutineScope,
) {

    /**
     * Starts collecting the underlying flow.
     *
     * @param onEach invoked on the main thread for every emission.
     * @return a [Cancellable] that stops the collection.
     */
    public fun subscribe(onEach: (T) -> Unit): Cancellable =
        subscribe(onEach = onEach, onComplete = {}, onError = {})

    /**
     * Starts collecting the underlying flow.
     *
     * @param onEach invoked on the main thread for every emission.
     * @param onComplete invoked on the main thread when the flow completes normally.
     * @param onError invoked on the main thread if the flow fails. Cancellation is not
     * reported as an error.
     * @return a [Cancellable] that stops the collection.
     */
    public fun subscribe(
        onEach: (T) -> Unit,
        onComplete: () -> Unit,
        onError: (Throwable) -> Unit,
    ): Cancellable {
        val job: Job = scope.launch(Dispatchers.Main.immediate) {
            try {
                source.collect { value -> onEach(value) }
                onComplete()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Throwable) {
                onError(e)
            }
        }
        return Cancellable { job.cancel() }
    }
}

/**
 * A Swift/Objective-C friendly wrapper around a [StateFlow].
 *
 * In addition to everything [NativeFlow] provides, the current [value] is available
 * synchronously — exactly what SwiftUI needs to render the first frame without waiting
 * for an emission.
 */
public class NativeStateFlow<T : Any>(
    private val stateFlow: StateFlow<T>,
    scope: CoroutineScope,
) : NativeFlow<T>(stateFlow, scope) {

    /** The current value, available synchronously from any thread. */
    public val value: T get() = stateFlow.value
}

/** Wraps this [Flow] for consumption from Swift. Emissions are delivered on the main thread. */
public fun <T : Any> Flow<T>.asNative(scope: CoroutineScope): NativeFlow<T> =
    NativeFlow(this, scope)

/** Wraps this [StateFlow] for consumption from Swift. Emissions are delivered on the main thread. */
public fun <T : Any> StateFlow<T>.asNative(scope: CoroutineScope): NativeStateFlow<T> =
    NativeStateFlow(this, scope)
