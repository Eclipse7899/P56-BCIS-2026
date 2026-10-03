package com.p56.safereach.ui.home

import android.content.Context
import androidx.core.content.edit
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions

object MapCameraPreferences {
    private const val PREFS_NAME = "MapPrefs"
    private const val KEY_LAT = "lat"
    private const val KEY_LON = "lon"
    private const val KEY_ZOOM = "zoom"
    private const val KEY_PITCH = "pitch"
    private const val KEY_BEARING = "bearing"
    const val DEFAULT_ZOOM = 9.0

    fun saveCameraPosition(
        context: Context,
        latitude: Double,
        longitude: Double,
        zoom: Double,
        pitch: Double = 0.0,
        bearing: Double = 0.0
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putFloat(KEY_LAT, latitude.toFloat())
                    .putFloat(KEY_LON, longitude.toFloat())
                    .putFloat(KEY_ZOOM, zoom.toFloat())
                    .putFloat(KEY_PITCH, pitch.toFloat())
                    .putFloat(KEY_BEARING, bearing.toFloat())
            }
    }

    fun hasSavedPosition(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(KEY_LAT) && prefs.contains(KEY_LON)
    }

    fun getSavedCameraOptions(context: Context): CameraOptions? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_LAT) || !prefs.contains(KEY_LON)) {
            return null
        }
        val latitude = prefs.getFloat(KEY_LAT, 0.0f).toDouble()
        val longitude = prefs.getFloat(KEY_LON, 0.0f).toDouble()
        val zoom = prefs.getFloat(KEY_ZOOM, DEFAULT_ZOOM.toFloat()).toDouble()
        val pitch = prefs.getFloat(KEY_PITCH, 0.0f).toDouble()
        val bearing = prefs.getFloat(KEY_BEARING, 0.0f).toDouble()

        return CameraOptions.Builder()
            .center(Point.fromLngLat(longitude, latitude))
            .zoom(zoom)
            .pitch(pitch)
            .bearing(bearing)
            .build()
    }
}
