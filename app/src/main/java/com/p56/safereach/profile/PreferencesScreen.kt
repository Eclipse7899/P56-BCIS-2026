package com.p56.safereach.profile

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun PreferencesScreen() {
    var notificationsEnabled by rememberSaveable { mutableStateOf(true) }
    var darkModeEnabled by rememberSaveable { mutableStateOf(false) }
    Column(modifier = Modifier.padding(24.dp)) {
        Text(
            text = "Preferences",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))
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
        ){
            Column {
                Text(
                    text = "Disaster Alerts",
                    style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "Receive SafeReach alerts",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = notificationsEnabled,
                onCheckedChange = {
                    notificationsEnabled = it
                    // TODO - Connect to the app's notification system.
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) { Column {
            Text(
                text = "Dark Mode",
                style = MaterialTheme.typography.bodyLarge
            )
            Text (
                text = "Use a dark appearance for SafeReach",
                style = MaterialTheme.typography.bodySmall
            )
        }
            Switch(
                checked = darkModeEnabled,
                onCheckedChange = {
                    darkModeEnabled = it
                    // TODO - Connect to the SafeReach app theme.
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreferencesScreenPreview() {
    PreferencesScreen()
}