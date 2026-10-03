package com.p56.safereach.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.auth.FirebaseAuth
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
import com.p56.safereach.ui.theme.DarkMode
import com.p56.safereach.ui.theme.ThemePreferences

private const val MIN_SELECTION_ZOOM = 11.0

@Composable
fun ProfileRoute(viewModel: OfflineRegionViewModel = hiltViewModel()) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val email = currentUser?.email ?: "Email"
    val username = currentUser?.email
        ?.substringBefore("@")
        ?: "User"
    val offlineRegionState by viewModel.uiState.collectAsStateWithLifecycle()
    var isPickingArea by rememberSaveable { mutableStateOf(false) }

    if (isPickingArea) {
        OfflineAreaPicker(
            state = offlineRegionState,
            onViewportChanged = viewModel::onViewportChanged,
            onConfirm = { isPickingArea = false },
            onCancel = {
                isPickingArea = false
                viewModel.cancelSelection()
            }
        )
    } else {
        ProfileContent(
            username = username,
            email = email,
            offlineRegionState = offlineRegionState,
            onDownload = viewModel::downloadSelection,
            onClearDownloads = viewModel::clearDownloads,
            onDismissMessage = viewModel::dismissMessage,
            onEditArea = {
                viewModel.beginSelection()
                isPickingArea = true
            },
            onSignOut = {
                FirebaseAuth.getInstance().signOut()
            }
        )
    }
}

@Composable
fun ProfileContent(
    username: String,
    email: String,
    offlineRegionState: OfflineRegionUiState,
    onDownload: () -> Unit,
    onClearDownloads: () -> Unit,
    onDismissMessage: () -> Unit,
    onEditArea: () -> Unit,
    onSignOut: () -> Unit
) {
    val notificationsEnabled by NotificationPreferences.notificationsEnabled
        .collectAsStateWithLifecycle()
    val darkMode by ThemePreferences.darkMode
        .collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Text(
            style = MaterialTheme.typography.headlineMedium,
            text = "Your Profile",
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Username",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = username,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Email",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = email,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Preferences",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Notifications",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Disaster Alerts",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = if (notificationsEnabled) {
                        "Enabled"
                    } else {
                        "Disabled"
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = notificationsEnabled,
                onCheckedChange = { enabled ->
                    NotificationPreferences.setNotificationsEnabled(enabled)
                }
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Appearance",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Theme",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        val darkModeOptions = listOf(
            DarkMode.LIGHT to "Light",
            DarkMode.DARK to "Dark",
            DarkMode.SYSTEM to "System"
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            darkModeOptions.forEachIndexed { index, (mode, label) ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = darkModeOptions.size
                    ),
                    onClick = { ThemePreferences.setDarkMode(mode) },
                    selected = darkMode == mode
                ) {
                    Text(label)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        OfflineRegionPanel(
            state = offlineRegionState,
            onDownload = onDownload,
            onClearDownloads = onClearDownloads,
            onDismissMessage = onDismissMessage,
            onEditArea = onEditArea
        )
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "About",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "SafeReach",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Disaster monitoring and emergency response application.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onSignOut
        ) {
            Text(text = "Sign Out",
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
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
    val mapViewportState = rememberMapViewportState {
        MapCameraPreferences.getSavedCameraOptions()?.let { saved ->
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
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ){
            Card(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                OfflineAreaSummary(
                    state = state,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Card(
                modifier = Modifier
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
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileRoutePreview() {
    ProfileContent(
        username = "User",
        email = "user@example.com",
        offlineRegionState = OfflineRegionUiState(),
        onDownload = {},
        onClearDownloads = {},
        onDismissMessage = {},
        onEditArea = {},
        onSignOut = {}
    )
}
