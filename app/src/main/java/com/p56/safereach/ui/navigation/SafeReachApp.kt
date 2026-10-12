package com.p56.safereach.ui.navigation

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.p56.safereach.BuildConfig
import com.p56.safereach.R
import com.p56.safereach.android.permissions.AppSettingsHelper
import com.p56.safereach.android.permissions.BatteryOptimizationHelper
import com.p56.safereach.android.permissions.BluetoothHelper
import com.p56.safereach.android.MeshForegroundService
import com.p56.safereach.android.permissions.NotificationPermissionHelper
import com.p56.safereach.mesh.ble.features.BleFeature
import com.p56.safereach.android.BlePermissions
import com.p56.safereach.mesh.ble.fsm.BluetoothState
import com.p56.safereach.ui.banners.BatteryOptimizationBanner
import com.p56.safereach.ui.banners.BluetoothDisabledBanner
import com.p56.safereach.ui.banners.BluetoothIncompatibilityBanner
import com.p56.safereach.ui.banners.ConnectedDevicesPermissionBanner
import com.p56.safereach.ui.banners.LocationPermissionBanner
import com.p56.safereach.ui.banners.NotificationPermissionBanner
import com.p56.safereach.ui.alert.AlertHistoryRoute
import com.p56.safereach.ui.debug.DebugRoute
import com.p56.safereach.ui.debug.DebugViewModel
import com.p56.safereach.ui.home.HomeRoute
import com.p56.safereach.ui.profile.ProfileScreen
import com.p56.safereach.ui.theme.SafeReachTheme

@Composable
fun SafeReachApp() {
    val viewModel: SafeReachAppViewModel = hiltViewModel()
    val bluetoothState by viewModel.bluetoothState.collectAsStateWithLifecycle()
    val bluetoothIncompatibilities by viewModel.bluetoothIncompatibilities
        .collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var notificationsEnabled by remember {
        mutableStateOf(NotificationPermissionHelper.areNotificationsEnabled(context))
    }
    var connectedDevicePermissionGranted by remember {
        mutableStateOf(BlePermissions.hasConnectedDevicePermissions(context))
    }
    var locationPermissionGranted by remember {
        mutableStateOf(BlePermissions.hasLocationPermissions(context))
    }

    val connectedDevicePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        connectedDevicePermissionGranted = BlePermissions.hasConnectedDevicePermissions(context)
        startMeshServiceIfPermissionsGranted(context)
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        locationPermissionGranted = BlePermissions.hasLocationPermissions(context)
        startMeshServiceIfPermissionsGranted(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsEnabled =
                    NotificationPermissionHelper.areNotificationsEnabled(context)
                connectedDevicePermissionGranted =
                    BlePermissions.hasConnectedDevicePermissions(context)
                locationPermissionGranted =
                    BlePermissions.hasLocationPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    SafeReachAppContent(
        bluetoothState = bluetoothState,
        bluetoothIncompatibilities = bluetoothIncompatibilities,
        notificationsEnabled = notificationsEnabled,
        connectedDevicePermissionGranted = connectedDevicePermissionGranted,
        locationPermissionGranted = locationPermissionGranted,
        onEnableNotifications = {
            NotificationPermissionHelper.openNotificationSettings(context)
        },
        onGrantConnectedDevicePermission = {
            requestPermissionsOrOpenSettings(
                context = context,
                permissions = BlePermissions.missingConnectedDevicePermissions(context),
                onRequest = connectedDevicePermissionLauncher::launch
            )
        },
        onGrantLocationPermission = {
            requestPermissionsOrOpenSettings(
                context = context,
                permissions = BlePermissions.missingLocationPermissions(context),
                onRequest = locationPermissionLauncher::launch
            )
        },
    )
}

@Composable
fun SafeReachAppContent(
    bluetoothState: BluetoothState,
    bluetoothIncompatibilities: List<BleFeature> = emptyList(),
    notificationsEnabled: Boolean = true,
    connectedDevicePermissionGranted: Boolean = true,
    locationPermissionGranted: Boolean = true,
    onEnableNotifications: () -> Unit = {},
    onGrantConnectedDevicePermission: () -> Unit = {},
    onGrantLocationPermission: () -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isBatteryOptimizationOn by remember {
        mutableStateOf(BatteryOptimizationHelper.isBatteryOptimizationEnabled(context))
    }

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
                if (!BuildConfig.DEBUG && it == AppDestinations.DEBUG) {
                    return@forEach
                }
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
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxHeight * 0.5f)
                            .verticalScroll(scrollState)
                    ) {
                        if (!notificationsEnabled) {
                            NotificationPermissionBanner(
                                modifier = Modifier.padding(8.dp),
                                onEnableClick = onEnableNotifications
                            )
                        }
                        if (!connectedDevicePermissionGranted) {
                            ConnectedDevicesPermissionBanner(
                                modifier = Modifier.padding(8.dp),
                                onGrantClick = onGrantConnectedDevicePermission
                            )
                        }
                        if (!locationPermissionGranted) {
                            LocationPermissionBanner(
                                modifier = Modifier.padding(8.dp),
                                onGrantClick = onGrantLocationPermission
                            )
                        }
                        if (isBatteryOptimizationOn) {
                            BatteryOptimizationBanner(
                                modifier = Modifier.padding(8.dp),
                                onDisableClick = {
                                    BatteryOptimizationHelper.openBatteryOptimizationSettings(context)
                                }
                            )
                        }
                        if (bluetoothState == BluetoothState.Disabled) {
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
                        if (bluetoothState == BluetoothState.Enabled &&
                            bluetoothIncompatibilities.isNotEmpty()
                        ) {
                            BluetoothIncompatibilityBanner(
                                modifier = Modifier.padding(8.dp),
                                missingFeatures = bluetoothIncompatibilities
                            )
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    when (currentDestination) {
                        AppDestinations.HOME -> HomeRoute()
                        AppDestinations.ALERTS -> AlertHistoryRoute()
                        AppDestinations.PROFILE -> ProfileScreen()
                        AppDestinations.DEBUG -> {
                            val vm = hiltViewModel<DebugViewModel>()
                            DebugRoute(viewModel = vm)
                        }
                    }
                }
            }
        }
    }
}

private fun startMeshServiceIfPermissionsGranted(context: Context) {
    if (BlePermissions.hasBlePermissions(context)) {
        MeshForegroundService.start(context)
    }
}

private fun requestPermissionsOrOpenSettings(
    context: Context,
    permissions: List<String>,
    onRequest: (Array<String>) -> Unit,
) {
    if (permissions.isEmpty()) {
        return
    }
    val activity = context as? Activity
    val canRequest = activity != null &&
        permissions.any { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
    if (canRequest) {
        onRequest(permissions.toTypedArray())
    } else {
        AppSettingsHelper.openAppDetailsSettings(context)
    }
}

@PreviewScreenSizes
@Composable
private fun SafeReachAppPreview() {
    SafeReachTheme {
        SafeReachAppContent(bluetoothState = BluetoothState.Enabled)
    }
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    HOME("Home", R.drawable.ic_home),
    ALERTS("Alerts", R.drawable.ic_warning),
    PROFILE("Profile", R.drawable.ic_account_box),
    DEBUG("Debug", R.drawable.ic_debug),
}