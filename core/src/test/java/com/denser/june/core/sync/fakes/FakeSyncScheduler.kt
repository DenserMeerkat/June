package com.denser.june.core.sync.fakes

import com.denser.june.core.domain.sync.SyncScheduler

class FakeSyncScheduler : SyncScheduler {
    val enqueuedCalls = mutableListOf<Triple<Boolean, Boolean, Boolean>>()

    override fun enqueue(onlyWifi: Boolean, immediate: Boolean, isFullRevalidation: Boolean) {
        enqueuedCalls.add(Triple(onlyWifi, immediate, isFullRevalidation))
    }
}
