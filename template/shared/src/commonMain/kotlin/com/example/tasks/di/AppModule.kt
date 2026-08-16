package com.example.tasks.di

import com.example.tasks.data.InMemoryTaskRepository
import com.example.tasks.data.TaskRepository
import com.example.tasks.feature.tasks.TasksViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val appModule = module {
    single<TaskRepository> { InMemoryTaskRepository() }
    factory { TasksViewModel(get()) }
}

/** Call once at app startup — from `Application.onCreate()` on Android, `App.init()` on iOS. */
fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(appModule)
    }
}

/**
 * Type-safe ViewModel factories for platforms that can't use Koin's inline APIs (Swift).
 * Android can use these too, or resolve straight from Koin.
 */
object ViewModels : KoinComponent {
    fun tasks(): TasksViewModel = get()
}
