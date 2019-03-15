package com.ashwin.android.library.diygeofence

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.*

class DatabaseHandler(context: Context, name: String?, factory: SQLiteDatabase.CursorFactory?, version: Int) :
    SQLiteOpenHelper(context, DATABASE_NAME, factory, DATABASE_VERSION) {

    companion object {
        val DATABASE_VERSION = 1
        private val DATABASE_NAME = "geofence.db"
        val TABLE_GEOFENCE = "geofence"
        val COLUMN_ID = "id"
        val COLUMN_LAT = "lat"
        val COLUMN_LNG = "lng"
        val COLUMN_RAD = "rad"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val geofenceQuery = ("CREATE TABLE " +
                TABLE_GEOFENCE + "("
                + COLUMN_ID + " TEXT PRIMARY KEY,"
                + COLUMN_LAT + " REAL,"
                + COLUMN_LNG + " REAL,"
                + COLUMN_RAD + " REAL"
                + ")")
        db.execSQL(geofenceQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_GEOFENCE")
        onCreate(db)
    }

    fun addGeofence(id: String, lat: Double, lng: Double, rad: Double) : Boolean {
        val values = ContentValues()
        values.put(COLUMN_ID, id)
        values.put(COLUMN_LAT, lat)
        values.put(COLUMN_LNG, lng)
        values.put(COLUMN_RAD, rad)

        val db = this.writableDatabase
        val result = db.insertWithOnConflict(TABLE_GEOFENCE, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
        return result != -1L
    }

    fun removeGeofence(id: String): Boolean {
        var result = false
        val query = "SELECT * FROM $TABLE_GEOFENCE WHERE $COLUMN_ID = '$id'"
        val db = this.writableDatabase
        val cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst()) {
            db.delete(TABLE_GEOFENCE, "$COLUMN_ID = ?", arrayOf(id))
            cursor.close()
            result = true
        }
        db.close()
        return result
    }

    fun removeAllGeofences(): Boolean {
        val db = this.writableDatabase
        db.delete(TABLE_GEOFENCE, null, null)
        return true
    }

    fun getGeofences(): HashSet<GeofenceData> {
        var result = java.util.HashSet<GeofenceData>()
        val query = "SELECT * FROM $TABLE_GEOFENCE"
        val db = this.writableDatabase
        val cursor = db.rawQuery(query, null)
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getString(cursor.getColumnIndex(COLUMN_ID))
                val lat = cursor.getDouble(cursor.getColumnIndex(COLUMN_LAT))
                val lng = cursor.getDouble(cursor.getColumnIndex(COLUMN_LNG))
                val rad = cursor.getDouble(cursor.getColumnIndex(COLUMN_RAD))
                result.add(GeofenceData(id, lat, lng, rad))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return result
    }
}
