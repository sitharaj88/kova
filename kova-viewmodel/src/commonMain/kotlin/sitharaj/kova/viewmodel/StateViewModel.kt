package sitharaj.kova.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import sitharaj.kova.core.EventFlow
import sitharaj.kova.core.MutableEventFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * An MVVM/MVI ViewModel with a single immutable [state] and a stream of one-shot [actions].
 *
 * Built on `androidx.lifecycle.ViewModel` (multiplatform), so on Android it *is* a real
 * Jetpack ViewModel — it survives configuration changes, works with Compose's `viewModel()`
 * and Hilt/Koin, and `viewModelScope` is cancelled automatically. On iOS the same class is
 * held by SwiftUI (see `ViewModelHost`) and cleared when the owning view disappears.
 *
 * ```kotlin
 * @NativeExport
 * class CounterViewModel : StateViewModel<CounterState, CounterAction>(CounterState()) {
 *     fun increment() = setState { copy(count = count + 1) }
 *
 *     fun save() = intent {
 *         repository.save(currentState.count)
 *         sendAction(CounterAction.Saved)
 *     }
 * }
 * ```
 *
 * @param S the immutable UI state (a data class).
 * @param A one-shot actions/effects the UI must handle exactly once (navigation, snackbars).
 */
public abstract class StateViewModel<S : Any, A : Any>(initialState: S) : ViewModel() {

    private val _state = MutableStateFlow(initialState)

    /** The stream of UI state. Collect with `collectAsStateWithLifecycle()` on Android. */
    public val state: StateFlow<S> = _state.asStateFlow()

    private val _actions = MutableEventFlow<A>()

    /** One-shot actions. Buffered while the UI is away; each is delivered exactly once. */
    public val actions: EventFlow<A> = _actions

    /** The current state value. */
    public val currentState: S get() = _state.value

    /** Atomically reduces the current state to a new state. */
    protected fun setState(reducer: S.() -> S) {
        _state.update(reducer)
    }

    /** Sends a one-shot action to the UI. Safe to call from any thread. */
    protected fun sendAction(action: A) {
        _actions.tryEmit(action)
    }

    /**
     * Launches work in [viewModelScope]. Uncaught exceptions (other than cancellation) are
     * routed to [onError] instead of crashing the app.
     */
    protected fun intent(
        context: CoroutineContext = EmptyCoroutineContext,
        block: suspend CoroutineScope.() -> Unit,
    ): Job = viewModelScope.launch(context) {
        try {
            block()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            onError(e)
        }
    }

    /** Central hook for errors thrown inside [intent] blocks. Default: no-op. */
    protected open fun onError(error: Throwable) {}
}
