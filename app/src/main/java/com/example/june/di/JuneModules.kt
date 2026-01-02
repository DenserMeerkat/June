package com.example.june.di

import com.example.june.core.data.AppPreferencesImpl
import com.example.june.core.data.SongRepoImpl
import com.example.june.core.data.backup.ExportImpl
import com.example.june.core.data.backup.RestoreImpl
import com.example.june.core.data.database.DatabaseFactory
import com.example.june.core.data.database.journal.JournalDatabase
import com.example.june.core.data.datastore.DatastoreFactory
import com.example.june.core.data.remote.SonglinkApiService
import com.example.june.core.data.remote.SpotifyScraper
import com.example.june.core.data.repository.JournalRepository
import com.example.june.core.domain.AppPreferences
import com.example.june.core.domain.JournalRepo
import com.example.june.core.domain.SongRepo
import com.example.june.core.domain.backup.ExportRepo
import com.example.june.core.domain.backup.RestoreRepo
import com.example.june.core.navigation.AppNavigator
import com.example.june.core.navigation.AppNavigatorImpl
import com.example.june.viewmodels.HomeVM
import com.example.june.viewmodels.JournalVM
import com.example.june.viewmodels.SearchVM
import com.example.june.viewmodels.SettingsVM
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
    viewModelOf(::JournalVM)
    viewModelOf(::HomeVM)
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