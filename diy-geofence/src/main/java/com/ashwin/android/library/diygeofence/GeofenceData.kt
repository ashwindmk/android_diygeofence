package com.ashwin.android.library.diygeofence

data class GeofenceData(val id: String,
                        val lat: Double,
                        val lng: Double,
                        val rad: Double) {

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other is GeofenceData) {
            return other.id == this.id
        }
        return super.equals(other)
    }
}
