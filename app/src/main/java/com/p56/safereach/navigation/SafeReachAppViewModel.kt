package com.p56.safereach.navigation

import androidx.lifecycle.ViewModel
import com.p56.safereach.mesh.MeshStatusRepository
import com.p56.safereach.mesh.ble.fsm.BleState
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class SafeReachAppViewModel @Inject constructor(
    meshStatusRepository: MeshStatusRepository,
) : ViewModel() {

    val orchestratorState: StateFlow<BleState> = meshStatusRepository.orchestratorState
}
