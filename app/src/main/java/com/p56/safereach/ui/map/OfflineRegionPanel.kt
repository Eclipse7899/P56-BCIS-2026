package com.p56.safereach.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.p56.safereach.service.MAX_OFFLINE_REGION_SIZE_KM
import com.p56.safereach.service.RegionSizeKm
import com.p56.safereach.ui.theme.SafeReachTheme
import kotlin.math.roundToInt


@Composable
fun OfflineAreaSummary(
    state: OfflineRegionUiState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        val size = state.sizeKm
        Text(
            text = when {
                size == null -> "No offline area selected."
                else -> "Selected area: %.1f x %.1f km (max %.0f x %.0f)".format(
                    size.widthKm,
                    size.heightKm,
                    MAX_OFFLINE_REGION_SIZE_KM,
                    MAX_OFFLINE_REGION_SIZE_KM
                )
            },
            style = MaterialTheme.typography.bodySmall
        )

        if (state.isTooLarge) {
            Text(
                text = "Too large to save, zoom in until both sides are under " +
                        "%.0f km.".format(MAX_OFFLINE_REGION_SIZE_KM),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun OfflineRegionPanel(
    state: OfflineRegionUiState,
    onDownload: () -> Unit,
    onClearDownloads: () -> Unit,
    onDismissMessage: () -> Unit,
    onEditArea: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmClear by remember { mutableStateOf(false) }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Offline map area",
                style = MaterialTheme.typography.titleSmall
            )

            OfflineAreaSummary(state = state)

            when (val download = state.downloadState) {
                is OfflineDownloadState.Downloading -> {
                    LinearProgressIndicator(
                        progress = { download.progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Downloading: ${(download.progress * 100).roundToInt()}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                is OfflineDownloadState.Completed -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Saved for offline use: ${download.regionName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        TextButton(onClick = onDismissMessage) {
                            Text("Dismiss")
                        }
                    }
                }

                is OfflineDownloadState.Failed -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = download.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        TextButton(onClick = onDismissMessage) {
                            Text("Dismiss")
                        }
                    }
                }

                OfflineDownloadState.Idle -> Unit
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onDownload,
                    enabled = state.canDownload
                ) {
                    Text("Download area")
                }
                OutlinedButton(
                    onClick = onEditArea,
                    enabled = !state.isClearing
                ) {
                    Text(if (state.sizeKm == null) "Set offline area" else "Change area")
                }
            }

            TextButton(
                onClick = { confirmClear = true },
                enabled = !state.isClearing
            ) {
                Text(if (state.isClearing) "Clearing..." else "Clear offline data")
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear offline data?") },
            text = { Text("Removes every downloaded offline area and the tile cache.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        onClearDownloads()
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OfflineRegionPanelPreview() {
    SafeReachTheme {
        OfflineRegionPanel(
            state = OfflineRegionUiState(
                sizeKm = RegionSizeKm(widthKm = 43.2, heightKm = 27.8),
                downloadState = OfflineDownloadState.Downloading(0.42f)
            ),
            onDownload = {},
            onClearDownloads = {},
            onDismissMessage = {},
            onEditArea = {},
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OfflineRegionPanelTooLargePreview() {
    SafeReachTheme {
        OfflineRegionPanel(
            state = OfflineRegionUiState(
                sizeKm = RegionSizeKm(widthKm = 240.5, heightKm = 180.1)
            ),
            onDownload = {},
            onClearDownloads = {},
            onDismissMessage = {},
            onEditArea = {},
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OfflineRegionPanelNothingSelectedPreview() {
    SafeReachTheme {
        OfflineRegionPanel(
            state = OfflineRegionUiState(),
            onDownload = {},
            onClearDownloads = {},
            onDismissMessage = {},
            onEditArea = {},
            modifier = Modifier.padding(8.dp)
        )
    }
}
