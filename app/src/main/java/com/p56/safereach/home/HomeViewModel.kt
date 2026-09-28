package com.p56.safereach.home

import androidx.lifecycle.ViewModel
import com.p56.safereach.model.Alert
import com.p56.safereach.service.AlertRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class HomeViewModel @Inject constructor(
    alertRepository: AlertRepository,
) : ViewModel() {

    val activeAlerts: StateFlow<List<Alert>> = alertRepository.activeAlerts
}
