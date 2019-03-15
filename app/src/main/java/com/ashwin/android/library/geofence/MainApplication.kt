package com.ashwin.android.library.geofence

import android.app.Application
import com.ashwin.android.library.diygeofence.DiyGeofenceManager

class MainApplication : Application() {
    companion object {
        const val PREFS_FILENAME = "MY_PREFS"
        lateinit var appContext: MainApplication
    }

    override fun onCreate() {
        super.onCreate()
        appContext = this
        DiyGeofenceManager.init(this, BuildConfig.DEBUG, MyListener())
    }
}
