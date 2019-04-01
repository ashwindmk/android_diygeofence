package com.ashwin.android.library.diygeofence

import android.content.Context
import android.support.annotation.Keep

@Keep
interface GeofenceListener {
    fun onEnter(context: Context, id: String)
    fun onExit(context: Context, id: String)
}
