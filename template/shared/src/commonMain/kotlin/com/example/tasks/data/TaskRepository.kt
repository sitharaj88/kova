package com.example.tasks.data

import com.example.tasks.model.TaskItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * The data layer lives entirely in shared code. Swap [InMemoryTaskRepository] for a
 * SQLDelight/Room/Ktor-backed implementation without touching either UI.
 */
interface TaskRepository {
    val tasks: Flow<List<TaskItem>>
    suspend fun add(title: String)
    suspend fun toggle(id: Long)
    suspend fun delete(id: Long)
}

class InMemoryTaskRepository : TaskRepository {

    private val store = MutableStateFlow(
        listOf(
            TaskItem(id = 1, title = "Explore the Kova template"),
            TaskItem(id = 2, title = "Ship shared ViewModels", done = true),
        ),
    )
    private var nextId = 3L

    override val tasks: Flow<List<TaskItem>> = store

    override suspend fun add(title: String) {
        simulateIo()
        store.update { it + TaskItem(id = nextId++, title = title) }
    }

    override suspend fun toggle(id: Long) {
        simulateIo()
        store.update { tasks ->
            tasks.map { if (it.id == id) it.copy(done = !it.done) else it }
        }
    }

    override suspend fun delete(id: Long) {
        simulateIo()
        store.update { tasks -> tasks.filterNot { it.id == id } }
    }

    private suspend fun simulateIo() = delay(150)
}
