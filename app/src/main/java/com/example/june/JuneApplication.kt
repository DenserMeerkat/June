package com.example.june;

import android.app.Application
import com.example.june.di.juneModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class JuneApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@JuneApplication)
            modules(juneModules)
        }
    }
}
