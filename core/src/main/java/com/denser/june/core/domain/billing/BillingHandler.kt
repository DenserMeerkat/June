package com.denser.june.core.domain.billing

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

interface BillingHandler {
    val isPro: StateFlow<Boolean>
    val proPrice: StateFlow<String?>
    suspend fun purchasePro(activity: Activity): Result<Boolean>
    suspend fun restorePurchases(): Result<Boolean>
    fun resetProForTesting()
}
