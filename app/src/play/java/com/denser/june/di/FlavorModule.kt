package com.denser.june.di

import com.denser.june.presentation.utils.LocationProvider
import com.denser.june.presentation.utils.PlayLocationProvider
import com.denser.june.presentation.utils.PlayUpdateChecker
import com.denser.june.presentation.utils.UpdateChecker
import com.denser.june.core.domain.sync.CloudProvider
import com.denser.june.data.sync.GoogleDriveProvider
import com.denser.june.core.domain.billing.BillingHandler
import com.denser.june.billing.BillingHandlerImpl
import org.koin.core.qualifier.named
import org.koin.dsl.module

val flavorModule = module {
    single<UpdateChecker> { PlayUpdateChecker(get()) }
    single<LocationProvider> { PlayLocationProvider() }
    single<CloudProvider>(named("GoogleDrive")) { GoogleDriveProvider(get(), get(), get()) }
    single<BillingHandler> { BillingHandlerImpl(get()) }
}
