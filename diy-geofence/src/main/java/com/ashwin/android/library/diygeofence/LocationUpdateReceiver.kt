package com.ashwin.android.library.diygeofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.google.android.gms.location.LocationResult
import kotlin.Exception

class LocationUpdateReceiver : BroadcastReceiver() {
    companion object {
        val ACTION_PROCESS_UPDATES = "com.ashwin.android.library.diygeofence.LOCATION_UPDATES"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) return

        if (DiyGeofenceManager.appDebugBuild) {
            Log.w(DiyGeofenceManager.DEBUG_TAG, "Received location update")
        }

        val action = intent?.action
        if (ACTION_PROCESS_UPDATES == action) {
            DiyGeofenceManager.locationHandler?.removeCallbacksAndMessages(null)

            try {
                DiyGeofenceManager.testLocationUpdate(context, "Location updated")
                intent.setClass(context, LocationUpdateService::class.java)
                context.startService(intent)
            } catch (e: Exception) {
                val handlerThread = HandlerThread("temp-thread")
                handlerThread.start()
                Handler(handlerThread.looper).post(Runnable {
                    try {
                        val result = LocationResult.extractResult(intent)
                        if (result != null) {
                            val locations = result.locations
                            DiyGeofenceManager.processGeofences(context, locations)

                            if (BuildConfig.DEBUG) {
                                for (location in locations) {
                                    Log.w(DiyGeofenceManager.DEBUG_TAG, "Received location: ${location.latitude}, ${location.longitude}")
                                }
                            }

                            // Update location update service
                            //val min = DiyGeofenceManager.getMinDistance(context, locations)
                            //DiyGeofenceManager.updateLocationUpdates(context, min)
                        }
                    } catch (e: java.lang.Exception) {
                        if (DiyGeofenceManager.appDebugBuild) {
                            Log.e(DiyGeofenceManager.DEBUG_TAG, "Exception while starting service from receiver", e)
                        }
                    }
                })
            }
        } else {
            if (DiyGeofenceManager.appDebugBuild) {
                Log.w(DiyGeofenceManager.DEBUG_TAG, "Location update receiver action mismatch")
            }
        }
    }
}
