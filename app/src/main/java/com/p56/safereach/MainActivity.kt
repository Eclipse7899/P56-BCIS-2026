@file:Suppress("LongMethod")

package com.p56.safereach

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.p56.safereach.auth.MainScreen
import com.p56.safereach.mesh.BlePermissions
import com.p56.safereach.mesh.MeshService
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
        val intent = Intent(this, MeshService::class.java)
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
                MainScreen(context = this)
            }
        }
    }
}
