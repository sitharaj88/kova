package sitharaj.kova.core

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class EventFlowTest {

    @Test
    fun buffersEventsUntilCollected() = runTest {
        val events = MutableEventFlow<String>()
        events.tryEmit("navigate")
        events.tryEmit("toast")

        events.test {
            assertEquals("navigate", awaitItem())
            assertEquals("toast", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deliversLiveEvents() = runTest {
        val events = MutableEventFlow<Int>()
        events.test {
            events.emit(1)
            assertEquals(1, awaitItem())
            events.emit(2)
            assertEquals(2, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
