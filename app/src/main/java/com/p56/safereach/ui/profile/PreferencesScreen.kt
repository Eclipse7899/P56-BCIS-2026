package com.p56.safereach.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.p56.safereach.ui.theme.DarkMode
import com.p56.safereach.ui.theme.ThemePreferences

@Composable
fun PreferencesScreen() {
    val notificationsEnabled by NotificationPreferences.notificationsEnabled.collectAsStateWithLifecycle()
    val darkMode by ThemePreferences.darkMode.collectAsStateWithLifecycle()
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
                    NotificationPreferences.setNotificationsEnabled(it)
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
    }
}

@Preview(showBackground = true)
@Composable
fun PreferencesScreenPreview() {
    PreferencesScreen()
}