package com.p56.safereach.ui.home

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val Context.mapCameraDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "map_camera_preferences"
)

object MapCameraPreferences {
    private val KEY_LAT = doublePreferencesKey("lat")
    private val KEY_LON = doublePreferencesKey("lon")
    private val KEY_ZOOM = doublePreferencesKey("zoom")
    private val KEY_PITCH = doublePreferencesKey("pitch")
    private val KEY_BEARING = doublePreferencesKey("bearing")
    const val DEFAULT_ZOOM = 9.0

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var dataStore: DataStore<Preferences>

    private val _savedCameraOptions = MutableStateFlow<CameraOptions?>(null)

    fun initialize(context: Context) {
        if (::dataStore.isInitialized) return
        dataStore = context.applicationContext.mapCameraDataStore
        val preferences = runCatching {
            runBlocking { dataStore.data.first() }
        }.getOrNull() ?: return
        _savedCameraOptions.value = preferences.toCameraOptions()
    }

    fun getSavedCameraOptions(): CameraOptions? = _savedCameraOptions.value

    fun saveCameraPosition(
        latitude: Double,
        longitude: Double,
        zoom: Double,
        pitch: Double = 0.0,
        bearing: Double = 0.0
    ) {
        _savedCameraOptions.value = cameraOptionsOf(
            latitude = latitude,
            longitude = longitude,
            zoom = zoom,
            pitch = pitch,
            bearing = bearing
        )
        if (::dataStore.isInitialized) {
            scope.launch {
                dataStore.edit { preferences ->
                    preferences[KEY_LAT] = latitude
                    preferences[KEY_LON] = longitude
                    preferences[KEY_ZOOM] = zoom
                    preferences[KEY_PITCH] = pitch
                    preferences[KEY_BEARING] = bearing
                }
            }
        }
    }

    private fun Preferences.toCameraOptions(): CameraOptions? {
        val latitude = this[KEY_LAT] ?: return null
        val longitude = this[KEY_LON] ?: return null
        return cameraOptionsOf(
            latitude = latitude,
            longitude = longitude,
            zoom = this[KEY_ZOOM] ?: DEFAULT_ZOOM,
            pitch = this[KEY_PITCH] ?: 0.0,
            bearing = this[KEY_BEARING] ?: 0.0
        )
    }

    private fun cameraOptionsOf(
        latitude: Double,
        longitude: Double,
        zoom: Double,
        pitch: Double,
        bearing: Double
    ): CameraOptions = CameraOptions.Builder()
        .center(Point.fromLngLat(longitude, latitude))
        .zoom(zoom)
        .pitch(pitch)
        .bearing(bearing)
        .build()
}
