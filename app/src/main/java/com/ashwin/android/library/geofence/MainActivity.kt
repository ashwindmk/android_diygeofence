package com.ashwin.android.library.geofence

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.view.Menu
import android.widget.Toast
import com.ashwin.android.library.diygeofence.DiyGeofenceManager
import com.ashwin.android.library.geofence.MyListener.Companion.VISITED_GEOFENCES
import kotlinx.android.synthetic.main.activity_main.*
import android.location.Location
import android.view.MenuItem


class MainActivity : AppCompatActivity() {
    companion object {
        const val DEBUG_TAG = "debug-log-app"
        const val LOCATION_PERMISSIONS_REQUEST_CODE = 1025
        const val NEVER_ASK_AGAIN = "never_ask_again"

        const val ADDED_GEOFENCES = "added_geofences"
    }

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.reload -> {
                reload()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = MainApplication.appContext.getSharedPreferences(MainApplication.PREFS_FILENAME, Context.MODE_PRIVATE)

        initViews()
    }

    private fun initViews() {
        add_button.setOnClickListener {
            addGeofence()
        }

        remove_button.setOnClickListener {
            removeGeofence()
        }

        set_button.setOnClickListener {
            setLocation()
        }

        reload()

        clear_button.setOnClickListener {
            clear()
        }

        addall_button.setOnClickListener {
            addAllGeofences()
        }
    }

    private fun reload() {
        added_textview.text = "Loading..."
        visited_textview.text = ""

        var addedGeofencesText = ""
        val addedGeofences = DiyGeofenceManager.getAddedGeofences(applicationContext)
        if (addedGeofences.isNotEmpty()) {
            addedGeofencesText = "ADDED GEOFENCES:\n\n"
            for (addedGeofence in addedGeofences) {
                addedGeofencesText += "${addedGeofence.id}: {lat: ${addedGeofence.lat}, lng: ${addedGeofence.lng}, rad: ${addedGeofence.rad}}\n\n"
            }
        } else {
            addedGeofencesText = "ADDED GEOFENCES:\n\nNo geofences added"
        }
        added_textview.text = addedGeofencesText

        val prevGeofences = sharedPreferences.getString(VISITED_GEOFENCES, "No geofences visited")
        visited_textview.text = "VISITED GEOFENCES:\n\n$prevGeofences"
    }

    private fun addGeofence() {
        val id = id_add_edittext.text.toString().trim()
        val latStr = latitude_edittext.text.toString().trim()
        val lngStr = longitude_edittext.text.toString().trim()
        val radStr = radius_edittext.text.toString().trim()
        if (id.isNotBlank() && latStr.isNotBlank() && lngStr.isNotBlank() && radStr.isNotBlank()) {
            try {
                val lat = latStr.toDouble()
                val lng = lngStr.toDouble()
                val rad = radStr.toDouble()
                if (DiyGeofenceManager.addGeofence(applicationContext, id, lat, lng, rad)) {
                    Toast.makeText(this, "Geofence added successfully", Toast.LENGTH_LONG).show()
                    reload()
                } else {
                    Toast.makeText(this, "Failed to add geofence", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(DEBUG_TAG, "Invalid argument for geofence", e)
                Toast.makeText(this, "Invalid argument for geofence", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Empty argument for geofence", Toast.LENGTH_LONG).show()
        }
    }

    private fun removeGeofence() {
        val id = id_remove_edittext.text.toString().trim()
        if (id.isNotBlank()) {
            if (DiyGeofenceManager.removeGeofence(applicationContext, id)) {
                Toast.makeText(this, "Geofence removed successfully", Toast.LENGTH_LONG).show()
                reload()
            } else {
                Toast.makeText(this, "Failed to remove geofence", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Empty id for geofence", Toast.LENGTH_LONG).show()
        }
    }

    private fun setLocation() {
        val latStr = set_latitude_edittext.text.toString().trim()
        val lngStr = set_longitude_edittext.text.toString().trim()
        if (latStr.isNotBlank() && lngStr.isNotBlank()) {
            try {
                val lat = latStr.toDouble()
                val lng = lngStr.toDouble()
                val location = Location("")
                location.latitude = lat
                location.longitude = lng
                DiyGeofenceManager.setLocation(applicationContext, location)
                Toast.makeText(this, "Location set successfully", Toast.LENGTH_LONG).show()
            } catch (e: java.lang.Exception) {
                Log.e(DEBUG_TAG, "Invalid location", e)
                Toast.makeText(this, "Invalid location", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_LONG).show()
        }
    }

    private fun addAllGeofences() {
        DiyGeofenceManager.addGeofence(applicationContext, "lotus", 19.1450672, 72.85318033, 500.0)
        DiyGeofenceManager.addGeofence(applicationContext, "rammandir", 19.1516243, 72.8501241, 500.0)
        DiyGeofenceManager.addGeofence(applicationContext, "jogeshwari", 19.1361473, 72.8488414, 500.0)
        DiyGeofenceManager.addGeofence(applicationContext, "andheri", 19.1188514, 72.8472434, 500.0)
        DiyGeofenceManager.addGeofence(applicationContext, "vileparle", 19.0995335, 72.8439462, 500.0)
        DiyGeofenceManager.addGeofence(applicationContext, "santacruz", 19.0821852, 72.8416635, 500.0)
        DiyGeofenceManager.addGeofence(applicationContext, "home", 19.0816474, 72.8555775, 500.0)
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && this.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permission_button.isEnabled = true
            permission_button.text = "REQUEST"
            permission_button.setOnClickListener {
                if (sharedPreferences.getBoolean(NEVER_ASK_AGAIN, false)) {
                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + BuildConfig.APPLICATION_ID)))
                } else {
                    requestPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
        } else {
            sharedPreferences.edit().putBoolean(NEVER_ASK_AGAIN, false).apply()
            permission_button.text = "GRANTED"
            permission_button.isEnabled = false
        }
    }

    private fun requestPermission(permission: String) {
        ActivityCompat.requestPermissions(this, arrayOf(permission), LOCATION_PERMISSIONS_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode != LOCATION_PERMISSIONS_REQUEST_CODE) {
            return
        }

        sharedPreferences.edit().putBoolean(NEVER_ASK_AGAIN, false).apply()
        if (grantResults.isEmpty()) {
            if (BuildConfig.DEBUG) {
                Log.w(DEBUG_TAG, "onRequestPermissionsResult() > User interaction was cancelled.")
            }
        } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission granted", Toast.LENGTH_LONG).show()
            DiyGeofenceManager.updateLocation(applicationContext)
        } else {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                sharedPreferences.edit().putBoolean(NEVER_ASK_AGAIN, true).apply()
            }
            Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show()
        }
    }

    private fun clear() {
        sharedPreferences.edit().remove(VISITED_GEOFENCES).apply()
        DiyGeofenceManager.reset(applicationContext)
        reload()
    }
}
