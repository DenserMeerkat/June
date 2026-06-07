package com.denser.june;

import android.app.Application
import com.denser.june.di.juneModules
import com.denser.june.di.flavorModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import com.denser.june.core.utils.FileUtils
import com.denser.june.core.domain.repository.JournalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.denser.june.notification.NotificationsHelper
import org.koin.android.ext.android.inject

class JuneApplication : Application() {
    private val journalRepo: JournalRepository by inject()
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@JuneApplication)
            modules(juneModules, flavorModule)
        }
        
        NotificationsHelper(this).createNotificationChannel()
        cleanupStorage()
    }
    private fun cleanupStorage() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val allJournals = journalRepo.getAllJournalsIncludeDeletedSync()
                val activePaths = allJournals.flatMap { it.images }
                FileUtils.cleanOrphanedFiles(applicationContext, activePaths)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
