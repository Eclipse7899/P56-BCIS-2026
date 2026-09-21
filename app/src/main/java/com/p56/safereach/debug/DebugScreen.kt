package com.p56.safereach.debug

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
import com.p56.safereach.MeshMeta
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvState
import com.p56.safereach.mesh.ble.fsm.BleState
import com.p56.safereach.mesh.ble.scanning.fsm.BleScanState
import com.p56.safereach.proto.Alert
import com.p56.safereach.proto.AlertType
import com.p56.safereach.proto.LatLong
import com.p56.safereach.proto.Severity
import com.p56.safereach.proto.Shape
import com.p56.safereach.ui.theme.StatusError
import com.p56.safereach.ui.theme.StatusInfo
import com.p56.safereach.ui.theme.StatusNeutral
import com.p56.safereach.ui.theme.StatusSuccess


@Composable
fun DebugRoute(
    viewModel: DebugViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DebugScreen(
        uiState = uiState,
        clearAlert = viewModel::clearAlert,
        updateAlert = viewModel::updateAlert,
    )
}

@Composable
fun DebugScreen(
    uiState: DebugUiState,
    clearAlert: () -> Unit,
    updateAlert: (Alert) -> Unit,
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
            if (uiState.msg != null && uiState.alert != null) {
                AlertDebugCard(
                    msg = uiState.msg,
                    alert = uiState.alert,
                    onDelete = clearAlert
                )
            }
            SetStatusDebugCard { alert ->
                updateAlert(alert)
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
    msg: MeshMeta,
    alert: Alert,
    onDelete: () -> Unit = {},
) {
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
                text = "Type: ${alert.type.name}, Severity: ${alert.severity.name}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Location", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            when (val shapeType = alert.shape.kindCase) {
                Shape.KindCase.CIRCLE -> {
                    Text(
                        text = "Circle: Radius: ${alert.shape.circle.radius} meters",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Shape.KindCase.POLYGON -> {
                    Text(
                        text = "Polygon: ${alert.shape.polygon.pointsCount} points",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                else -> {
                    Text(
                        text = "Unknown shape type: $shapeType",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            val formattedAlertTime = remember(msg) {
                java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                    .format(java.util.Date(alert.timestamp))
            }
            Text(
                text = "Time: $formattedAlertTime",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Metadata", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            val formattedReceivedTime = remember(msg) {
                java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                    .format(java.util.Date(msg.receivedAt))
            }
            Text(
                text = "Received at: $formattedReceivedTime",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Received from: ${msg.receivedFrom ?: "Unknown"}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            val formattedTimeDifference = remember(msg) {
                val timeDifference = msg.receivedAt - alert.timestamp
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
            Text(text = "Packet", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Hop Count: ${msg.hopCount}",
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
fun SetStatusDebugCard(
    onSent: (Alert) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable { mutableStateOf(AlertType.ALERT_TYPE_EARTHQUAKE) }
    var selectedSeverity by rememberSaveable { mutableStateOf(Severity.SEVERITY_MEDIUM) }
    var lat by rememberSaveable { mutableStateOf<Double?>(null) }
    var long by rememberSaveable { mutableStateOf<Double?>(null) }
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
                    lat = newLatLong.latitude
                    long = newLatLong.longitude
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
                        when (selectedType) {
                            AlertType.ALERT_TYPE_EARTHQUAKE -> Text("Type: Earthquake")
                            AlertType.ALERT_TYPE_FIRE -> Text("Type: Fire")
                            AlertType.ALERT_TYPE_FLOOD -> Text("Type: Flood")
                            else -> {
                                Text("Type: Unknown")
                            }
                        }
                    }
                    DropdownMenu(
                        expanded = typeMenuExpanded,
                        onDismissRequest = { typeMenuExpanded = false }
                    ) {
                        AlertType.entries.filter { it != AlertType.UNRECOGNIZED }.forEach { t ->
                            DropdownMenuItem(
                                text = {
                                    when (t) {
                                        AlertType.ALERT_TYPE_EARTHQUAKE -> Text("Earthquake")
                                        AlertType.ALERT_TYPE_FIRE -> Text("Fire")
                                        AlertType.ALERT_TYPE_FLOOD -> Text("Flood")
                                        else -> {
                                            Text("Unknown")
                                        }
                                    }
                                },
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
                        when (selectedSeverity) {
                            Severity.SEVERITY_LOW -> Text("Severity: Low")
                            Severity.SEVERITY_MEDIUM -> Text("Severity: Medium")
                            Severity.SEVERITY_HIGH -> Text("Severity: High")
                            else -> {
                                Text("Severity: Unknown")
                            }
                        }
                    }
                    DropdownMenu(
                        expanded = severityMenuExpanded,
                        onDismissRequest = { severityMenuExpanded = false }
                    ) {
                        Severity.entries.filter { it != Severity.UNRECOGNIZED }.forEach { s ->
                            DropdownMenuItem(
                                text = {
                                    when (s) {
                                        Severity.SEVERITY_LOW -> Text("Low")
                                        Severity.SEVERITY_MEDIUM -> Text("Medium")
                                        Severity.SEVERITY_HIGH -> Text("High")
                                        else -> {
                                            Text("Unknown")
                                        }
                                    }
                                },
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
                    val location = LatLong.newBuilder()
                        .setLatitude(lat ?: 0.0)
                        .setLongitude(long ?: 0.0)
                        .build()
                    val circle = Shape.Circle.newBuilder()
                        .setCenter(location)
                        .setRadius(radius ?: 0.0)
                        .build()
                    val shape = Shape.newBuilder()
                        .setCircle(circle)
                        .build()
                    val alert = Alert.newBuilder()
                        .setTimestamp(System.currentTimeMillis())
                        .setTitle(title.ifBlank { "" })
                        .setDescription(description.ifBlank { "" })
                        .setType(selectedType)
                        .setSeverity(selectedSeverity)
                        .setShape(shape)
                        .build()
                    onSent(alert)
                },
                enabled =
                    title.isNotBlank() &&
                            lat != null &&
                            long != null &&
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
    orchestratorState: BleState,
    advertiserState: BleAdvState,
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
                val orchStatusText = when (orchestratorState) {
                    is BleState.Active -> "Active"
                    BleState.Passive -> "Passive"
                    BleState.Off -> "Off"
                    BleState.Failed -> "Failed"
                    BleState.Disabled -> "Disabled"
                }
                val orchStatusColor = when (orchestratorState) {
                    is BleState.Active -> StatusSuccess
                    BleState.Passive -> StatusInfo
                    BleState.Off -> StatusNeutral
                    BleState.Failed -> StatusError
                    BleState.Disabled -> StatusError
                }

                CollapsibleTreeNode(
                    title = "BLE Orchestrator",
                    statusText = orchStatusText,
                    statusColor = orchStatusColor,
                    isExpanded = isOrchestratorExpanded,
                    onToggle = { isOrchestratorExpanded = !isOrchestratorExpanded },
                    indentation = 16
                ) {
                    if (orchestratorState is BleState.Active) {
                        Text(
                            text = "Payload: ${orchestratorState.payload.size} bytes",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(start = 36.dp, top = 2.dp, bottom = 4.dp)
                        )
                    }
                    val advStatusText = when (advertiserState) {
                        is BleAdvState.Advertising -> "Advertising"
                        BleAdvState.Idle -> "Idle"
                        BleAdvState.Disabled -> "Disabled"
                        is BleAdvState.Failed -> "Failed (${advertiserState.error})"
                    }
                    val advStatusColor = when (advertiserState) {
                        is BleAdvState.Advertising -> StatusSuccess
                        BleAdvState.Idle -> StatusNeutral
                        BleAdvState.Disabled -> StatusError
                        is BleAdvState.Failed -> StatusError
                    }

                    CollapsibleTreeNode(
                        title = "Advertiser",
                        statusText = advStatusText,
                        statusColor = advStatusColor,
                        isExpanded = isAdvertiserExpanded,
                        onToggle = { isAdvertiserExpanded = !isAdvertiserExpanded },
                        indentation = 32
                    ) {
                        if (advertiserState is BleAdvState.Advertising) {
                            Text(
                                text = "Data Size: ${advertiserState.data.size} bytes",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(
                                    start = 52.dp,
                                    top = 2.dp,
                                    bottom = 2.dp
                                )
                            )
                        } else if (advertiserState is BleAdvState.Failed) {
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
                    val scanStatusText = when (scannerState) {
                        is BleScanState.Scanning -> "Scanning"
                        BleScanState.Idle -> "Idle"
                        BleScanState.Disabled -> "Disabled"
                        BleScanState.Failed -> "Failed"
                    }
                    val scanStatusColor = when (scannerState) {
                        is BleScanState.Scanning -> StatusSuccess
                        BleScanState.Idle -> StatusNeutral
                        BleScanState.Disabled -> StatusError
                        BleScanState.Failed -> StatusError
                    }

                    CollapsibleTreeNode(
                        title = "Scanner",
                        statusText = scanStatusText,
                        statusColor = scanStatusColor,
                        isExpanded = isScannerExpanded,
                        onToggle = { isScannerExpanded = !isScannerExpanded },
                        indentation = 32
                    ) {
                        if (scannerState is BleScanState.Scanning) {
                            Text(
                                text = "Callback Active: true",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(
                                    start = 52.dp,
                                    top = 2.dp,
                                    bottom = 2.dp
                                )
                            )
                        }
                    }
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
                                LatLong.newBuilder()
                                    .setLatitude(mapViewportState.cameraState!!.center.latitude())
                                    .setLongitude(mapViewportState.cameraState!!.center.longitude())
                                    .build(),
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
    DebugScreen(
        DebugUiState(
            msg = null,
            alert = null,
            isMeshRunning = true,
            orchestratorState = BleState.Active(ByteArray(10)),
            advertiserState = BleAdvState.Idle,
            scannerState = BleScanState.Scanning
        ),
        clearAlert = {},
        updateAlert = {}
    )
}