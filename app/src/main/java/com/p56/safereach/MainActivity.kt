@file:Suppress("LongMethod")

package com.p56.safereach

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.p56.safereach.mesh.BlePermissions
import com.p56.safereach.mesh.BleService
import com.p56.safereach.ui.theme.SafeReachTheme

class MainActivity : ComponentActivity() {

    private val requestBlePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val allGranted = grants.values.all { it }
        if (allGranted) {
            startBleService()
        }
    }

    private fun hasAllRequiredPermissions(): Boolean =
        BlePermissions.missingRuntimePermissions(this).isEmpty()

    private fun startBleService() {
        val intent = Intent(this, BleService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun ensureBleServiceRunning() {
        if (hasAllRequiredPermissions()) {
            startBleService()
        } else {
            requestBlePermissionsLauncher.launch(BlePermissions.requiredRuntimePermissions())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureBleServiceRunning()
        enableEdgeToEdge()
        setContent {
            SafeReachTheme {
                SafeReachApp()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun SafeReachApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            painterResource(it.icon),
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.HOME -> Greeting(
                    name = "Android",
                    modifier = Modifier.padding(innerPadding)
                )

                AppDestinations.FAVORITES -> Greeting(
                    name = "Favorites",
                    modifier = Modifier.padding(innerPadding)
                )

                AppDestinations.PROFILE -> Greeting(
                    name = "Profile",
                    modifier = Modifier.padding(innerPadding)
                )

                AppDestinations.DEBUG -> {
                    Column(
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        BleMessageEditor(onUpdateAlert = { alert ->
                            AlertStateRepository.update(alert)
                        })
                    }
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    HOME("Home", R.drawable.ic_home),
    FAVORITES("Favorites", R.drawable.ic_favorite),
    PROFILE("Profile", R.drawable.ic_account_box),
    DEBUG("Debug", R.drawable.ic_account_box),
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SafeReachTheme {
        Greeting("Android")
    }
}

@Composable
fun BleMessageEditor(
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
        modifier = Modifier
            .fillMaxWidth(),
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

