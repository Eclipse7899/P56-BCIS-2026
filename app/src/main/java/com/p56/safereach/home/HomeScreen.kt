package com.p56.safereach.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
import com.p56.safereach.AlertStateRepository
import com.p56.safereach.Severity
import com.p56.safereach.Shape
import com.p56.safereach.ui.theme.SafeReachTheme
import com.p56.safereach.ui.theme.alertShapeBorderColorSeverityHigh
import com.p56.safereach.ui.theme.alertShapeBorderColorSeverityLow
import com.p56.safereach.ui.theme.alertShapeBorderColorSeverityMedium
import com.p56.safereach.ui.theme.alertShapeFillColorSeverityHigh
import com.p56.safereach.ui.theme.alertShapeFillColorSeverityLow
import com.p56.safereach.ui.theme.alertShapeFillColorSeverityMedium

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val mapViewportState = rememberMapViewportState()
    val msg by AlertStateRepository.alert.collectAsStateWithLifecycle()
    Column(modifier = modifier) {
        MapboxMap(
            Modifier
                .fillMaxSize()
                .padding(4.dp)
                .clip(shape = RoundedCornerShape(24.dp)),
            mapViewportState = mapViewportState
        ){
            MapEffect(Unit) { mapView ->
                mapView.location.updateSettings {
                    locationPuck = createDefault2DPuck(withBearing = true)
                    enabled = true
                    puckBearing = PuckBearing.COURSE
                    puckBearingEnabled = true
                }
                mapViewportState.transitionToFollowPuckState()
            }
            when (val msg = msg) {
                null -> {}
                else -> {
                    when(msg.packet.data.shape){
                        is Shape.Circle -> {
                            val point = Point.fromLngLat(
                                msg.packet.data.shape.center.longitude,
                                msg.packet.data.shape.center.latitude
                            )
                            val color = when (msg.packet.data.severity) {
                                Severity.LOW -> alertShapeFillColorSeverityLow
                                Severity.MEDIUM -> alertShapeFillColorSeverityMedium
                                Severity.HIGH -> alertShapeFillColorSeverityHigh
                            }
                            val borderColor = when (msg.packet.data.severity) {
                                Severity.LOW -> alertShapeBorderColorSeverityLow
                                Severity.MEDIUM -> alertShapeBorderColorSeverityMedium
                                Severity.HIGH -> alertShapeBorderColorSeverityHigh
                            }
                            val polygonCircle = TurfTransformation.circle(
                                point,
                                msg.packet.data.shape.radius,
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
                        is Shape.Polygon -> TODO()
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    SafeReachTheme {
        HomeScreen()
    }
}
