package com.p56.safereach.service

import com.p56.safereach.mesh.ble.features.BleFeature
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvertisingState
import com.p56.safereach.mesh.ble.fsm.AggregateBleState
import com.p56.safereach.mesh.ble.fsm.BluetoothState
import com.p56.safereach.mesh.ble.scanning.fsm.BleScanState
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class MeshStatusStore @Inject constructor() {
    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _bleOrchestratorState = MutableStateFlow<AggregateBleState>(AggregateBleState.Off)
    val bleOrchestratorState: StateFlow<AggregateBleState> = _bleOrchestratorState.asStateFlow()

    private val _bluetoothState: MutableStateFlow<BluetoothState> = MutableStateFlow(BluetoothState.Disabled)

    val bluetoothState: StateFlow<BluetoothState> = _bluetoothState.asStateFlow()

    private val _bluetoothIncompatibilities = MutableStateFlow<List<BleFeature>>(emptyList())
    val bluetoothIncompatibilities: StateFlow<List<BleFeature>> =
        _bluetoothIncompatibilities.asStateFlow()

    private val _advertiserState = MutableStateFlow<BleAdvertisingState>(BleAdvertisingState.Idle)
    val advertiserState: StateFlow<BleAdvertisingState> = _advertiserState.asStateFlow()

    private val _scannerState = MutableStateFlow<BleScanState>(BleScanState.Idle)
    val scannerState: StateFlow<BleScanState> = _scannerState.asStateFlow()

    fun updateServiceRunning(running: Boolean) {
        _isServiceRunning.value = running
    }

    fun updateOrchestratorState(state: AggregateBleState) {
        _bleOrchestratorState.value = state
    }

    fun updateBluetoothState(state: BluetoothState) {
        _bluetoothState.value = state
    }

    fun updateBluetoothIncompatibilities(features: List<BleFeature>) {
        _bluetoothIncompatibilities.value = features
    }

    fun updateAdvertiserState(state: BleAdvertisingState) {
        _advertiserState.value = state
    }

    fun updateScannerState(state: BleScanState) {
        _scannerState.value = state
    }
}