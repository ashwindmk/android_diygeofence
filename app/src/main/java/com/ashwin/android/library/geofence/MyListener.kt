package com.ashwin.android.library.geofence

import android.content.Context
import com.ashwin.android.library.diygeofence.GeofenceListener
import java.util.*

class MyListener : GeofenceListener {
    companion object {
        val VISITED_GEOFENCES = "visited_geofences"
    }

    override fun onEnter(context: Context, id: String) {
        val sharedPrefs = MainApplication.appContext.getSharedPreferences(MainApplication.PREFS_FILENAME, Context.MODE_PRIVATE)
        val prevGeofences = sharedPrefs.getString(VISITED_GEOFENCES, "")
        val latestGeofences = if (prevGeofences.isNotBlank()) {
            "${Date()}: Entered: $id\n\n$prevGeofences"
        } else {
            "${Date()}: Entered: $id"
        }
        sharedPrefs.edit()
            .putString(VISITED_GEOFENCES, latestGeofences)
            .apply()
    }

    override fun onExit(context: Context, id: String) {
        val sharedPrefs = MainApplication.appContext.getSharedPreferences(MainApplication.PREFS_FILENAME, Context.MODE_PRIVATE)
        val prevGeofences = sharedPrefs.getString(VISITED_GEOFENCES, "")
        val latestGeofences = if (prevGeofences.isNotBlank()) {
            "${Date()}: Exited: $id\n\n$prevGeofences"
        } else {
            "${Date()}: Exited: $id"
        }
        sharedPrefs.edit()
            .putString(VISITED_GEOFENCES, latestGeofences)
            .apply()
    }
}