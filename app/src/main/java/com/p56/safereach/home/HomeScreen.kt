package com.p56.safereach.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
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
import com.mapbox.turf.TurfTransformation
import com.p56.safereach.proto.Alert
import com.p56.safereach.proto.Severity
import com.p56.safereach.proto.Shape
import com.p56.safereach.ui.theme.SafeReachTheme
import com.p56.safereach.ui.theme.alertShapeBorderColorSeverityHigh
import com.p56.safereach.ui.theme.alertShapeBorderColorSeverityLow
import com.p56.safereach.ui.theme.alertShapeBorderColorSeverityMedium
import com.p56.safereach.ui.theme.alertShapeFillColorSeverityHigh
import com.p56.safereach.ui.theme.alertShapeFillColorSeverityLow
import com.p56.safereach.ui.theme.alertShapeFillColorSeverityMedium

@Composable
fun HomeRoute(viewModel: HomeViewModel = hiltViewModel()) {
    val alerts by viewModel.activeAlerts.collectAsStateWithLifecycle()
    HomeScreen(alerts = alerts)
}

@Composable
fun HomeScreen(
    alerts: List<Alert>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mapViewportState = rememberMapViewportState {
        MapPreferences.getSavedCameraOptions(context)?.let { cameraOptions ->
            setCameraOptions(cameraOptions)
        }
    }

    LaunchedEffect(mapViewportState.cameraState) {
        val camera = mapViewportState.cameraState ?: return@LaunchedEffect
        val center = camera.center
        MapPreferences.saveCameraPosition(
            context = context,
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
            MapPreferences.saveCameraPosition(
                context = context,
                latitude = center.latitude(),
                longitude = center.longitude(),
                zoom = camera.zoom,
                pitch = camera.pitch,
                bearing = camera.bearing
            )
        }
    }

    Column(modifier = modifier) {
        MapboxMap(
            Modifier
                .fillMaxSize()
                .padding(4.dp)
                .clip(shape = RoundedCornerShape(24.dp)),
            mapViewportState = mapViewportState,
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
                when (alert.shape.kindCase) {
                    Shape.KindCase.CIRCLE -> {
                        val point = Point.fromLngLat(
                            alert.shape.circle.center.longitude,
                            alert.shape.circle.center.latitude
                        )
                        val color = when (alert.severity) {
                            Severity.SEVERITY_LOW -> alertShapeFillColorSeverityLow
                            Severity.SEVERITY_MEDIUM -> alertShapeFillColorSeverityMedium
                            Severity.SEVERITY_HIGH -> alertShapeFillColorSeverityHigh
                            else -> alertShapeFillColorSeverityLow
                        }
                        val borderColor = when (alert.severity) {
                            Severity.SEVERITY_LOW -> alertShapeBorderColorSeverityLow
                            Severity.SEVERITY_MEDIUM -> alertShapeBorderColorSeverityMedium
                            Severity.SEVERITY_HIGH -> alertShapeBorderColorSeverityHigh
                            else -> alertShapeBorderColorSeverityLow
                        }
                        val polygonCircle = TurfTransformation.circle(
                            point,
                            alert.shape.circle.radius,
                            64,
                            TurfConstants.UNIT_METERS
                        )
                        val geoJson = GeoJSONData(polygonCircle)
                        val geoSourceState = GeoJsonSourceState()
                        geoSourceState.data = geoJson
                        FillLayer(
                            sourceState = geoSourceState,
                            layerId = "physical-circle-layer"
                        ) {
                            fillColor = ColorValue(color)
                            fillOutlineColor = ColorValue(borderColor)
                        }
                    }
                    Shape.KindCase.POLYGON -> TODO()
                    Shape.KindCase.KIND_NOT_SET -> TODO()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    SafeReachTheme {
        HomeScreen(alerts = emptyList())
    }
}
