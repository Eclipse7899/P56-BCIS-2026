package com.p56.safereach.navigation

import androidx.lifecycle.ViewModel
import com.p56.safereach.mesh.ble.fsm.BleState
import com.p56.safereach.service.MeshStatusStore
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class SafeReachAppViewModel @Inject constructor(
    meshStatusStore: MeshStatusStore,
) : ViewModel() {

    val orchestratorState: StateFlow<BleState> = meshStatusStore.orchestratorState
}
