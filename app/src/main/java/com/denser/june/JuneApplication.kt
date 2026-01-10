package com.denser.june;

import android.app.Application
import com.denser.june.di.juneModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import com.denser.june.core.domain.utils.FileUtils
import com.denser.june.core.domain.JournalRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class JuneApplication : Application() {
    private val journalRepo: JournalRepo by inject()
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@JuneApplication)
            modules(juneModules)
        }
        cleanupStorage()
    }
    private fun cleanupStorage() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val allJournals = journalRepo.getAllJournals()
                val activePaths = allJournals.flatMap { it.images }
                FileUtils.cleanOrphanedFiles(applicationContext, activePaths)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}
