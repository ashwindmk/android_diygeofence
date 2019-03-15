package com.ashwin.android.library.diygeofence

interface GeofenceListener {
    fun onEnter(id: String)
    fun onExit(id: String)
}
