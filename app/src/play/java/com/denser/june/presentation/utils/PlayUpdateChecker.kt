package com.denser.june.presentation.utils

import android.content.Context
import com.denser.june.BuildConfig
import com.denser.june.core.domain.preferences.PrivacyPreferences
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PlayUpdateChecker(
    private val privacyPreferences: PrivacyPreferences
) : UpdateChecker {

    override fun checkForUpdates(
        context: Context,
        onUpdateAvailable: (versionName: String, changelog: String, downloadUrl: String) -> Unit,
        onNoUpdate: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        if (BuildConfig.DEBUG) {
            onError(Exception("In-app updates are not available in debug builds. The Google Play Store requires a published release version matching this application ID to check for updates."))
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val isInternetAllowed = privacyPreferences.getIsInternetAllowedFlow().first()
                if (!isInternetAllowed) {
                    launch(Dispatchers.Main) {
                        onError(InternetDisabledException())
                    }
                    return@launch
                }

                val appUpdateManager = AppUpdateManagerFactory.create(context)
                val appUpdateInfoTask = appUpdateManager.appUpdateInfo

                appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
                    if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                        onUpdateAvailable(
                            appUpdateInfo.availableVersionCode().toString(),
                            "A new version is available on the Google Play Store.",
                            "market://details?id=${context.packageName}"
                        )
                    } else {
                        onNoUpdate()
                    }
                }.addOnFailureListener { exception ->
                    onError(exception)
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    onError(e)
                }
            }
        }
    }
}

