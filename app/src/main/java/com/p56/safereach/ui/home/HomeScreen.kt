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
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
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
import com.p56.safereach.model.AlertType
import com.p56.safereach.model.Severity
import com.p56.safereach.ui.alert.AlertCard
import com.p56.safereach.ui.theme.SafeReachTheme
import com.p56.safereach.ui.theme.StatusError
import com.p56.safereach.ui.theme.StatusErrorDark
import com.p56.safereach.ui.theme.StatusInfo
import com.p56.safereach.ui.theme.StatusInfoDark
import com.p56.safereach.ui.theme.StatusNeutral
import com.p56.safereach.ui.theme.StatusNeutralDark
import com.p56.safereach.ui.theme.StatusWarning
import com.p56.safereach.ui.theme.StatusWarningDark
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
                    TurfMeasurement.distance(
                        Point.fromLngLat(
                            alert.longitude,
                            alert.latitude
                        ),
                        point,
                        TurfConstants.UNIT_METERS
                    ) <= alert.radius
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
                val point = Point.fromLngLat(
                    alert.longitude,
                    alert.latitude
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
                    alert.radius.toDouble(),
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

            val calloutAlert = selectedAlert
            val colorScheme = MaterialTheme.colorScheme
            val localDensity = LocalDensity.current
            MapEffect(calloutAlert?.id, colorScheme) { mapView ->
                val alert = calloutAlert ?: return@MapEffect
                val point = Point.fromLngLat(
                    alert.longitude,
                    alert.latitude
                )
                val calloutView = createAlertCalloutView(context, alert, colorScheme)
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


private fun createAlertCalloutView(
    context: Context,
    alert: Alert,
    colorScheme: ColorScheme,
): View {
    val density = context.resources.displayMetrics.density
    fun dp(value: Int) = (value * density).roundToInt()
    val isDark = colorScheme.surface.luminance() < 0.5f
    val severityColor = severityArgb(alert.severity, isDark)

    val container = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = FrameLayout.LayoutParams(
            dp(260),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setPadding(dp(14), dp(12), dp(14), dp(12))
        background = GradientDrawable().apply {
            cornerRadius = dp(14).toFloat()
            setColor(colorScheme.surfaceContainerHigh.toArgb())
            setStroke(dp(2), severityColor)
        }
        elevation = dp(6).toFloat()
    }

    container.addView(TextView(context).apply {
        text = "${alert.severity.displayName.uppercase()} SEVERITY"
        setTextColor(severityColor)
        textSize = 11f
        setTypeface(typeface, Typeface.BOLD)
    })

    container.addView(TextView(context).apply {
        text = alert.title.ifBlank { "${alert.type.displayName} alert" }
        setTextColor(colorScheme.onSurface.toArgb())
        textSize = 16f
        setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(6), 0, 0)
    })

    if (alert.description.isNotBlank()) {
        container.addView(TextView(context).apply {
            text = alert.description
            setTextColor(colorScheme.onSurfaceVariant.toArgb())
            textSize = 13f
            maxLines = 4
            ellipsize = TextUtils.TruncateAt.END
            setPadding(0, dp(4), 0, 0)
        })
    }

    container.addView(TextView(context).apply {
        text = "Type: ${alert.type.displayName}"
        setTextColor(colorScheme.onSurfaceVariant.toArgb())
        textSize = 12f
        setPadding(0, dp(8), 0, 0)
    })

    return container
}

private fun severityArgb(severity: Severity, isDark: Boolean): Int = when (severity) {
    Severity.LOW -> if (isDark) StatusInfoDark else StatusInfo
    Severity.MEDIUM -> if (isDark) StatusWarningDark else StatusWarning
    Severity.HIGH -> if (isDark) StatusErrorDark else StatusError
    Severity.UNKNOWN -> if (isDark) StatusNeutralDark else StatusNeutral
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
    val now = System.currentTimeMillis()
    SafeReachTheme {
        HomeScreen(
            alerts = listOf(
                Alert(
                    id = 1,
                    timestamp = now,
                    title = "Test Alert",
                    description = "Preview alert near San Francisco.",
                    type = AlertType.FIRE,
                    severity = Severity.HIGH,
                    latitude = 1.toDouble(),
                    longitude = 0.toDouble(),
                    radius = 0,
                    expiresAt = now + 60_000,
                    invalidated = false,
                )
            )
        )
    }
}
