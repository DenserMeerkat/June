package com.denser.june.core.data.remote

import com.denser.june.core.domain.preferences.PrivacyPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class InternetInterceptor(
    private val privacyPreferences: PrivacyPreferences,
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : Interceptor {

    private val isInternetAllowed = AtomicBoolean(true)

    init {
        scope.launch {
            privacyPreferences.getIsInternetAllowedFlow().collect { allowed ->
                isInternetAllowed.set(allowed)
            }
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!isInternetAllowed.get()) {
            throw IOException("Internet access restricted in app settings")
        }
        return chain.proceed(chain.request())
    }
}
