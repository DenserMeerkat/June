package com.denser.june.core.domain.billing

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

interface BillingHandler {
    val isPlus: StateFlow<Boolean>
    val plusPrice: StateFlow<String?>
    suspend fun purchasePlus(activity: Activity): Result<Boolean>
    suspend fun restorePurchases(): Result<Boolean>
    fun resetPlusForTesting()
}
