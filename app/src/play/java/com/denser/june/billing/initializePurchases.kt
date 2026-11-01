package com.denser.june.billing

import android.app.Application
import com.denser.june.BuildConfig
import com.denser.june.core.domain.logging.AppLogger
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

private const val TAG = "RevenueCatInit"

fun initializePurchases(application: Application) {
    val apiKey = BuildConfig.REVENUECAT_API_KEY
    if (apiKey.isNotBlank()) {
        if (BuildConfig.DEBUG) {
            Purchases.logLevel = LogLevel.DEBUG
        }

        try {
            Purchases.configure(
                PurchasesConfiguration.Builder(
                    context = application,
                    apiKey = apiKey
                ).build()
            )
            AppLogger.d(AppLogger.Category.BILLING, TAG, "RevenueCat initialized successfully.")
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Category.BILLING, TAG, "Failed to initialize RevenueCat", e)
        }
    } else {
        AppLogger.w(AppLogger.Category.BILLING, TAG, "RevenueCat initialization skipped: API key is placeholder or blank.")
    }
}
