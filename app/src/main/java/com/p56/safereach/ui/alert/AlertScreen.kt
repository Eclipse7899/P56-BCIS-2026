package com.p56.safereach.ui.alert

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.p56.safereach.model.Alert
import com.p56.safereach.model.Shape
import com.p56.safereach.ui.theme.SafeReachTheme

@Composable
fun AlertScreen(
    alert: Alert? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = Modifier.fillMaxSize()
        .padding(16.dp),
        verticalArrangement = Arrangement.Top) {
        Text(
            text = "Alerts",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (alert == null) {
            Card(modifier = Modifier.fillMaxWidth()){
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "No Active Alerts",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "There are currently no active disaster alerts."
                    )
                }
            }
        } else {
            ActiveAlertDetails(alert = alert)
        }
    }
}

@Composable
private fun ActiveAlertDetails(
    alert: Alert
) {
    Card(modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "ACTIVE ALERT",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = alert.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = alert.description,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Type: ${alert.type.displayName}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Severity: ${alert.severity.displayName}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Location",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            when (val shape = alert.shape) {
                is Shape.Circle -> {
                    Text(text = "Alert radius: ${shape.radius.toInt()} metres")
                    Text(text = "Latitude: ${shape.center.latitude}")
                    Text(text = "Longitude: ${shape.center.longitude}")
                }
                is Shape.Polygon -> {
                    Text(
                        text = "Alert area: ${shape.points.size} points"
                    )
                }
                null -> {
                    Text(
                        text = "No location information available."
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Timestamp: ${alert.timestamp}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AlertScreenPreview() {
    SafeReachTheme {
        AlertScreen()
    }
}