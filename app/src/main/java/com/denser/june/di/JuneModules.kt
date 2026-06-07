package com.denser.june.di

import android.content.Context
import coil.ImageLoader
import coil.memory.MemoryCache
import okhttp3.OkHttpClient
import com.denser.june.MainVM
import com.denser.june.core.di.coreModule
import com.denser.june.core.domain.model.AppTheme
import com.denser.june.presentation.navigation.AppNavigator
import com.denser.june.presentation.navigation.JuneNavigator
import com.denser.june.presentation.screens.home.journals.JournalsVM
import com.denser.june.presentation.screens.editor.EditorVM
import com.denser.june.presentation.screens.home.tags.TagsVM
import com.denser.june.presentation.screens.search.SearchVM
import com.denser.june.presentation.screens.settings.SettingsVM
import com.denser.june.presentation.screens.home.timeline.TimelineVM
import com.denser.june.presentation.screens.settings.screens.sync.SyncVM
import com.denser.june.presentation.screens.settings.screens.trash.BinVM
import com.denser.june.presentation.screens.settings.screens.reminder.ReminderVM
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module


val juneModules = module {
    includes(coreModule)

    single {
        val context = get<Context>()
        ImageLoader.Builder(context)
            .callFactory(get<OkHttpClient>())
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.15)
                    .build()
            }
            .build()
    }

    viewModel { params ->
        MainVM(
            initialAppTheme = params.getOrNull() ?: AppTheme(),
            get(), get(), get(), get(), get()
        )
    }
    viewModelOf(::SettingsVM)
    viewModelOf(::EditorVM)
    viewModelOf(::JournalsVM)
    viewModelOf(::TagsVM)
    viewModelOf(::TimelineVM)
    viewModelOf(::SearchVM)
    viewModelOf(::BinVM)
    viewModelOf(::SyncVM)
    viewModelOf(::ReminderVM)

    singleOf(::JuneNavigator)
    single<AppNavigator> { get<JuneNavigator>() }
}
