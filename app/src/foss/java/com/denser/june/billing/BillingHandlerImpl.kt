package com.denser.june.billing

import android.app.Activity
import com.denser.june.core.domain.billing.BillingHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BillingHandlerImpl : BillingHandler {
    private val _isPro = MutableStateFlow(true)
    override val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val _proPrice = MutableStateFlow<String?>(null)
    override val proPrice: StateFlow<String?> = _proPrice.asStateFlow()

    override suspend fun purchasePro(activity: Activity): Result<Boolean> {
        return Result.success(true)
    }

    override suspend fun restorePurchases(): Result<Boolean> {
        return Result.success(true)
    }

    override fun resetProForTesting() {
        _isPro.value = false
    }
}
