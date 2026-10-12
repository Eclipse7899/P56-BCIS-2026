package com.p56.safereach.ui.alert

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.p56.safereach.model.Alert
import com.p56.safereach.model.AlertType
import com.p56.safereach.model.Severity
import com.p56.safereach.model.isActive
import com.p56.safereach.ui.theme.SafeReachTheme
import com.p56.safereach.ui.theme.StatusError
import com.p56.safereach.ui.theme.StatusInfo
import com.p56.safereach.ui.theme.StatusNeutral
import com.p56.safereach.ui.theme.StatusSuccess
import com.p56.safereach.ui.theme.StatusWarning
import java.text.DateFormat.getDateInstance
import java.util.Date

@Composable
fun AlertHistoryRoute(
    viewModel: AlertHistoryViewModel = hiltViewModel()
) {
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    AlertHistoryScreen(alerts = alerts)
}

@Composable
fun AlertHistoryScreen(
    alerts: List<Alert>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Alert History",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (alerts.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No Previous Alerts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Previous disaster alerts will appear here.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = alerts,
                    key = { alert -> alert.id }) { alert ->
                    AlertCard(alert = alert)
                }
            }
        }
    }
}

@Composable
fun AlertCard(
    alert: Alert
) {
    val accentColor = severityColor(alert.severity)
    val isActive = alert.isActive()
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Severity accent stripe.
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )
            Column(modifier = Modifier.padding(16.dp))
            {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SeverityBadge(severity = alert.severity)
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = relativeTime(alert.timestamp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isActive) "Active" else "Expired",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) StatusSuccess else StatusNeutral
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = alert.title.ifBlank { "${alert.type.displayName} alert" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (alert.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = alert.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AlertFact(
                        label = "Type",
                        value = alert.type.displayName,
                        modifier = Modifier.weight(1f)
                    )
                    AlertFact(
                        label = "Reported",
                        value = getDateInstance().format(Date(alert.timestamp)),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SeverityBadge(severity: Severity) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(severityColor(severity))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = "${severity.displayName.uppercase()} SEVERITY",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun AlertFact(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun severityColor(severity: Severity): Color = when (severity) {
    Severity.LOW -> StatusInfo
    Severity.MEDIUM -> StatusWarning
    Severity.HIGH -> StatusError
    Severity.UNKNOWN -> StatusNeutral
}

private fun relativeTime(
    timestamp: Long,
    now: Long = System.currentTimeMillis()
): String {
    val elapsed = now - timestamp
    val minutes = elapsed / 60_000
    val hours = elapsed / 3_600_000
    val days = elapsed / 86_400_000
    return when {
        elapsed < 0 -> "Just now"
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> getDateInstance().format(Date(timestamp))
    }
}

@Preview(showBackground = true)
@Composable
fun AlertHistoryScreenPreview() {
    SafeReachTheme {
        AlertHistoryScreen(alerts = emptyList())
    }
}

@Preview(showBackground = true)
@Composable
private fun AlertCardPreview() {
    val now = System.currentTimeMillis()
    SafeReachTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AlertCard(
                alert = Alert(
                    id = 1,
                    timestamp = now - 45 * 60_000,
                    title = "Tsunami advisory issued",
                    description = "Sea level gauges have returned to normal. The evacuation " +
                            "advisory for the east coast has been lifted effective immediately.",
                    type = AlertType.TSUNAMI,
                    severity = Severity.HIGH,
                    expiresAt = now - 15 * 60_000,
                    latitude = 39.0,
                    longitude = -77.0,
                    radius = 100,
                    invalidated = false
                )
            )
            AlertCard(
                alert = Alert(
                    id = 2,
                    timestamp = now - 5 * 3_600_000,
                    title = "Coastal flood warning",
                    description = "Minor flooding expected along low-lying roads during the " +
                            "next high tide.",
                    type = AlertType.FLOOD,
                    severity = Severity.MEDIUM,
                    expiresAt = now + 3_600_000,
                    latitude = 39.0,
                    longitude = -77.0,
                    radius = 100,
                    invalidated = false
                )
            )
            AlertCard(
                alert = Alert(
                    id = 3,
                    timestamp = now - 32 * 86_400_000,
                    title = "Earthquake early-warning test",
                    description = "Routine test of the regional early-warning network. " +
                            "No action was required.",
                    type = AlertType.EARTHQUAKE,
                    severity = Severity.LOW,
                    latitude = 39.0,
                    longitude = -77.0,
                    expiresAt = now - 32 * 86_400_000,
                    radius = 100,
                    invalidated = true
                )
            )
        }
    }
}
