package com.p56.safereach.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.p56.safereach.Alert
import com.p56.safereach.AlertStateRepository
import com.p56.safereach.AlertType
import com.p56.safereach.Severity

@Composable
fun DebugScreen(
    onUpdateAlert: (Alert) -> Unit,
) {
    val currentMessage by AlertStateRepository.message.collectAsStateWithLifecycle()

    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable { mutableStateOf(AlertType.EARTHQUAKE) }
    var selectedSeverity by rememberSaveable { mutableStateOf(Severity.MEDIUM) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var severityMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ALERT",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (currentMessage != null) {
                Text(text = currentMessage!!.title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = currentMessage!!.description)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Type: ${currentMessage!!.type.name}, Severity: ${currentMessage!!.severity.name}",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                val formattedTime = remember(currentMessage) {
                    java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                        .format(java.util.Date(currentMessage!!.timestamp))
                }
                Text(
                    text = "Last updated: $formattedTime",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(
                    text = "No alert set",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        label = { Text("Title") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = description,
        onValueChange = { description = it },
        label = { Text("Description") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(8.dp))

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

    Spacer(modifier = Modifier.height(12.dp))

    Button(
        onClick = {
            val alert = Alert(
                System.currentTimeMillis(),
                title.ifBlank { "" },
                description.ifBlank { "" },
                selectedType,
                selectedSeverity
            )
            onUpdateAlert(alert)
            title = ""
            description = ""
        },
        enabled = title.isNotBlank() || description.isNotBlank()
    ) {
        Text("Send Over Mesh")
    }
}