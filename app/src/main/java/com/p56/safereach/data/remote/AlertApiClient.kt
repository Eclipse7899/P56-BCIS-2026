package com.p56.safereach.data.remote

import android.util.Log
import com.p56.safereach.Constants
import com.p56.safereach.model.Alert
import com.p56.safereach.model.AlertType
import com.p56.safereach.model.LatLong
import com.p56.safereach.model.Severity
import com.p56.safereach.model.Shape
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "AlertApiClient"
private const val CONNECT_TIMEOUT_MS = 10_000
private const val READ_TIMEOUT_MS = 10_000

/**
 * Fetches alerts from the `alertsApi` Firebase function.
 *
 * The endpoint returns unsigned alert documents, so the values are trusted only because they
 * travel over HTTPS directly from the SafeReach Cloud Function.
 */
@Singleton
class AlertApiClient @Inject constructor() {

    suspend fun fetchAlerts(
        count: Int = Constants.ALERTS_API_DEFAULT_COUNT,
    ): List<Alert> = withContext(Dispatchers.IO) {
        val url = URL("${Constants.ALERTS_API_BASE_URL}${Constants.ALERTS_API_PATH}?count=$count")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/json")

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.w(TAG, "Alerts API returned HTTP $responseCode")
                return@withContext emptyList()
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            parseAlerts(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseAlerts(body: String): List<Alert> {
        val messages = JSONObject(body).optJSONArray(KEY_MESSAGES) ?: return emptyList()
        return (0 until messages.length()).mapNotNull { index ->
            val json = messages.optJSONObject(index) ?: return@mapNotNull null
            runCatching { parseAlert(json) }
                .onFailure { Log.w(TAG, "Failed to parse alert", it) }
                .getOrNull()
        }
    }

    private fun parseAlert(json: JSONObject): Alert = Alert(
        id = json.getInt(KEY_ID),
        timestamp = json.getLong(KEY_TIMESTAMP),
        title = json.getString(KEY_TITLE),
        description = json.getString(KEY_DESCRIPTION),
        type = AlertType.fromValue(json.getInt(KEY_TYPE)),
        severity = Severity.fromValue(json.getInt(KEY_SEVERITY)),
        latitude = json.getDouble(KEY_LATITUDE),
        longitude = json.getDouble(KEY_LONGITUDE),
        radius = json.getInt(KEY_RADIUS),
        expiresAt = json.getLong(KEY_EXPIRES_AT),
        invalidated = json.optBoolean(KEY_INVALIDATED, false),
    )

    private fun parseShape(json: JSONObject?): Shape? {
        json ?: return null
        json.optJSONObject(KEY_CIRCLE)?.let { circle ->
            val center = circle.getJSONObject(KEY_CENTER)
            return Shape.Circle(
                center = LatLong(center.getDouble(KEY_LATITUDE), center.getDouble(KEY_LONGITUDE)),
                radius = circle.getDouble(KEY_RADIUS),
            )
        }
        json.optJSONObject(KEY_POLYGON)?.let { polygon ->
            val points = polygon.getJSONArray(KEY_POINTS)
            return Shape.Polygon(
                points = (0 until points.length()).map { index ->
                    val point = points.getJSONObject(index)
                    LatLong(point.getDouble(KEY_LATITUDE), point.getDouble(KEY_LONGITUDE))
                }
            )
        }
        return null
    }

    private companion object {
        private const val KEY_MESSAGES = "messages"
        private const val KEY_ID = "id"
        private const val KEY_TIMESTAMP = "timestamp"
        private const val KEY_TITLE = "title"
        private const val KEY_DESCRIPTION = "description"
        private const val KEY_TYPE = "type"
        private const val KEY_SEVERITY = "severity"
        private const val KEY_CIRCLE = "circle"
        private const val KEY_CENTER = "center"
        private const val KEY_LATITUDE = "latitude"
        private const val KEY_LONGITUDE = "longitude"
        private const val KEY_RADIUS = "radius"
        private const val KEY_POLYGON = "polygon"
        private const val KEY_POINTS = "points"
        private const val KEY_EXPIRES_AT = "expiresAt"
        private const val KEY_INVALIDATED = "invalidated"
    }
}
