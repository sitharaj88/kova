package com.example.tasks.feature.tasks

import com.example.tasks.data.TaskRepository
import sitharaj.kova.annotations.NativeExport
import sitharaj.kova.viewmodel.StateViewModel

/**
 * The complete presentation logic for the Tasks screen — shared verbatim by
 * Jetpack Compose and SwiftUI. The platforms only render [TasksState] and
 * forward user input here.
 */
@NativeExport
class TasksViewModel(
    private val repository: TaskRepository,
) : StateViewModel<TasksState, TasksAction>(TasksState()) {

    init {
        intent {
            repository.tasks.collect { tasks ->
                setState { copy(tasks = tasks, isLoading = false) }
            }
        }
    }

    fun add(title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) {
            sendAction(TasksAction.ShowMessage("Task title can't be empty"))
            return
        }
        intent {
            setState { copy(isSaving = true) }
            repository.add(trimmed)
            setState { copy(isSaving = false) }
            sendAction(TasksAction.ShowMessage("Added \"$trimmed\""))
        }
    }

    fun toggle(id: Long) {
        intent { repository.toggle(id) }
    }

    fun delete(id: Long) {
        intent {
            repository.delete(id)
            sendAction(TasksAction.ShowMessage("Task deleted"))
        }
    }

    override fun onError(error: Throwable) {
        setState { copy(isLoading = false, isSaving = false) }
        sendAction(TasksAction.ShowMessage(error.message ?: "Something went wrong"))
    }
}
