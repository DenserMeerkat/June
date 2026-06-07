package com.denser.june.presentation.utils

import android.content.Context

class InternetDisabledException : Exception("Internet access is disabled in the app settings.")

interface UpdateChecker {
    fun checkForUpdates(
        context: Context,
        onUpdateAvailable: (versionName: String, changelog: String, downloadUrl: String) -> Unit,
        onNoUpdate: () -> Unit,
        onError: (Throwable) -> Unit
    )
}
