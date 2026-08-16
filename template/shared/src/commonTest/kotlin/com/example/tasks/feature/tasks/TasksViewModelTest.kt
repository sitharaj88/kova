package com.example.tasks.feature.tasks

import app.cash.turbine.test
import com.example.tasks.data.InMemoryTaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class TasksViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = TasksViewModel(InMemoryTaskRepository())

    @Test
    fun loadsInitialTasks() = runTest(dispatcher) {
        val vm = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.currentState.isLoading)
        assertEquals(2, vm.currentState.tasks.size)
        assertEquals(1, vm.currentState.remaining)
    }

    @Test
    fun addAppendsTaskAndNotifies() = runTest(dispatcher) {
        val vm = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.add("Write more tests")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(3, vm.currentState.tasks.size)
        assertEquals("Write more tests", vm.currentState.tasks.last().title)
        vm.actions.test {
            assertIs<TasksAction.ShowMessage>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun rejectsBlankTitles() = runTest(dispatcher) {
        val vm = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.add("   ")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, vm.currentState.tasks.size)
        vm.actions.test {
            val action = assertIs<TasksAction.ShowMessage>(awaitItem())
            assertEquals("Task title can't be empty", action.text)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun toggleFlipsDone() = runTest(dispatcher) {
        val vm = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.toggle(1)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, vm.currentState.remaining)
    }

    @Test
    fun deleteRemovesTask() = runTest(dispatcher) {
        val vm = createViewModel()
        dispatcher.scheduler.advanceUntilIdle()

        vm.delete(1)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(2L), vm.currentState.tasks.map { it.id })
    }
}
