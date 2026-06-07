package com.denser.june.di

import com.denser.june.presentation.utils.LocationProvider
import com.denser.june.presentation.utils.PlayLocationProvider
import com.denser.june.presentation.utils.PlayUpdateChecker
import com.denser.june.presentation.utils.UpdateChecker
import org.koin.dsl.module

val flavorModule = module {
    single<UpdateChecker> { PlayUpdateChecker(get()) }
    single<LocationProvider> { PlayLocationProvider() }
}
