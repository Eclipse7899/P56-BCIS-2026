@file:Suppress("LongMethod")

package com.p56.safereach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.p56.safereach.android.BlePermissions
import com.p56.safereach.android.MeshForegroundService
import com.p56.safereach.ui.auth.MainScreen
import com.p56.safereach.ui.theme.SafeReachTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestBlePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        if (hasBlePermissions()) {
            startBleService()
        }
    }

    private fun hasBlePermissions(): Boolean = BlePermissions.hasBlePermissions(this)

    private fun startBleService() {
        MeshForegroundService.start(this)
    }

    private fun ensureBleServiceRunning() {
        if (hasBlePermissions()) {
            startBleService()
        } else {
            requestBlePermissionsLauncher.launch(
                BlePermissions.requiredRuntimePermissions().toTypedArray()
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureBleServiceRunning()
        enableEdgeToEdge()
        setContent {
            SafeReachTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(context = this)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasBlePermissions()) {
            startBleService()
        }
    }
}
