package sitharaj.kova.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * A Swift/Objective-C friendly wrapper around a suspend function.
 *
 * While Kotlin/Native already exports suspend functions as Swift `async` functions,
 * those calls cannot be cancelled from Swift and must be made on the main thread.
 * [NativeSuspend] gives Swift full control:
 *
 * ```swift
 * let task = viewModel.refreshNative().execute(
 *     onSuccess: { result in ... },
 *     onError: { error in ... }
 * )
 * // cancellable from Swift:
 * task.cancel()
 * ```
 *
 * Callbacks are always delivered on the main thread. The task is additionally bound to the
 * [scope] it was created with (usually `viewModelScope`), so it dies with its owner.
 */
public class NativeSuspend<T : Any>(
    private val scope: CoroutineScope,
    private val block: suspend () -> T,
) {

    /**
     * Starts the wrapped suspend function.
     *
     * @param onSuccess invoked on the main thread with the result.
     * @param onError invoked on the main thread if the function throws. Cancellation is not
     * reported as an error.
     * @return a [Cancellable] handle for the running task.
     */
    public fun execute(
        onSuccess: (T) -> Unit,
        onError: (Throwable) -> Unit,
    ): Cancellable {
        val job: Job = scope.launch(Dispatchers.Main.immediate) {
            try {
                onSuccess(block())
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Throwable) {
                onError(e)
            }
        }
        return Cancellable { job.cancel() }
    }
}

/** Wraps a suspend [block] for consumption from Swift. */
public fun <T : Any> nativeSuspend(
    scope: CoroutineScope,
    block: suspend () -> T,
): NativeSuspend<T> = NativeSuspend(scope, block)
