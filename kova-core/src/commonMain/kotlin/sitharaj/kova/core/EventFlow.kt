package sitharaj.kova.core

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * A [Flow] of one-shot events (navigation, snackbars, dialogs).
 *
 * Unlike [kotlinx.coroutines.flow.SharedFlow], events emitted while there is no collector are
 * buffered and delivered as soon as a collector appears — nothing is dropped during
 * configuration changes on Android or view re-creation on iOS. Each event is delivered to
 * exactly one collector.
 */
public interface EventFlow<out T : Any> : Flow<T>

/**
 * The mutable side of [EventFlow]. Keep this private inside your ViewModel and expose it
 * as [EventFlow].
 */
public class MutableEventFlow<T : Any> : EventFlow<T> {
    private val channel = Channel<T>(Channel.UNLIMITED)
    private val flow = channel.receiveAsFlow()

    /** Emits an event. Never suspends and never drops (unbounded buffer). */
    public fun tryEmit(value: T) {
        channel.trySend(value)
    }

    /** Emits an event from a coroutine. */
    public suspend fun emit(value: T) {
        channel.send(value)
    }

    override suspend fun collect(collector: FlowCollector<T>) {
        flow.collect(collector)
    }
}

/** Read-only view of this [MutableEventFlow]. */
public fun <T : Any> MutableEventFlow<T>.asEventFlow(): EventFlow<T> = this
