package com.denser.june.presentation.utils

import android.content.Context
import com.denser.june.core.domain.model.JournalLocation

interface LocationProvider {
    suspend fun fetchCurrentLocation(context: Context): JournalLocation?
}
