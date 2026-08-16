package sitharaj.kova.viewmodel

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class StateViewModelTest {

    private data class State(val count: Int = 0, val loading: Boolean = false)

    private sealed interface Action {
        data class Toast(val message: String) : Action
    }

    private class TestViewModel : StateViewModel<State, Action>(State()) {
        var lastError: Throwable? = null

        fun increment() = setState { copy(count = count + 1) }

        fun load() = intent {
            setState { copy(loading = true) }
            setState { copy(loading = false, count = 100) }
            sendAction(Action.Toast("loaded"))
        }

        fun fail() = intent { throw IllegalStateException("boom") }

        override fun onError(error: Throwable) {
            lastError = error
        }
    }

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun setStateReducesState() = runTest(dispatcher) {
        val vm = TestViewModel()
        assertEquals(0, vm.currentState.count)
        vm.increment()
        vm.increment()
        assertEquals(2, vm.currentState.count)
    }

    @Test
    fun intentUpdatesStateAndSendsActions() = runTest(dispatcher) {
        val vm = TestViewModel()
        vm.load()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(State(count = 100, loading = false), vm.currentState)
        vm.actions.test {
            val action = awaitItem()
            assertIs<Action.Toast>(action)
            assertEquals("loaded", action.message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun intentRoutesErrorsToOnError() = runTest(dispatcher) {
        val vm = TestViewModel()
        vm.fail()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("boom", vm.lastError?.message)
    }
}
