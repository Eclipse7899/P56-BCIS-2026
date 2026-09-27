package com.p56.safereach.alert

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.p56.safereach.Alert
import com.p56.safereach.AlertStateRepository
import com.p56.safereach.ui.theme.SafeReachTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.remember

@Composable
fun AlertHistoryScreen(
    modifier: Modifier = Modifier
) {
    val alertHistory by AlertStateRepository.alertHistory
        .collectAsStateWithLifecycle()
    Column(modifier = modifier
        .fillMaxSize()
        .padding(16.dp)
    ) {
        Text(
            text = "Alert History",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (alertHistory.isEmpty()){
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center
            ){
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
                items(alertHistory) {alert -> PreviousAlertCard(alert = alert)
                }
            }
        }
    }
}

@Composable
private fun PreviousAlertCard(
    alert: Alert
) {
    val formattedTime = remember(alert.timestamp){
        SimpleDateFormat(
            "dd/MM/yyyy HH:mm",
            Locale.getDefault()
        ).format(Date(alert.timestamp))
    }
    Card(
        modifier = Modifier.fillMaxWidth()
    ){
        Column(modifier = Modifier.padding(16.dp))
        {
            Text(
                text = alert.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = alert.description,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Type: ${alert.type.name}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Severity: ${alert.severity.name}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Time: $formattedTime",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AlertHistoryScreenPreview(){
    SafeReachTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ){
            Text(
                text = "Alert History",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
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
    }
}