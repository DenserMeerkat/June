package com.denser.june

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.denser.june.core.domain.repository.JournalRepository
import com.denser.june.core.utils.FileUtils
import com.denser.june.di.flavorModule
import com.denser.june.di.juneModules
import com.denser.june.notification.NotificationsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class JuneApplication : Application(), ImageLoaderFactory {
    private val journalRepo: JournalRepository by inject()
    private val imageLoader: ImageLoader by inject()

    override fun newImageLoader(): ImageLoader = imageLoader

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
