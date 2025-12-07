package com.example.june.di

import com.example.june.core.data.AppPreferencesImpl
import com.example.june.core.data.backup.ExportImpl
import com.example.june.core.data.backup.RestoreImpl
import com.example.june.core.data.database.DatabaseFactory
import com.example.june.core.data.database.NoteDatabase
import com.example.june.core.data.datastore.DatastoreFactory
import com.example.june.core.data.repository.NoteRepository
import com.example.june.core.domain.AppPreferences
import com.example.june.core.domain.NoteRepo
import com.example.june.core.domain.backup.ExportRepo
import com.example.june.core.domain.backup.RestoreRepo
import com.example.june.core.navigation.AppNavigator
import com.example.june.core.navigation.AppNavigatorImpl
import com.example.june.viewmodels.HomeVM
import com.example.june.viewmodels.NoteVM
import com.example.june.viewmodels.SettingsVM
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module


val juneModules = module {

    singleOf(::DatabaseFactory)
    singleOf(::DatastoreFactory)
    single { get<DatabaseFactory>().create().build() }
    single { get<NoteDatabase>().noteDao() }

    singleOf(::ExportImpl).bind<ExportRepo>()
    singleOf(::RestoreImpl).bind<RestoreRepo>()


    singleOf(::NoteRepository).bind<NoteRepo>()

    single(named("AppPreferences")) { get<DatastoreFactory>().getPreferencesDataStore() }
    single { AppPreferencesImpl(get(named("AppPreferences"))) }.bind<AppPreferences>()

    viewModelOf(::SettingsVM)
    viewModelOf(::NoteVM)
    viewModelOf(::HomeVM)

    singleOf(::AppNavigatorImpl)
    single<AppNavigator> { get<AppNavigatorImpl>() }
}