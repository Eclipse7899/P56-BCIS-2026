package com.p56.safereach.mesh

import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvState
import com.p56.safereach.mesh.ble.fsm.BleState
import com.p56.safereach.mesh.ble.scanning.fsm.BleScanState
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class MeshStatusRepository @Inject constructor() {
    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _orchestratorState = MutableStateFlow<BleState>(BleState.Off)
    val orchestratorState: StateFlow<BleState> = _orchestratorState.asStateFlow()

    private val _advertiserState = MutableStateFlow<BleAdvState>(BleAdvState.Idle)
    val advertiserState: StateFlow<BleAdvState> = _advertiserState.asStateFlow()

    private val _scannerState = MutableStateFlow<BleScanState>(BleScanState.Idle)
    val scannerState: StateFlow<BleScanState> = _scannerState.asStateFlow()

    fun updateServiceRunning(running: Boolean) {
        _isServiceRunning.value = running
    }

    fun updateOrchestratorState(state: BleState) {
        _orchestratorState.value = state
    }

    fun updateAdvertiserState(state: BleAdvState) {
        _advertiserState.value = state
    }

    fun updateScannerState(state: BleScanState) {
        _scannerState.value = state
    }
}
