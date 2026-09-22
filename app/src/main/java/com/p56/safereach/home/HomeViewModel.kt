package com.p56.safereach.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.p56.safereach.debug.DebugUiState
import com.p56.safereach.service.AlertService
import com.p56.safereach.proto.Alert
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HomeViewModel @Inject constructor(
    alertService: AlertService,
) : ViewModel() {

    val activeAlerts: StateFlow<List<Alert>> = alertService.activeAlerts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = listOf(),
    )
}
