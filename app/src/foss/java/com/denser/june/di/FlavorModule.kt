package com.denser.june.di

import com.denser.june.presentation.utils.FossLocationProvider
import com.denser.june.presentation.utils.FossUpdateChecker
import com.denser.june.presentation.utils.LocationProvider
import com.denser.june.presentation.utils.UpdateChecker
import org.koin.dsl.module

val flavorModule = module {
    single<UpdateChecker> { FossUpdateChecker(get(), get()) }
    single<LocationProvider> { FossLocationProvider() }
}
