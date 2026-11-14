package com.denser.june.core.domain.backup

import com.denser.june.core.domain.model.enums.AutoBackupInterval

interface AutoBackupScheduler {
    fun schedule(interval: AutoBackupInterval, requireCharging: Boolean)
    fun cancel()
}

