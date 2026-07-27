package com.denser.june.core.domain.sync

interface SyncScheduler {
    fun enqueue(onlyWifi: Boolean, immediate: Boolean = false, isFullRevalidation: Boolean = false)
    fun cancel()
}
