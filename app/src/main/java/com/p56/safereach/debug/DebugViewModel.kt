package com.p56.safereach.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.p56.safereach.AlertContext
import com.p56.safereach.AlertMeta
import com.p56.safereach.mesh.MeshStatusRepository
import com.p56.safereach.mesh.ble.advertisement.fsm.BleAdvState
import com.p56.safereach.mesh.ble.fsm.BleState
import com.p56.safereach.mesh.ble.scanning.fsm.BleScanState
import com.p56.safereach.proto.Alert
import com.p56.safereach.service.AlertService
import com.p56.safereach.verification.SigningManager
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DebugUiState(
    val alertCtx: List<AlertContext> = listOf(),
    val isMeshRunning: Boolean = false,
    val orchestratorState: BleState = BleState.Off,
    val advertiserState: BleAdvState = BleAdvState.Idle,
    val scannerState: BleScanState = BleScanState.Idle,
)

@HiltViewModel
class DebugViewModel @Inject constructor(
    private val signingManager: SigningManager,
    private val alertService: AlertService,
    meshStatusRepository: MeshStatusRepository,
) : ViewModel() {
    val uiState: StateFlow<DebugUiState> = combine(
        alertService.alerts,
        meshStatusRepository.isServiceRunning,
        meshStatusRepository.orchestratorState,
        meshStatusRepository.advertiserState,
        meshStatusRepository.scannerState,
    ) { alerts, isMeshRunning, orchestratorState, advertiserState, scannerState ->
        DebugUiState(
            alertCtx = alerts.values.toList().map { it },
            isMeshRunning = isMeshRunning,
            orchestratorState = orchestratorState,
            advertiserState = advertiserState,
            scannerState = scannerState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DebugUiState(),
    )

    fun updateAlert(alert: Alert) {
        val signed = signingManager.sign(alert.toByteArray())
        val ctx = AlertContext(
            alert = alert,
            signedMessage = signed,
            meta = AlertMeta(System.currentTimeMillis(), null)
        )
        alertService.add(ctx)
    }

    fun invalidateAlert(id: String) {
        alertService.remove(id)
    }
}
