package com.p56.safereach.ui.alert

import androidx.lifecycle.ViewModel
import com.p56.safereach.model.Alert
import com.p56.safereach.service.AlertRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class AlertHistoryViewModel @Inject constructor(
    alertRepository: AlertRepository
) : ViewModel() {

    val alerts: StateFlow<List<Alert>> = alertRepository.allAlerts
}