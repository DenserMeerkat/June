package com.denser.june.presentation.utils

import android.Manifest
import android.content.Context
import android.location.LocationManager
import androidx.annotation.RequiresPermission
import com.denser.june.core.domain.model.JournalLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.geometry.LatLng

class FossLocationProvider : LocationProvider {

    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override suspend fun fetchCurrentLocation(context: Context): JournalLocation? {
        return withContext(Dispatchers.IO) {
            try {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

                val providers = listOf(
                    LocationManager.GPS_PROVIDER,
                    LocationManager.NETWORK_PROVIDER
                ).filter { locationManager.isProviderEnabled(it) }

                if (providers.isEmpty()) return@withContext null

                var bestLocation: android.location.Location? = null
                for (provider in providers) {
                    val location = locationManager.getLastKnownLocation(provider) ?: continue
                    if (bestLocation == null || location.accuracy < bestLocation.accuracy) {
                        bestLocation = location
                    }
                }

                bestLocation?.let {
                    MapProviderUtils.updateLocationFromCenter(context, LatLng(it.latitude, it.longitude))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
