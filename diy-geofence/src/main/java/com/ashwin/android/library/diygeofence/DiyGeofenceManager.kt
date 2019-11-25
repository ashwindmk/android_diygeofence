package com.ashwin.android.library.diygeofence

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.support.annotation.Keep
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import java.lang.Exception
import com.google.android.gms.common.GooglePlayServicesUtil
import java.util.*

@Keep
object DiyGeofenceManager {
    const val DEBUG_TAG = "diy-geofence"

    var appDebugBuild: Boolean = false

    // Handler
    var locationHandler: Handler? = null

    private const val REQUEST_CODE = 1024
    private val PREFS_FILENAME = "com.ashwin.geofence.prefs"
    const val PREFS_FILENAME_DEBUG = "com.ashwin.geofence.prefs.debug"

    const val LOCATION_UPDATES = "location_updates"

    private const val ONE_SECOND = 1000
    private const val ONE_MINUTE = 60 * ONE_SECOND
    private const val ONE_HOUR = 60 * ONE_MINUTE
    private const val ONE_KILOMETER = 1000f

    // Street
    private const val INTERVAL_STREET: Long = 10L * ONE_MINUTE
    private const val DISPLACEMENT_STREET: Float = ONE_KILOMETER
    const val ACCURACY_STREET: Int = 1

    // City
    private const val INTERVAL_CITY: Long = 3L * ONE_HOUR
    private const val DISPLACEMENT_CITY: Float = 10 * ONE_KILOMETER
    const val ACCURACY_CITY: Int = 2

    // Country
    private const val INTERVAL_COUNTRY: Long = 12L * ONE_HOUR
    private const val DISPLACEMENT_COUNTRY: Float = 50 * ONE_KILOMETER
    const val ACCURACY_COUNTRY: Int = 3

    const val ACCURACY_NONE: Int = 0

    // Sharedprefs
    private const val ENABLE_LOCATION_UPDATES = "enable_location_updates"
    private const val LAST_ENTERED_GEOFENCES = "last_entered_geofences"
    private const val LOCATION_ACCURACY = "location_accuracy"

    private var listener: GeofenceListener? = null

    private fun isGooglePlayServicesAvailable(context: Context): Boolean {
        val status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context)
        return ConnectionResult.SUCCESS == status
    }

    private val FUSED_LOCATION_AVAILABLE: Boolean = try {
        Class.forName("com.google.android.gms.location.LocationServices")
        true
    } catch (e: Exception) {
        if (appDebugBuild) {
            Log.e(DEBUG_TAG, "Location dependency not found", e)
        }
        false
    }

    private fun getDb(context: Context): DatabaseHandler {
        return DatabaseHandler(context, null, null, DatabaseHandler.DATABASE_VERSION)
    }

    fun init(context: Context, debug: Boolean, geofenceListener: GeofenceListener?, enableLocationUpdates: Boolean = true) {
        appDebugBuild = debug

        listener = geofenceListener

        val sharedPreferences = context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putBoolean(ENABLE_LOCATION_UPDATES, enableLocationUpdates)
            .apply()

        // Fetch location and process geofences
        val handlerThread = HandlerThread("worker-init-location-check")
        handlerThread.start()
        locationHandler = Handler(handlerThread.looper)
        locationHandler?.postDelayed({
            if (appDebugBuild) {
                val geofences = getDb(context).getGeofences()
                Log.w(DEBUG_TAG, "total added geofences: ${geofences.size}")
            }
            updateLocation(context, true)
            handlerThread.quit()
        }, 2500)
    }

    fun getAddedGeofences(context: Context): ArrayList<GeofenceData> {
        return ArrayList<GeofenceData>(getDb(context).getGeofences())
    }

    fun setGeofenceListener(c: GeofenceListener) {
        listener = c
    }

    fun removeGeofenceListener() {
        listener = null
    }

    fun addGeofence(context: Context, id: String, lat: Double, lng: Double, rad: Double): Boolean {
        val isAdded = getDb(context).addGeofence(id, lat, lng, rad)
        if (isAdded) {
            updateLocation(context)
        }
        return isAdded
    }

    fun removeGeofence(context: Context, id: String): Boolean {
        val isRemoved = getDb(context).removeGeofence(id)
        if (isRemoved) {
            updateLocation(context)
        }
        return isRemoved
    }

    fun removeAllGeofences(context: Context): Boolean {
        val areRemoved = getDb(context).removeAllGeofences()
        if (areRemoved) {
            updateLocation(context)
        }
        return areRemoved
    }

    fun setLocation(context: Context, lat: Double, lng: Double) {
        val location = Location("")
        location.latitude = lat
        location.longitude = lng
        setLocation(context, location)
    }

    fun setLocation(context: Context, location: Location) {
        val list = ArrayList<Location>()
        list.add(location)
        processGeofences(context, list)
    }

    private fun canUpdateLocation(context: Context): Boolean {
        var canUpdateLocation = true

        if (!FUSED_LOCATION_AVAILABLE) {
            if (appDebugBuild) {
                Log.e(DEBUG_TAG, "Location dependency library not added")
            }
            canUpdateLocation = false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (appDebugBuild) {
                Log.e(DEBUG_TAG, "Location permission not granted")
            }
            canUpdateLocation = false
        }

        if (!canUpdateLocation) {
            val sharedPreferences = context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
            stopLocationUpdates(context, sharedPreferences)
        }

        return canUpdateLocation
    }

    @SuppressLint("MissingPermission")
    fun updateLocation(context: Context, forceLocationUpdate: Boolean = false) {
        if (!canUpdateLocation(context)) {
            return
        }

        val geofences = getDb(context).getGeofences()
        if (geofences.isEmpty()) {
            stopLocationUpdates(context, context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE))
            return
        }

        val fusedLocationProvider = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationProvider.lastLocation
            .addOnSuccessListener { location : Location? ->
                if (location != null) {
                    if (appDebugBuild) {
                        Log.w(DEBUG_TAG, "Updated location: ${location.latitude}, ${location.longitude}")
                    }

                    val list = ArrayList<Location>()
                    list.add(location)
                    processGeofences(context, list, forceLocationUpdate)
                } else {
                    if (appDebugBuild) {
                        Log.e(DEBUG_TAG, "Location is null")
                    }
                }
            }
    }

    private fun getPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, LocationUpdateReceiver::class.java)
        intent.action = LocationUpdateReceiver.ACTION_PROCESS_UPDATES
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun updateLocationUpdates(context: Context, min: Double, force: Boolean = false) {
        testLocationUpdate(context, "Location processed")

        val sharedPreferences = context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
        val enableLocationUpdates = sharedPreferences.getBoolean(ENABLE_LOCATION_UPDATES, true)
        val lastAccuracy = sharedPreferences.getInt(LOCATION_ACCURACY, ACCURACY_NONE)

        if (!enableLocationUpdates) {
            stopLocationUpdates(context, sharedPreferences)
            return
        }

        val newAccuracy: Int = when {
            min == Double.MAX_VALUE -> ACCURACY_NONE
            min >= DISPLACEMENT_COUNTRY -> ACCURACY_COUNTRY
            min >= DISPLACEMENT_CITY -> ACCURACY_CITY
            else -> ACCURACY_STREET
        }
        testLogAccuracy(newAccuracy)

        if (lastAccuracy == newAccuracy && !force) {
            return
        }

        startLocationUpdates(context, newAccuracy)
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates(context: Context, accuracy: Int): Boolean {
        if (!canUpdateLocation(context)) {
            return false
        }

        val sharedPreferences = context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)

        if (accuracy == ACCURACY_NONE) {
            stopLocationUpdates(context, sharedPreferences)
            return false
        }

        val interval = when (accuracy) {
            ACCURACY_STREET -> INTERVAL_STREET
            ACCURACY_CITY -> INTERVAL_CITY
            ACCURACY_COUNTRY -> INTERVAL_COUNTRY
            else -> INTERVAL_CITY
        }

        val locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = interval
        locationRequest.fastestInterval = interval / 3
        locationRequest.maxWaitTime = interval * 3

        try {
            val fusedLocationProvider = LocationServices.getFusedLocationProviderClient(context)
            if (appDebugBuild) {
                val request = fusedLocationProvider.requestLocationUpdates(locationRequest, getPendingIntent(context))
                request.addOnCompleteListener {
                    if (it.isSuccessful) {
                        Log.w(DEBUG_TAG, "Successfully registered for location updates")
                    } else {
                        Log.w(DEBUG_TAG, "Failed to register for location updates")
                    }
                }
            } else {
                fusedLocationProvider.requestLocationUpdates(locationRequest, getPendingIntent(context))
            }

            sharedPreferences.edit()
                .putInt(LOCATION_ACCURACY, accuracy)
                .apply()

            return true
        } catch (e: Exception) {
            if (appDebugBuild) {
                Log.e(DEBUG_TAG, "Failed to request location updates", e)
            }
        }

        return false
    }

    internal fun processGeofences(context: Context, locations: List<Location>, forceLocationUpdate: Boolean = false) {
        val sharedPreferences = context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)

        val geofences = getDb(context).getGeofences()
        if (geofences.isEmpty()) {
            stopLocationUpdates(context, sharedPreferences)
            return
        }

        val lastEnteredGeofences: HashSet<String> = sharedPreferences.getStringSet(LAST_ENTERED_GEOFENCES, HashSet()) as HashSet<String>
        val enteredGeofences: HashSet<String> = HashSet()
        val exitedGeofences: HashSet<String> = HashSet()

        val geofenceIds = ArrayList<String>()

        var min: Double = Double.MAX_VALUE
        var closestId = "null"

        for (geofence in geofences) {
            geofenceIds.add(geofence.id)
            for (location in locations) {
                val distance = getDistance(location.latitude, location.longitude, geofence.lat, geofence.lng)

                if (distance <= geofence.rad) {
                    enteredGeofences.add(geofence.id)
                }

                if (distance <= min) {
                    min = distance
                    closestId = geofence.id
                }
            }
        }

        if (BuildConfig.DEBUG) {
            Log.w(DEBUG_TAG, "closest geofence: $closestId, min distance: $min")
        }

        // Exited geofences
        exitedGeofences.addAll(geofenceIds)
        exitedGeofences.removeAll(enteredGeofences)
        for (exitedGeofence in exitedGeofences) {
            if (BuildConfig.DEBUG) {
                Log.w(DEBUG_TAG, "Not in geofence: $exitedGeofence")
            }

            if (lastEnteredGeofences.contains(exitedGeofence)) {
                dispatchExitCallback(context, exitedGeofence)
            }
        }

        // Entered geofences
        if (enteredGeofences.isNotEmpty()) {
            for (enteredGeofence in enteredGeofences) {
                if (BuildConfig.DEBUG) {
                    Log.w(DEBUG_TAG, "In geofence: $enteredGeofence")
                }

                if (!lastEnteredGeofences.contains(enteredGeofence)) {
                    dispatchEnterCallback(context, enteredGeofence)
                }
            }
        }

        // Update last entered geofences
        sharedPreferences.edit()
            .putStringSet(LAST_ENTERED_GEOFENCES, enteredGeofences)
            //.putStringSet("exited_geofences", exitedGeofences)
            .apply()

        // Update location service
        updateLocationUpdates(context, min, forceLocationUpdate)
    }

    private fun getDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val latDistance = Math.toRadians(lat2 - lat1)
        val lonDistance = Math.toRadians(lng2 - lng1)
        val a = Math.sin(latDistance / 2.0) * Math.sin(latDistance / 2.0)
        + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(lonDistance / 2.0) * Math.sin(lonDistance / 2.0)
        val distance = 6371.0 * (2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a))) * 1000.0
        return Math.abs(distance)
    }

    private fun dispatchEnterCallback(context: Context, id: String) {
        if (appDebugBuild) {
            val threadName = Thread.currentThread().name
            Log.w(DEBUG_TAG, "Entered geofence: $id ($threadName)")
        }
        listener?.onEnter(context, id)
    }

    private fun dispatchExitCallback(context: Context, id: String) {
        if (appDebugBuild) {
            val threadName = Thread.currentThread().name
            Log.w(DEBUG_TAG, "Exited geofence: $id ($threadName)")
        }

        listener?.onExit(context, id)
    }

    fun reset(context: Context) {
        context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE).edit()
            .remove(LAST_ENTERED_GEOFENCES)
            //.remove("exited_geofences")
            .apply()
    }

    private fun stopLocationUpdates(context: Context, sharedPreferences: SharedPreferences): Boolean {
        try {
            val accuracy = sharedPreferences.getInt(LOCATION_ACCURACY, ACCURACY_NONE)
            if (accuracy == ACCURACY_NONE) {
                return true
            }

            val fusedLocationProvider = LocationServices.getFusedLocationProviderClient(context)
            if (appDebugBuild) {
                val request = fusedLocationProvider.removeLocationUpdates(getPendingIntent(context))
                request.addOnCompleteListener {
                    if (it.isSuccessful) {
                        if (appDebugBuild) {
                            Log.w(DEBUG_TAG, "Successfully removed location updates")
                        }
                    } else {
                        if (appDebugBuild) {
                            Log.w(DEBUG_TAG, "Failed to remove location updates")
                        }
                    }
                }
            } else {
                fusedLocationProvider.removeLocationUpdates(getPendingIntent(context))
            }

            sharedPreferences.edit()
                .putInt(LOCATION_ACCURACY, ACCURACY_NONE)
                .apply()

            return true
        } catch (e: Exception) {
            if (appDebugBuild) {
                Log.e(DEBUG_TAG, "Exception while stopping location updates", e)
            }
        }
        return false
    }

    // Testing
    internal fun testLocationUpdate(context: Context, msg: String) {
        if (BuildConfig.DEBUG) {
            val sharedPreferences = context.getSharedPreferences(DiyGeofenceManager.PREFS_FILENAME_DEBUG, Context.MODE_PRIVATE)
            val prevLocationUpdates = sharedPreferences.getString(DiyGeofenceManager.LOCATION_UPDATES, "")
            val latestLocationUpdates = if (prevLocationUpdates.isNotBlank()) {
                "${Date()}: $msg\n\n$prevLocationUpdates"
            } else {
                "${Date()}: $msg"
            }

            context.getSharedPreferences(DiyGeofenceManager.PREFS_FILENAME_DEBUG, Context.MODE_PRIVATE).edit()
                .putString(DiyGeofenceManager.LOCATION_UPDATES, latestLocationUpdates)
                .apply()
        }
    }

    private fun testLogAccuracy(accuracy: Int) {
        if (BuildConfig.DEBUG) {
            val str = when (accuracy) {
                ACCURACY_STREET -> "street"
                ACCURACY_CITY -> "city"
                ACCURACY_COUNTRY -> "country"
                else -> "none"
            }
            Log.w(DEBUG_TAG, "location accuracy: $str")
        }
    }
}
