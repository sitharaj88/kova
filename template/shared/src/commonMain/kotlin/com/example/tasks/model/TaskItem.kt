package com.example.tasks.model

// Named TaskItem (not Task) so it never collides with Swift Concurrency's Task in Swift code.
data class TaskItem(
    val id: Long,
    val title: String,
    val done: Boolean = false,
)
