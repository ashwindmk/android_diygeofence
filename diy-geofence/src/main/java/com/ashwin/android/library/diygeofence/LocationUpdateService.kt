package com.ashwin.android.library.diygeofence

import android.app.IntentService
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.LocationResult

class LocationUpdateService : IntentService("GeofenceLocationService") {
    override fun onHandleIntent(intent: Intent?) {
        val result = LocationResult.extractResult(intent)
        if (result != null) {
            val locations = result.locations
            DiyGeofenceManager.processGeofences(applicationContext, locations, forceLocationUpdate = false)

            if (BuildConfig.DEBUG) {
                for (location in locations) {
                    Log.w(DiyGeofenceManager.DEBUG_TAG, "Received location: ${location.latitude}, ${location.longitude}")
                }
            }

            // Update location update service
            //val min = DiyGeofenceManager.getMinDistance(applicationContext, locations)
            //DiyGeofenceManager.updateLocationUpdates(applicationContext, min)
        } else {
            DiyGeofenceManager.updateLocation(applicationContext)
        }
    }
}
