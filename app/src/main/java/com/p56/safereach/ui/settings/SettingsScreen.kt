package com.p56.safereach.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CoordinateBounds
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.toCameraOptions
import com.p56.safereach.ui.home.MapCameraPreferences
import com.p56.safereach.ui.map.OfflineAreaSummary
import com.p56.safereach.ui.map.OfflineRegionPanel
import com.p56.safereach.ui.map.OfflineRegionUiState
import com.p56.safereach.ui.map.OfflineRegionViewModel

private const val MIN_SELECTION_ZOOM = 11.0

@Composable
fun SettingsRoute(viewModel: OfflineRegionViewModel = hiltViewModel()) {
    val offlineRegionState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        offlineRegionState = offlineRegionState,
        onViewportChanged = viewModel::onViewportChanged,
        onDownload = viewModel::downloadSelection,
        onClearDownloads = viewModel::clearDownloads,
        onDismissMessage = viewModel::dismissMessage,
        onBeginAreaSelection = viewModel::beginSelection,
        onCancelAreaSelection = viewModel::cancelSelection,
    )
}

@Composable
fun SettingsScreen(
    offlineRegionState: OfflineRegionUiState,
    onViewportChanged: (CoordinateBounds) -> Unit,
    onDownload: () -> Unit,
    onClearDownloads: () -> Unit,
    onDismissMessage: () -> Unit,
    onBeginAreaSelection: () -> Unit,
    onCancelAreaSelection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isPickingArea by rememberSaveable { mutableStateOf(false) }

    if (isPickingArea) {
        OfflineAreaPicker(
            state = offlineRegionState,
            onViewportChanged = onViewportChanged,
            onConfirm = { isPickingArea = false },
            onCancel = {
                isPickingArea = false
                onCancelAreaSelection()
            },
            modifier = modifier,
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            OfflineRegionPanel(
                state = offlineRegionState,
                onDownload = onDownload,
                onClearDownloads = onClearDownloads,
                onDismissMessage = onDismissMessage,
                onEditArea = {
                    onBeginAreaSelection()
                    isPickingArea = true
                },
            )
        }
    }
}


@Composable
private fun OfflineAreaPicker(
    state: OfflineRegionUiState,
    onViewportChanged: (CoordinateBounds) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mapViewportState = rememberMapViewportState {
        MapCameraPreferences.getSavedCameraOptions(context)?.let { saved ->
            setCameraOptions(
                CameraOptions.Builder()
                    .center(saved.center)
                    .zoom(maxOf(saved.zoom ?: MIN_SELECTION_ZOOM, MIN_SELECTION_ZOOM))
                    .build()
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState,
        ) {
            MapEffect(mapViewportState.cameraState) { mapView ->
                val camera = mapViewportState.cameraState ?: return@MapEffect
                onViewportChanged(
                    mapView.mapboxMap.coordinateBoundsForCamera(camera.toCameraOptions())
                )
            }
        }
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Set offline area",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Pan and zoom to frame the area to keep offline.",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onConfirm) {
                        Text("Done")
                    }
                    TextButton(onClick = onCancel) {
                        Text("Cancel")
                    }
                }
            }
        }
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            OfflineAreaSummary(
                state = state,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
