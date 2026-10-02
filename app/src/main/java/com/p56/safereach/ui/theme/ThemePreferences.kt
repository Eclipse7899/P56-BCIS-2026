package com.p56.safereach.ui.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ThemePreferences {

    private val _darkModeEnabled = MutableStateFlow(false)

    val darkModeEnabled = _darkModeEnabled.asStateFlow()

    fun setDarkMode(enabled: Boolean) {
        _darkModeEnabled.value = enabled
    }
}