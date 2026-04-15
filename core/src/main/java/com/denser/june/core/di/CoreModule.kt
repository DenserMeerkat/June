package com.denser.june.core.di

import com.denser.june.core.data.backup.ExportImpl
import com.denser.june.core.data.backup.RestoreImpl
import com.denser.june.core.data.database.DatabaseFactory
import com.denser.june.core.data.database.journal.JournalDatabase
import com.denser.june.core.data.datastore.DatastoreFactory
import com.denser.june.core.data.preferences.JournalPreferencesImpl
import com.denser.june.core.data.preferences.PrivacyPreferencesImpl
import com.denser.june.core.data.preferences.SyncPreferencesImpl
import com.denser.june.core.data.preferences.ThemePreferencesImpl
import com.denser.june.core.data.remote.SonglinkApiService
import com.denser.june.core.data.remote.SpotifyScraper
import com.denser.june.core.data.repository.JournalRepositoryImpl
import com.denser.june.core.data.repository.SongRepositoryImpl
import com.denser.june.core.data.sync.WebDAVProvider
import com.denser.june.core.domain.backup.ExportRepo
import com.denser.june.core.domain.backup.RestoreRepo
import com.denser.june.core.domain.preferences.JournalPreferences
import com.denser.june.core.domain.preferences.PrivacyPreferences
import com.denser.june.core.domain.preferences.SyncPreferences
import com.denser.june.core.domain.preferences.ThemePreferences
import com.denser.june.core.domain.repository.JournalRepository
import com.denser.june.core.domain.repository.SongRepository
import com.denser.june.core.domain.sync.CloudProvider
import com.denser.june.core.domain.sync.SyncManager
import com.denser.june.core.utils.Constants
import java.io.File
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import android.content.Context
import org.koin.dsl.bind
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

val coreModule = module {
    singleOf(::DatabaseFactory)
    singleOf(::DatastoreFactory)
    single { get<DatabaseFactory>().createJournalDatabase().build() }
    single { get<JournalDatabase>().journalDao() }

    singleOf(::ExportImpl).bind<ExportRepo>()
    singleOf(::RestoreImpl).bind<RestoreRepo>()

    singleOf(::JournalRepositoryImpl).bind<JournalRepository>()

    single(named("PreferencesDataStore")) { get<DatastoreFactory>().getPreferencesDataStore() }
    single { ThemePreferencesImpl(get(named("PreferencesDataStore"))) }.bind<ThemePreferences>()
    single { PrivacyPreferencesImpl(get(named("PreferencesDataStore"))) }.bind<PrivacyPreferences>()
    single { JournalPreferencesImpl(get(named("PreferencesDataStore"))) }.bind<JournalPreferences>()

    single { OkHttpClient() }
    single {
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        val contentType = "application/json".toMediaType()

        Retrofit.Builder()
            .baseUrl(Constants.ODESIL_URL)
            .addConverterFactory(json.asConverterFactory(contentType))
            .client(get<OkHttpClient>())
            .build()
    }

    single { get<Retrofit>().create(SonglinkApiService::class.java) }
    singleOf(::SpotifyScraper)
    singleOf(::SongRepositoryImpl).bind<SongRepository>()

    single { SyncPreferencesImpl(get(named("PreferencesDataStore"))) }.bind<SyncPreferences>()
    single<CloudProvider>(named("WebDAV")) { WebDAVProvider(get(), get(), get()) }

    single(named("ApplicationScope")) { CoroutineScope(SupervisorJob() + Dispatchers.Main) }

    single {
        val context = get<Context>()
        
        SyncManager(
            get(),
            get(),
            mapOf("WebDAV" to get<CloudProvider>(named("WebDAV"))),
            File(context.filesDir, "journal_media"),
            context,
            get(named("ApplicationScope"))
        )
    }
}
