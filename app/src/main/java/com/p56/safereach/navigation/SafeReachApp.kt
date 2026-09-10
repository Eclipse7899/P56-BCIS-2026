package com.p56.safereach.navigation

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.p56.safereach.AlertStateRepository
import com.p56.safereach.MeshMeta
import com.p56.safereach.MeshPacket
import com.p56.safereach.R
import com.p56.safereach.battery.BatteryOptimizationBanner
import com.p56.safereach.battery.BatteryOptimizationHelper
import com.p56.safereach.bluetooth.BluetoothDisabledBanner
import com.p56.safereach.bluetooth.BluetoothHelper
import com.p56.safereach.debug.DebugScreen
import com.p56.safereach.home.HomeScreen
import com.p56.safereach.mesh.MeshStatusRepository
import com.p56.safereach.mesh.ble.fsm.BleState
import com.p56.safereach.profile.ProfileScreen

@PreviewScreenSizes
@Composable
fun SafeReachApp() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isBatteryOptimizationOn by remember {
        mutableStateOf(BatteryOptimizationHelper.isBatteryOptimizationEnabled(context))
    }

    val state by MeshStatusRepository.orchestratorState
        .collectAsStateWithLifecycle()

    val isBluetoothEnabled = state != BleState.Disabled

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isBatteryOptimizationOn =
                    BatteryOptimizationHelper.isBatteryOptimizationEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isBatteryOptimizationOn) {
                    BatteryOptimizationBanner(
                        modifier = Modifier.padding(8.dp),
                        onDisableClick = {
                            BatteryOptimizationHelper.openBatteryOptimizationSettings(context)
                        }
                    )
                }
                if (!isBluetoothEnabled) {
                    BluetoothDisabledBanner(
                        modifier = Modifier.padding(8.dp),
                        onEnableClick = {
                            if (ActivityCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.BLUETOOTH_CONNECT
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                return@BluetoothDisabledBanner
                            }
                            BluetoothHelper.requestEnableBluetooth(context)
                        }
                    )
                }
                when (currentDestination) {
                    AppDestinations.HOME -> HomeScreen()
                    AppDestinations.PROFILE -> ProfileScreen()
                    AppDestinations.DEBUG -> {
                        DebugScreen(
                            onUpdateAlert = { alert ->
                                AlertStateRepository.update(
                                    MeshMeta(
                                        receivedAt = System.currentTimeMillis(),
                                        receivedFrom = "Debug",
                                        packet = MeshPacket(
                                            hopCount = 0,
                                            data = alert
                                        )
                                    )
                                )
                            }
                        )
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
    PROFILE("Profile", R.drawable.ic_account_box),
    DEBUG("Debug", R.drawable.ic_debug),
}