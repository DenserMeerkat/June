package com.example.june.di

import com.example.june.core.data.AppPreferencesImpl
import com.example.june.core.data.backup.ExportImpl
import com.example.june.core.data.backup.RestoreImpl
import com.example.june.core.data.database.DatabaseFactory
import com.example.june.core.data.database.chat.ChatDatabase
import com.example.june.core.data.database.journal.JournalDatabase
import com.example.june.core.data.datastore.DatastoreFactory
import com.example.june.core.data.repository.ChatRepository
import com.example.june.core.data.repository.JournalRepository
import com.example.june.core.domain.AppPreferences
import com.example.june.core.domain.ChatRepo
import com.example.june.core.domain.JournalRepo
import com.example.june.core.domain.backup.ExportRepo
import com.example.june.core.domain.backup.RestoreRepo
import com.example.june.core.navigation.AppNavigator
import com.example.june.core.navigation.AppNavigatorImpl
import com.example.june.viewmodels.ChatVM
import com.example.june.viewmodels.HomeChatVM
import com.example.june.viewmodels.HomeJournalVM
import com.example.june.viewmodels.JournalVM
import com.example.june.viewmodels.SettingsVM
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module


val juneModules = module {

    singleOf(::DatabaseFactory)
    singleOf(::DatastoreFactory)
    single { get<DatabaseFactory>().createJournalDatabase().build() }
    single { get<DatabaseFactory>().createChatDatabase().build() }
    single { get<JournalDatabase>().journalDao() }
    single { get<ChatDatabase>().chatDao() }
    single { get<ChatDatabase>().messageDao() }

    singleOf(::ExportImpl).bind<ExportRepo>()
    singleOf(::RestoreImpl).bind<RestoreRepo>()

    singleOf(::JournalRepository).bind<JournalRepo>()
    single { ChatRepository(get(), get()) }.bind<ChatRepo>()

    single(named("AppPreferences")) { get<DatastoreFactory>().getPreferencesDataStore() }
    single { AppPreferencesImpl(get(named("AppPreferences"))) }.bind<AppPreferences>()

    viewModelOf(::SettingsVM)
    viewModelOf(::JournalVM)
    viewModelOf(::HomeJournalVM)
    viewModelOf(::ChatVM)
    viewModelOf(::HomeChatVM)

    singleOf(::AppNavigatorImpl)
    single<AppNavigator> { get<AppNavigatorImpl>() }
}