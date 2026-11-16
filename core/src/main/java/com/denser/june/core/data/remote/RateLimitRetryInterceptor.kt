package com.denser.june.core.data.remote

import com.denser.june.core.domain.logging.AppLogger
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class RateLimitRetryInterceptor(
    private val maxRetries: Int = 3,
    private val initialDelayMs: Long = 500L
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response = chain.proceed(request)
        var attempt = 0

        while ((response.code == 429 || response.code == 503) && attempt < maxRetries) {
            attempt++
            val retryAfterSeconds = response.header("Retry-After")?.toLongOrNull()
            val delayMs = if (retryAfterSeconds != null && retryAfterSeconds > 0) {
                retryAfterSeconds * 1000L
            } else {
                initialDelayMs * (1L shl (attempt - 1))
            }

            AppLogger.w(
                AppLogger.Category.SYNC,
                "RateLimitRetryInterceptor",
                "HTTP ${response.code} on ${request.url}. Retrying in ${delayMs}ms (attempt $attempt/$maxRetries)..."
            )

            response.close()
            try {
                Thread.sleep(delayMs)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IOException("Interrupted while waiting for rate-limit retry", e)
            }
            response = chain.proceed(request)
        }

        return response
    }
}
