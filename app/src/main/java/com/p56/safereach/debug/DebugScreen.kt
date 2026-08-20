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
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.p56.safereach.Alert
import com.p56.safereach.AlertStateRepository
import com.p56.safereach.AlertType
import com.p56.safereach.MeshMeta
import com.p56.safereach.Severity
import com.p56.safereach.mesh.MeshStatusRepository
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvState
import com.p56.safereach.mesh.ble.fsm.BleState
import com.p56.safereach.mesh.ble.scanning.fsm.BleScanState
import com.p56.safereach.ui.theme.StatusError
import com.p56.safereach.ui.theme.StatusInfo
import com.p56.safereach.ui.theme.StatusNeutral
import com.p56.safereach.ui.theme.StatusSuccess

@Composable
fun DebugScreen(
    onUpdateAlert: (Alert) -> Unit,
) {
    val msg by AlertStateRepository.alert.collectAsStateWithLifecycle()
    val isMeshRunning by MeshStatusRepository.isServiceRunning.collectAsStateWithLifecycle()
    val orchestratorState by MeshStatusRepository.orchestratorState.collectAsStateWithLifecycle()
    val advertiserState by MeshStatusRepository.advertiserState.collectAsStateWithLifecycle()
    val scannerState by MeshStatusRepository.scannerState.collectAsStateWithLifecycle()

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
            if (msg != null) {
                AlertDebugCard(msg = msg!!)
            }
            SetStatusDebugCard {
                onUpdateAlert(it)
            }
            MeshStatusCollapsibleCard(
                isMeshRunning = isMeshRunning,
                orchestratorState = orchestratorState,
                advertiserState = advertiserState,
                scannerState = scannerState
            )
        }
    }
}

@Composable
fun AlertDebugCard(
    msg: MeshMeta<Alert>
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Alert",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                )
                Button(
                    onClick = {
                        AlertStateRepository.clear()
                    }
                ) {
                    Text("Delete")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val alert = msg.packet.data
            Text(text = alert.title, style = MaterialTheme.typography.titleMedium)
            Text(text = alert.description)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Type: ${alert.type.name}, Severity: ${alert.severity.name}",
                style = MaterialTheme.typography.bodySmall
            )
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
                text = "Hop Count: ${msg.packet.hopCount}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun SetStatusDebugCard(
    onSent: (Alert) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable { mutableStateOf(AlertType.EARTHQUAKE) }
    var selectedSeverity by rememberSaveable { mutableStateOf(Severity.MEDIUM) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var severityMenuExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
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
                        Text("Type: ${selectedType.name}")
                    }
                    DropdownMenu(
                        expanded = typeMenuExpanded,
                        onDismissRequest = { typeMenuExpanded = false }
                    ) {
                        AlertType.entries.forEach { t ->
                            DropdownMenuItem(text = { Text(t.name) }, onClick = {
                                selectedType = t
                                typeMenuExpanded = false
                            })
                        }
                    }
                }

                Box {
                    TextButton(
                        onClick = { severityMenuExpanded = true }
                    ) {
                        Text("Severity: ${selectedSeverity.name}")
                    }
                    DropdownMenu(
                        expanded = severityMenuExpanded,
                        onDismissRequest = { severityMenuExpanded = false }
                    ) {
                        Severity.entries.forEach { s ->
                            DropdownMenuItem(text = { Text(s.name) }, onClick = {
                                selectedSeverity = s
                                severityMenuExpanded = false
                            })
                        }
                    }
                }
            }
            Button(
                onClick = {
                    val alert = Alert(
                        System.currentTimeMillis(),
                        title.ifBlank { "" },
                        description.ifBlank { "" },
                        selectedType,
                        selectedSeverity
                    )
                    onSent(alert)
                    title = ""
                    description = ""
                },
                enabled = title.isNotBlank() || description.isNotBlank()
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
                }
                val orchStatusColor = when (orchestratorState) {
                    is BleState.Active -> StatusSuccess
                    BleState.Passive -> StatusInfo
                    BleState.Off -> StatusNeutral
                    BleState.Failed -> StatusError
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
                        is BleAdvState.Failed -> "Failed (${advertiserState.error})"
                    }
                    val advStatusColor = when (advertiserState) {
                        is BleAdvState.Advertising -> StatusSuccess
                        BleAdvState.Idle -> StatusNeutral
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
                        BleScanState.Failed -> "Failed"
                    }
                    val scanStatusColor = when (scannerState) {
                        is BleScanState.Scanning -> StatusSuccess
                        BleScanState.Idle -> StatusNeutral
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
@Preview(showBackground = true)
fun DebugScreenPreview() {
    DebugScreen(onUpdateAlert = {})
}