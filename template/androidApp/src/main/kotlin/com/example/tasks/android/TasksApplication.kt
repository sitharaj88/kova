package com.example.tasks.android

import android.app.Application
import com.example.tasks.di.initKoin
import org.koin.android.ext.koin.androidContext

class TasksApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@TasksApplication)
        }
    }
}
