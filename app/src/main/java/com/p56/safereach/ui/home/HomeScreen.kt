package com.p56.safereach.ui.home

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.ColorValue
import com.mapbox.maps.extension.compose.style.layers.generated.FillLayer
import com.mapbox.maps.extension.compose.style.sources.GeoJSONData
import com.mapbox.maps.extension.compose.style.sources.generated.GeoJsonSourceState
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import com.mapbox.turf.TurfTransformation
import com.p56.safereach.model.Alert
import com.p56.safereach.model.Severity
import com.p56.safereach.model.Shape
import com.p56.safereach.ui.alert.AlertCard
import com.p56.safereach.ui.theme.SafeReachTheme
import com.p56.safereach.ui.theme.StatusError
import com.p56.safereach.ui.theme.StatusInfo
import com.p56.safereach.ui.theme.StatusNeutral
import com.p56.safereach.ui.theme.StatusWarning
import com.p56.safereach.ui.theme.alertShapeBorderColorSeverityHigh
import com.p56.safereach.ui.theme.alertShapeBorderColorSeverityLow
import com.p56.safereach.ui.theme.alertShapeBorderColorSeverityMedium
import com.p56.safereach.ui.theme.alertShapeFillColorSeverityHigh
import com.p56.safereach.ui.theme.alertShapeFillColorSeverityLow
import com.p56.safereach.ui.theme.alertShapeFillColorSeverityMedium
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt


@Composable
fun HomeRoute(viewModel: HomeViewModel = hiltViewModel()) {
    val alerts by viewModel.activeAlerts.collectAsStateWithLifecycle()
    HomeScreen(alerts = alerts)
}

@Composable
fun HomeScreen(
    alerts: List<Alert>,
) {
    val mapViewportState = rememberMapViewportState {
        MapCameraPreferences.getSavedCameraOptions()?.let { cameraOptions ->
            setCameraOptions(cameraOptions)
        }
    }

    val context = LocalContext.current

    var selectedAlert by remember { mutableStateOf<Alert?>(null) }

    LaunchedEffect(alerts, selectedAlert) {
        if (alerts.none { it.id == selectedAlert?.id }) {
            selectedAlert = null
        }
    }

    LaunchedEffect(mapViewportState.cameraState) {
        val camera = mapViewportState.cameraState ?: return@LaunchedEffect
        val center = camera.center
        MapCameraPreferences.saveCameraPosition(
            latitude = center.latitude(),
            longitude = center.longitude(),
            zoom = camera.zoom,
            pitch = camera.pitch,
            bearing = camera.bearing
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            val camera = mapViewportState.cameraState ?: return@onDispose
            val center = camera.center
            MapCameraPreferences.saveCameraPosition(
                latitude = center.latitude(),
                longitude = center.longitude(),
                zoom = camera.zoom,
                pitch = camera.pitch,
                bearing = camera.bearing
            )
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        MapboxMap(
            Modifier
                .fillMaxSize()
                .padding(4.dp)
                .clip(shape = RoundedCornerShape(24.dp)),
            mapViewportState = mapViewportState,
            onMapClickListener = { point ->
                val hit = alerts.firstOrNull { alert ->
                    val shape = alert.shape
                    shape is Shape.Circle &&
                        TurfMeasurement.distance(
                            Point.fromLngLat(
                                shape.center.longitude,
                                shape.center.latitude
                            ),
                            point,
                            TurfConstants.UNIT_METERS
                        ) <= shape.radius
                }
                selectedAlert = hit
                hit != null
            },
        ) {
            MapEffect(Unit) { mapView ->
                mapView.location.updateSettings {
                    locationPuck = createDefault2DPuck(withBearing = true)
                    enabled = true
                    puckBearing = PuckBearing.HEADING
                    puckBearingEnabled = true
                }
                mapViewportState.transitionToFollowPuckState()
            }
            alerts.forEach { alert ->
                when (val shape = alert.shape) {
                    is Shape.Circle -> key(alert.id) {
                        val point = Point.fromLngLat(
                            shape.center.longitude,
                            shape.center.latitude
                        )
                        val color = when (alert.severity) {
                            Severity.LOW -> alertShapeFillColorSeverityLow
                            Severity.MEDIUM -> alertShapeFillColorSeverityMedium
                            Severity.HIGH -> alertShapeFillColorSeverityHigh
                            Severity.UNKNOWN -> alertShapeFillColorSeverityLow
                        }
                        val borderColor = when (alert.severity) {
                            Severity.LOW -> alertShapeBorderColorSeverityLow
                            Severity.MEDIUM -> alertShapeBorderColorSeverityMedium
                            Severity.HIGH -> alertShapeBorderColorSeverityHigh
                            Severity.UNKNOWN -> alertShapeBorderColorSeverityLow
                        }
                        val polygonCircle = TurfTransformation.circle(
                            point,
                            shape.radius,
                            64,
                            TurfConstants.UNIT_METERS
                        )
                        val geoSourceState = remember { GeoJsonSourceState() }
                        geoSourceState.data = GeoJSONData(polygonCircle)
                        FillLayer(
                            sourceState = geoSourceState,
                            layerId = "alert-circle-${alert.id}"
                        ) {
                            fillColor = ColorValue(color)
                            fillOutlineColor = ColorValue(borderColor)
                        }
                    }
                    is Shape.Polygon -> {
                    //TODO: Add polygon alert display later.
                    }
                    null -> {
                        // No alert shape available.
                    }
                }
            }

            val calloutAlert = selectedAlert
            val calloutShape = calloutAlert?.shape
            val localDensity = LocalDensity.current
            MapEffect(calloutAlert?.id) { mapView ->
                val alert = calloutAlert ?: return@MapEffect
                val shape = calloutShape as? Shape.Circle ?: return@MapEffect
                val point = Point.fromLngLat(
                    shape.center.longitude,
                    shape.center.latitude
                )
                val calloutView = createAlertCalloutView(context, alert)
                mapView.addView(calloutView)
                try {
                    while (isActive) {
                        val pixel = mapView.mapboxMap.pixelForCoordinate(point)
                        calloutView.x = pixel.x.toFloat() - calloutView.width / 2f
                        calloutView.y = pixel.y.toFloat() -
                            calloutView.height - (8f * localDensity.density)
                        withFrameNanos { }
                    }
                } finally {
                    mapView.removeView(calloutView)
                }
            }
        }
        HomeAlertPanel(
            alerts = alerts,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}


private fun createAlertCalloutView(context: Context, alert: Alert): View {
    val density = context.resources.displayMetrics.density
    fun dp(value: Int) = (value * density).roundToInt()

    val container = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = FrameLayout.LayoutParams(
            dp(260),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setPadding(dp(14), dp(12), dp(14), dp(12))
        background = GradientDrawable().apply {
            cornerRadius = dp(14).toFloat()
            setColor(0xFFFFFFFF.toInt())
            setStroke(dp(2), severityArgb(alert.severity))
        }
        elevation = dp(6).toFloat()
    }

    container.addView(TextView(context).apply {
        text = "${alert.severity.displayName.uppercase()} SEVERITY"
        setTextColor(severityArgb(alert.severity))
        textSize = 11f
        setTypeface(typeface, Typeface.BOLD)
    })

    container.addView(TextView(context).apply {
        text = alert.title.ifBlank { "${alert.type.displayName} alert" }
        setTextColor(0xFF1B1B1B.toInt())
        textSize = 16f
        setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(6), 0, 0)
    })

    if (alert.description.isNotBlank()) {
        container.addView(TextView(context).apply {
            text = alert.description
            setTextColor(0xFF4A4A4A.toInt())
            textSize = 13f
            maxLines = 4
            ellipsize = TextUtils.TruncateAt.END
            setPadding(0, dp(4), 0, 0)
        })
    }

    container.addView(TextView(context).apply {
        text = "Type: ${alert.type.displayName}"
        setTextColor(0xFF6B6B6B.toInt())
        textSize = 12f
        setPadding(0, dp(8), 0, 0)
    })

    return container
}

private fun severityArgb(severity: Severity): Int = when (severity) {
    Severity.LOW -> StatusInfo
    Severity.MEDIUM -> StatusWarning
    Severity.HIGH -> StatusError
    Severity.UNKNOWN -> StatusNeutral
}.toArgb()

@Composable
private fun HomeAlertPanel(
    alerts: List<Alert>,
    modifier: Modifier = Modifier
) {
    val hasAlerts = alerts.isNotEmpty()
    var expanded by rememberSaveable { mutableStateOf(false) }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = hasAlerts) { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ACTIVE ALERTS",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when {
                            !hasAlerts -> "No active alerts nearby"
                            alerts.size == 1 -> "1 alert nearby"
                            else -> "${alerts.size} alerts nearby"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (hasAlerts) {
                    Text(
                        text = if (expanded) "HIDE \u25BC" else "SHOW \u25B2",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = alerts,
                            key = { alert -> alert.id }
                        ) { alert ->
                            AlertCard(alert = alert)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    SafeReachTheme {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp))
        {Text(text = "Home Screen Preview")}
    }
}
