package com.p56.safereach.ui.debug

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvertisingState
import com.p56.safereach.mesh.ble.fsm.AggregateBleState
import com.p56.safereach.mesh.ble.scanning.fsm.BleScanState
import com.p56.safereach.model.Alert
import com.p56.safereach.model.AlertBatch
import com.p56.safereach.model.AlertType
import com.p56.safereach.model.LatLong
import com.p56.safereach.model.ReceivedMessage
import com.p56.safereach.model.Severity
import com.p56.safereach.model.Shape
import com.p56.safereach.model.SignedMessage
import com.p56.safereach.ui.theme.StatusError
import com.p56.safereach.ui.theme.StatusInfo
import com.p56.safereach.ui.theme.StatusNeutral
import com.p56.safereach.ui.theme.StatusSuccess
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val DEFAULT_ALERT_LIFETIME_MILLIS = 60 * 60 * 1000L

@Composable
fun DebugRoute(
    viewModel: DebugViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DebugScreen(
        uiState = uiState,
        onDelete = viewModel::deleteAlert,
        onAdded = viewModel::addAlert,
    )
}

@Composable
fun DebugScreen(
    uiState: DebugUiState,
    onDelete: () -> Unit,
    onAdded: (Alert) -> Unit,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            uiState.receivedMessage?.let { wrapper ->
                wrapper.batch.alerts.forEach { alert ->
                    AlertDebugCard(
                        alert = alert,
                        wrapper = wrapper,
                        onDelete = onDelete,
                    )
                }
            }

            GenerateAlertDebugCard { alert ->
                onAdded(alert)
            }
            MeshStatusCollapsibleCard(
                isMeshRunning = uiState.isMeshRunning,
                orchestratorState = uiState.orchestratorState,
                advertiserState = uiState.advertiserState,
                scannerState = uiState.scannerState,
            )
        }
    }
}

@Composable
fun AlertDebugCard(
    alert: Alert,
    wrapper: ReceivedMessage,
    onDelete: () -> Unit,
) {
    val signed = wrapper.signedMessage
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Active Alert",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = alert.title, style = MaterialTheme.typography.titleMedium)
            Text(text = alert.description)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Type: ${alert.type.displayName}, Severity: ${alert.severity.displayName}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Location", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            when (val shape = alert.shape) {
                is Shape.Circle -> {
                    Text(
                        text = "Circle: Radius: ${shape.radius} meters",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                is Shape.Polygon -> {
                    Text(
                        text = "Polygon: ${shape.points.size} points",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                null -> {
                    Text(
                        text = "Unknown shape type",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            val formattedAlertTime = remember(alert) {
                SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    .format(Date(alert.timestamp))
            }
            Text(
                text = "Time: $formattedAlertTime",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Metadata", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Received from: ${wrapper.receivedFrom}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            val formattedReceivedTime = remember(wrapper) {
                SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    .format(Date(wrapper.receivedAt))
            }
            Text(
                text = "Received at: $formattedReceivedTime",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            val formattedTimeDifference = remember(alert) {
                val timeDifference = wrapper.receivedAt - alert.timestamp
                val minutes = timeDifference / (1000 * 60)
                val seconds = (timeDifference % (1000 * 60)) / 1000
                if (minutes > 0) {
                    "$minutes minutes and $seconds seconds"
                } else {
                    "$seconds seconds"
                }
            }
            Text(
                text = "Time difference: $formattedTimeDifference",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Signed Message", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            val formattedSignedTimestamp = remember(wrapper) {
                SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    .format(Date(wrapper.batch.timestamp))
            }
            Text(
                text = "Timestamp: $formattedSignedTimestamp",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Data: ${signed.payload.size} bytes",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Signature: ${signed.signature.size} bytes",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.error
                ),
                onClick = onDelete
            ) {
                Text("Delete")
            }
        }
    }
}

@Composable
fun GenerateAlertDebugCard(
    onAlertGenerated: (Alert) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable { mutableStateOf(AlertType.EARTHQUAKE) }
    var selectedSeverity by rememberSaveable { mutableStateOf(Severity.MEDIUM) }
    var latitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var longitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var radius by rememberSaveable { mutableStateOf<Double?>(null) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var severityMenuExpanded by remember { mutableStateOf(false) }
    var openAlertDialog by remember { mutableStateOf(false) }
    when {
        openAlertDialog -> {
            LocationSelectDialog(
                onDismissRequest = { openAlertDialog = false },
                onConfirmation = { newLatLong, newRadius ->
                    openAlertDialog = false
                    latitude = newLatLong.latitude
                    longitude = newLatLong.longitude
                    radius = newRadius
                },
            )
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Generate Alert", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    TextButton(onClick = { typeMenuExpanded = true }) {
                        Text("Type: ${selectedType.displayName}")
                    }
                    DropdownMenu(
                        expanded = typeMenuExpanded,
                        onDismissRequest = { typeMenuExpanded = false }
                    ) {
                        AlertType.entries.filter { it != AlertType.UNKNOWN }.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.displayName) },
                                onClick = {
                                    selectedType = t
                                    typeMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Box {
                    TextButton(
                        onClick = { severityMenuExpanded = true }
                    ) {
                        Text("Severity: ${selectedSeverity.displayName}")
                    }
                    DropdownMenu(
                        expanded = severityMenuExpanded,
                        onDismissRequest = { severityMenuExpanded = false }
                    ) {
                        Severity.entries.filter { it != Severity.UNKNOWN }.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.displayName) },
                                onClick = {
                                    selectedSeverity = s
                                    severityMenuExpanded = false
                                })
                        }
                    }
                }
            }
            Button(
                onClick = {
                    openAlertDialog = true
                }
            ) {
                Text("Set Location")
            }
            Button(
                onClick = {
                    val shape = Shape.Circle(
                        center = LatLong(
                            latitude = latitude ?: 0.0,
                            longitude = longitude ?: 0.0,
                        ),
                        radius = radius ?: 0.0,
                    )
                    val version = System.currentTimeMillis()
                    val alert = Alert(
                        id = "debug",
                        timestamp = version,
                        title = title.ifBlank { "" },
                        description = description.ifBlank { "" },
                        type = selectedType,
                        severity = selectedSeverity,
                        shape = shape,
                        version = version,
                        expiresAt = version + DEFAULT_ALERT_LIFETIME_MILLIS,
                        invalidated = false,
                    )
                    onAlertGenerated(alert)
                },
                enabled =
                    title.isNotBlank() &&
                            latitude != null &&
                            longitude != null &&
                            radius != null
            ) {
                Text("Send Over Mesh")
            }
        }
    }
}

@Composable
fun MeshStatusCollapsibleCard(
    isMeshRunning: Boolean,
    orchestratorState: AggregateBleState,
    advertiserState: BleAdvertisingState,
    scannerState: BleScanState,
) {
    var isMeshExpanded by rememberSaveable { mutableStateOf(true) }
    var isOrchestratorExpanded by rememberSaveable { mutableStateOf(true) }
    var isAdvertiserExpanded by rememberSaveable { mutableStateOf(true) }
    var isScannerExpanded by rememberSaveable { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "System Status",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            CollapsibleTreeNode(
                title = "Mesh Service",
                statusText = if (isMeshRunning) "Running" else "Stopped",
                statusColor = if (isMeshRunning) StatusSuccess else StatusError,
                isExpanded = isMeshExpanded,
                onToggle = { isMeshExpanded = !isMeshExpanded },
                indentation = 0
            ) {
                val orchestratorStatusText = when (orchestratorState) {
                    is AggregateBleState.Active -> "Active"
                    AggregateBleState.Passive -> "Passive"
                    AggregateBleState.Off -> "Off"
                    AggregateBleState.PartialFail -> "Partial failure"
                    AggregateBleState.CompleteFail -> "Failed"
                    AggregateBleState.Unknown -> "Unknown"
                }
                val orchestratorStatusColor = when (orchestratorState) {
                    is AggregateBleState.Active -> StatusSuccess
                    AggregateBleState.Passive -> StatusInfo
                    AggregateBleState.Off -> StatusNeutral
                    AggregateBleState.PartialFail -> StatusError
                    AggregateBleState.CompleteFail -> StatusError
                    AggregateBleState.Unknown -> StatusNeutral
                }

                CollapsibleTreeNode(
                    title = "BLE Orchestrator",
                    statusText = orchestratorStatusText,
                    statusColor = orchestratorStatusColor,
                    isExpanded = isOrchestratorExpanded,
                    onToggle = { isOrchestratorExpanded = !isOrchestratorExpanded },
                    indentation = 16
                ) {
                    if (orchestratorState is AggregateBleState.Active) {
                        Text(
                            text = "Payload: ${orchestratorState.payload.size} bytes",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(start = 36.dp, top = 2.dp, bottom = 4.dp)
                        )
                    }
                    val advertiserStatusText = when (advertiserState) {
                        is BleAdvertisingState.Advertising -> "Advertising"
                        BleAdvertisingState.Idle -> "Idle"
                        is BleAdvertisingState.Starting -> "Starting"
                        BleAdvertisingState.Stopping -> "Stopping"
                        is BleAdvertisingState.Retrying -> "Retrying (${advertiserState.error})"
                    }
                    val advertiserStatusColor = when (advertiserState) {
                        is BleAdvertisingState.Advertising -> StatusSuccess
                        BleAdvertisingState.Idle -> StatusNeutral
                        is BleAdvertisingState.Starting -> StatusInfo
                        BleAdvertisingState.Stopping -> StatusInfo
                        is BleAdvertisingState.Retrying -> StatusError
                    }

                    CollapsibleTreeNode(
                        title = "Advertiser",
                        statusText = advertiserStatusText,
                        statusColor = advertiserStatusColor,
                        isExpanded = isAdvertiserExpanded,
                        onToggle = { isAdvertiserExpanded = !isAdvertiserExpanded },
                        indentation = 32
                    ) {
                        if (advertiserState is BleAdvertisingState.Advertising) {
                            Text(
                                text = "Data Size: ${advertiserState.payload.size} bytes",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(
                                    start = 52.dp,
                                    top = 2.dp,
                                    bottom = 2.dp
                                )
                            )
                        } else if (advertiserState is BleAdvertisingState.Retrying) {
                            Text(
                                text = "Error: ${advertiserState.error}",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusError,
                                modifier = Modifier.padding(
                                    start = 52.dp,
                                    top = 2.dp,
                                    bottom = 2.dp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    val scannerStatusText = when (scannerState) {
                        is BleScanState.Starting -> "Starting"
                        is BleScanState.Scanning -> "Scanning"
                        BleScanState.Idle -> "Idle"
                        BleScanState.Stopping -> "Stopping"
                        is BleScanState.Retrying -> "Retrying: ${scannerState.error}, attempt ${scannerState.attempt}"
                    }
                    val scannerStatusColor = when (scannerState) {
                        is BleScanState.Starting, is BleScanState.Scanning -> StatusSuccess
                        BleScanState.Idle, BleScanState.Stopping -> StatusNeutral
                        is BleScanState.Retrying -> StatusError
                    }

                    CollapsibleTreeNode(
                        title = "Scanner",
                        statusText = scannerStatusText,
                        statusColor = scannerStatusColor,
                        isExpanded = isScannerExpanded,
                        onToggle = { isScannerExpanded = !isScannerExpanded },
                        indentation = 32
                    )
                }
            }
        }
    }
}

@Composable
fun StatusBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun CollapsibleTreeNode(
    title: String,
    statusText: String,
    statusColor: Color,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    indentation: Int,
    content: @Composable () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .clip(RoundedCornerShape(4.dp))
                .padding(start = indentation.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isExpanded) "▼" else "▶",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(16.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(8.dp))
            StatusBadge(
                text = statusText,
                color = statusColor
            )
        }
        AnimatedVisibility(visible = isExpanded) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun LocationSelectDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: (LatLong, Double) -> Unit,
) {
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val mapViewportState = rememberMapViewportState()
                var radius by remember { mutableDoubleStateOf(10.0) }
                MapboxMap(
                    Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(4.dp)
                        .clip(shape = RoundedCornerShape(24.dp)),
                    mapViewportState = mapViewportState
                ) {
                    MapEffect(Unit) { mapView ->
                        mapView.location.updateSettings {
                            locationPuck = createDefault2DPuck(withBearing = true)
                            enabled = true
                            puckBearing = PuckBearing.COURSE
                            puckBearingEnabled = true
                        }
                        mapViewportState.transitionToFollowPuckState()
                    }
                }
                Text(
                    text = "Radius: ${radius.toInt()} meters",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(8.dp)
                )
                Slider(
                    value = radius.toFloat(),
                    onValueChange = { radius = it.toDouble() },
                    valueRange = 10f..1000f,
                    modifier = Modifier.padding(8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = { onDismissRequest() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text("Dismiss")
                    }
                    TextButton(
                        onClick = {
                            if (mapViewportState.cameraState == null) return@TextButton
                            onConfirmation(
                                LatLong(
                                    latitude = mapViewportState.cameraState!!.center.latitude(),
                                    longitude = mapViewportState.cameraState!!.center.longitude(),
                                ),
                                radius
                            )
                        },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
fun DebugScreenPreview() {
    val now = System.currentTimeMillis()
    DebugScreen(
        uiState = DebugUiState(
            receivedMessage = ReceivedMessage(
                signedMessage = SignedMessage(
                    ByteArray(10),
                    ByteArray(10),
                    "Debug",
                    1,
                    false
                ),
                batch = AlertBatch(
                    alerts = listOf(
                        Alert(
                            id = "preview",
                            timestamp = now,
                            title = "Test Alert",
                            description = "This is a test alert for preview purposes.",
                            type = AlertType.FIRE,
                            severity = Severity.HIGH,
                            shape = Shape.Circle(
                                center = LatLong(37.7749, -122.4194),
                                radius = 100.0,
                            ),
                            version = now,
                            expiresAt = now + 60_000,
                            invalidated = false,
                        )
                    ),
                    validUntil = now + 60_000,
                    timestamp = now,
                ),
                receivedFrom = "Device123",
                receivedAt = now,
            ),
            isMeshRunning = true,
            orchestratorState = AggregateBleState.Active(ByteArray(10)),
            advertiserState = BleAdvertisingState.Idle,
            scannerState = BleScanState.Scanning
        ),
        onDelete = {},
        onAdded = {}
    )
}