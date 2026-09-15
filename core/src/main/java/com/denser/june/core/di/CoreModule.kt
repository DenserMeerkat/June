package com.denser.june.core.di

import com.denser.june.core.data.backup.ExportImpl
import com.denser.june.core.data.backup.MarkdownImportImpl
import com.denser.june.core.data.backup.RestoreImpl
import com.denser.june.core.domain.backup.MarkdownImportRepo
import com.denser.june.core.data.database.DatabaseFactory
import com.denser.june.core.data.database.journal.JournalDatabase
import com.denser.june.core.data.datastore.DatastoreFactory
import com.denser.june.core.data.preferences.JournalPreferencesImpl
import com.denser.june.core.data.preferences.PrivacyPreferencesImpl
import com.denser.june.core.data.preferences.SyncPreferencesImpl
import com.denser.june.core.data.preferences.ThemePreferencesImpl
import com.denser.june.core.data.remote.SongLinkScraper
import com.denser.june.core.data.remote.SpotifyScraper
import com.denser.june.core.data.remote.DeezerFetcher
import com.denser.june.core.data.remote.ItunesFetcher
import com.denser.june.core.data.repository.JournalRepositoryImpl
import com.denser.june.core.data.repository.SongRepositoryImpl
import com.denser.june.core.data.sync.WebDAVProvider
import com.denser.june.core.domain.reminder.ReminderScheduler
import com.denser.june.core.data.reminder.ReminderSchedulerImpl
import com.denser.june.core.domain.backup.ExportRepo
import com.denser.june.core.domain.backup.RestoreRepo
import com.denser.june.core.domain.preferences.FontPreferences
import com.denser.june.core.domain.preferences.JournalPreferences
import com.denser.june.core.domain.preferences.PrivacyPreferences
import com.denser.june.core.domain.preferences.SyncPreferences
import com.denser.june.core.domain.preferences.ThemePreferences
import com.denser.june.core.data.preferences.FontPreferencesImpl
import com.denser.june.core.domain.repository.JournalRepository
import com.denser.june.core.domain.repository.SongRepository
import com.denser.june.core.domain.sync.CloudProvider
import com.denser.june.core.domain.sync.SyncManager
import java.io.File
import okhttp3.OkHttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import android.content.Context
import com.denser.june.core.data.remote.InternetInterceptor
import org.koin.dsl.bind
import org.koin.dsl.module

val coreModule = module {
    singleOf(::DatabaseFactory)
    singleOf(::DatastoreFactory)
    single { get<DatabaseFactory>().createJournalDatabase().build() }
    single { get<JournalDatabase>().journalDao() }

    singleOf(::ExportImpl).bind<ExportRepo>()
    singleOf(::RestoreImpl).bind<RestoreRepo>()
    singleOf(::MarkdownImportImpl).bind<MarkdownImportRepo>()

    singleOf(::JournalRepositoryImpl).bind<JournalRepository>()
    singleOf(::ReminderSchedulerImpl).bind<ReminderScheduler>()

    single(named("PreferencesDataStore")) { get<DatastoreFactory>().getPreferencesDataStore() }
    single { ThemePreferencesImpl(get(named("PreferencesDataStore"))) }.bind<ThemePreferences>()
    single { PrivacyPreferencesImpl(get(named("PreferencesDataStore"))) }.bind<PrivacyPreferences>()
    single { FontPreferencesImpl(get(named("PreferencesDataStore"))) }.bind<FontPreferences>()
    single { JournalPreferencesImpl(get(named("PreferencesDataStore"))) }.bind<JournalPreferences>()

    single {
        OkHttpClient.Builder()
            .addInterceptor(InternetInterceptor(get()))
            .build()
    }
    singleOf(::SongLinkScraper)
    singleOf(::SpotifyScraper)
    singleOf(::DeezerFetcher)
    singleOf(::ItunesFetcher)
    singleOf(::SongRepositoryImpl).bind<SongRepository>()

    single { SyncPreferencesImpl(get(named("PreferencesDataStore"))) }.bind<SyncPreferences>()
    single<CloudProvider>(named("WebDAV")) { WebDAVProvider(get(), get(), get()) }

    single(named("ApplicationScope")) { CoroutineScope(SupervisorJob() + Dispatchers.Main) }

    single<com.denser.june.core.domain.sync.SyncScheduler> {
        com.denser.june.core.data.sync.WorkManagerSyncScheduler(get())
    }

    single {
        val context = get<Context>()
        val providers = mutableMapOf<String, CloudProvider>()
        providers["WebDAV"] = get<CloudProvider>(named("WebDAV"))
        
        getOrNull<CloudProvider>(named("GoogleDrive"))?.let {
            providers["GoogleDrive"] = it
        }
        
        SyncManager(
            get(),
            get(),
            providers,
            File(context.filesDir, "journal_media"),
            get(),
            get(named("ApplicationScope"))
        )
    }
}
