package com.denser.june.billing

import android.app.Activity
import com.denser.june.core.domain.billing.BillingHandler
import com.denser.june.core.domain.logging.AppLogger
import com.denser.june.core.domain.preferences.PrivacyPreferences
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.IOException

private const val TAG = "BillingHandler"
private const val ENTITLEMENT_ID = "june_pro"

class BillingHandlerImpl(
    private val privacyPreferences: PrivacyPreferences
) : BillingHandler {
    private val _isPro = MutableStateFlow(false)
    override val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val _proPrice = MutableStateFlow<String?>(null)
    override val proPrice: StateFlow<String?> = _proPrice.asStateFlow()

    private var cachedPackage: Package? = null

    private val purchases by lazy { Purchases.sharedInstance }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            try {
                if (!privacyPreferences.getIsInternetAllowedFlow().first()) return@launch

                if (Purchases.isConfigured) {
                    purchases.updatedCustomerInfoListener =
                        UpdatedCustomerInfoListener { customerInfo ->
                            updateProStatus(customerInfo)
                        }

                    purchases.getCustomerInfoWith(
                        onSuccess = { customerInfo -> updateProStatus(customerInfo) },
                        onError = { error ->
                            AppLogger.e(AppLogger.Category.BILLING, TAG, "Error fetching customer info: $error")
                        }
                    )

                    loadOfferings()
                }
            } catch (e: Exception) {
                AppLogger.e(AppLogger.Category.BILLING, TAG, "Purchases init error", e)
            }
        }
    }

    private fun loadOfferings() {
        try {
            purchases.getOfferingsWith(
                onSuccess = { offerings ->
                    val currentOffering = offerings.current
                    val pkg = currentOffering?.lifetime
                        ?: currentOffering?.availablePackages?.firstOrNull()
                    cachedPackage = pkg
                    _proPrice.value = pkg?.product?.price?.formatted
                },
                onError = { error ->
                    AppLogger.e(AppLogger.Category.BILLING, TAG, "Error fetching offerings: $error")
                }
            )
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Category.BILLING, TAG, "Error in loadOfferings", e)
        }
    }

    private fun updateProStatus(customerInfo: CustomerInfo) {
        val proEntitlement = customerInfo.entitlements[ENTITLEMENT_ID]
        _isPro.value = proEntitlement?.isActive == true
    }

    override suspend fun purchasePro(activity: Activity): Result<Boolean> {
        return try {
            if (!privacyPreferences.getIsInternetAllowedFlow().first()) {
                return Result.failure(IOException("Internet access restricted in app settings"))
            }

            if (!Purchases.isConfigured) {
                return Result.failure(IllegalStateException("Billing is not configured"))
            }

            val pkg = cachedPackage ?: run {
                val offerings = purchases.awaitOfferings()
                val current = offerings.current
                val resolved = current?.lifetime ?: current?.availablePackages?.firstOrNull()
                cachedPackage = resolved
                _proPrice.value = resolved?.product?.price?.formatted
                resolved
            } ?: run {
                AppLogger.e(AppLogger.Category.BILLING, TAG, "No lifetime package found in offerings")
                return Result.failure(IllegalStateException("No lifetime product found in RevenueCat"))
            }

            val params = PurchaseParams.Builder(activity, pkg).build()
            val purchaseResult = purchases.awaitPurchase(params)
            updateProStatus(purchaseResult.customerInfo)
            val isActive = purchaseResult.customerInfo.entitlements[ENTITLEMENT_ID]?.isActive == true
            Result.success(isActive)
        } catch (e: PurchasesTransactionException) {
            if (e.userCancelled) {
                Result.failure(Exception("Purchase cancelled"))
            } else {
                AppLogger.e(AppLogger.Category.BILLING, TAG, "Purchase transaction exception", e)
                Result.failure(e)
            }
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Category.BILLING, TAG, "Unexpected error in purchasePro", e)
            Result.failure(e)
        }
    }

    override suspend fun restorePurchases(): Result<Boolean> {
        return try {
            if (!privacyPreferences.getIsInternetAllowedFlow().first()) {
                return Result.failure(IOException("Internet access restricted in app settings"))
            }

            if (!Purchases.isConfigured) {
                return Result.failure(IllegalStateException("Billing is not configured"))
            }
            val customerInfo = purchases.awaitRestore()
            updateProStatus(customerInfo)
            val isActive = customerInfo.entitlements[ENTITLEMENT_ID]?.isActive == true
            Result.success(isActive)
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Category.BILLING, TAG, "Error in restorePurchases", e)
            Result.failure(e)
        }
    }

    override fun resetProForTesting() {
        _isPro.value = false
        try {
            if (Purchases.isConfigured) {
                purchases.logOut()
            }
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Category.BILLING, TAG, "Error logging out for testing", e)
        }
    }
}
