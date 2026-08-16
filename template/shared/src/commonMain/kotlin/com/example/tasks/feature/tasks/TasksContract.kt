package com.example.tasks.feature.tasks

import com.example.tasks.model.TaskItem

/** Immutable UI state — the single source of truth for both Compose and SwiftUI. */
data class TasksState(
    val tasks: List<TaskItem> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
) {
    val remaining: Int get() = tasks.count { !it.done }
}

/** One-shot effects the UI must handle exactly once. */
sealed interface TasksAction {
    data class ShowMessage(val text: String) : TasksAction
}
