package com.denser.june.billing

import android.app.Activity
import com.denser.june.core.domain.billing.BillingHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BillingHandlerImpl : BillingHandler {
    private val _isPlus = MutableStateFlow(true)
    override val isPlus: StateFlow<Boolean> = _isPlus.asStateFlow()

    private val _plusPrice = MutableStateFlow<String?>(null)
    override val plusPrice: StateFlow<String?> = _plusPrice.asStateFlow()

    override suspend fun purchasePlus(activity: Activity): Result<Boolean> {
        return Result.success(true)
    }

    override suspend fun restorePurchases(): Result<Boolean> {
        return Result.success(true)
    }

    override fun resetPlusForTesting() {
        _isPlus.value = false
    }
}
