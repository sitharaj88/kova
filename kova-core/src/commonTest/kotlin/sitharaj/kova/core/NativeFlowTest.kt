package sitharaj.kova.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NativeFlowTest {

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
    fun deliversEmissionsAndCompletion() = runTest(dispatcher) {
        val received = mutableListOf<Int>()
        var completed = false
        val scope = TestScope(dispatcher)

        flow {
            emit(1)
            emit(2)
            emit(3)
        }.asNative(scope).subscribe(
            onEach = { received += it },
            onComplete = { completed = true },
            onError = { throw it },
        )
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(1, 2, 3), received)
        assertTrue(completed)
    }

    @Test
    fun reportsErrors() = runTest(dispatcher) {
        var error: Throwable? = null
        val scope = TestScope(dispatcher)

        flow<Int> { throw IllegalStateException("boom") }
            .asNative(scope)
            .subscribe(onEach = {}, onComplete = {}, onError = { error = it })
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("boom", error?.message)
    }

    @Test
    fun cancellableStopsCollection() = runTest(dispatcher) {
        val state = MutableStateFlow(0)
        val received = mutableListOf<Int>()
        val scope = TestScope(dispatcher)

        val cancellable = state.asNative(scope).subscribe { received += it }
        dispatcher.scheduler.advanceUntilIdle()
        state.value = 1
        dispatcher.scheduler.advanceUntilIdle()

        cancellable.cancel()
        state.value = 2
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(0, 1), received)
    }

    @Test
    fun stateFlowExposesCurrentValueSynchronously() {
        val state = MutableStateFlow(42)
        val native = state.asNative(TestScope(dispatcher))
        assertEquals(42, native.value)
        state.value = 7
        assertEquals(7, native.value)
    }
}
