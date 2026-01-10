package com.denser.june.di

import com.denser.june.core.data.AppPreferencesImpl
import com.denser.june.core.data.SongRepoImpl
import com.denser.june.core.data.backup.ExportImpl
import com.denser.june.core.data.backup.RestoreImpl
import com.denser.june.core.data.database.DatabaseFactory
import com.denser.june.core.data.database.journal.JournalDatabase
import com.denser.june.core.data.datastore.DatastoreFactory
import com.denser.june.core.data.remote.SonglinkApiService
import com.denser.june.core.data.remote.SpotifyScraper
import com.denser.june.core.data.repository.JournalRepository
import com.denser.june.core.domain.AppPreferences
import com.denser.june.core.domain.JournalRepo
import com.denser.june.core.domain.SongRepo
import com.denser.june.core.domain.backup.ExportRepo
import com.denser.june.core.domain.backup.RestoreRepo
import com.denser.june.core.navigation.AppNavigator
import com.denser.june.core.navigation.AppNavigatorImpl
import com.denser.june.viewmodels.JournalsVM
import com.denser.june.viewmodels.EditorVM
import com.denser.june.viewmodels.SearchVM
import com.denser.june.viewmodels.SettingsVM
import com.denser.june.viewmodels.TimelineVM
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory


val juneModules = module {

    singleOf(::DatabaseFactory)
    singleOf(::DatastoreFactory)
    single { get<DatabaseFactory>().createJournalDatabase().build() }
    single { get<JournalDatabase>().journalDao() }

    singleOf(::ExportImpl).bind<ExportRepo>()
    singleOf(::RestoreImpl).bind<RestoreRepo>()

    singleOf(::JournalRepository).bind<JournalRepo>()

    single(named("AppPreferences")) { get<DatastoreFactory>().getPreferencesDataStore() }
    single { AppPreferencesImpl(get(named("AppPreferences"))) }.bind<AppPreferences>()

    viewModelOf(::SettingsVM)
    viewModelOf(::EditorVM)
    viewModelOf(::JournalsVM)
    viewModelOf(::TimelineVM)
    viewModelOf(::SearchVM)

    singleOf(::AppNavigatorImpl)
    single<AppNavigator> { get<AppNavigatorImpl>() }

    single { OkHttpClient() }
    single {
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
        val contentType = "application/json".toMediaType()

        Retrofit.Builder()
            .baseUrl("https://api.song.link/")
            .addConverterFactory(json.asConverterFactory(contentType))
            .client(get<OkHttpClient>())
            .build()
    }

    single { get<Retrofit>().create(SonglinkApiService::class.java) }
    singleOf(::SpotifyScraper)
    singleOf(::SongRepoImpl).bind<SongRepo>()
}