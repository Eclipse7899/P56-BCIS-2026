package com.p56.safereach.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

enum class DarkMode {
    LIGHT,
    DARK,
    SYSTEM
}

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "theme_preferences"
)

object ThemePreferences {

    private val KEY_DARK_MODE = stringPreferencesKey("dark_mode")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var dataStore: DataStore<Preferences>

    private val _darkMode = MutableStateFlow(DarkMode.SYSTEM)

    val darkMode = _darkMode.asStateFlow()

    fun initialize(context: Context) {
        if (::dataStore.isInitialized) return
        dataStore = context.applicationContext.themeDataStore
        val preferences = runCatching {
            runBlocking { dataStore.data.first() }
        }.getOrNull() ?: return
        val stored = preferences[KEY_DARK_MODE] ?: return
        DarkMode.entries.firstOrNull { it.name == stored }?.let { _darkMode.value = it }
    }

    fun setDarkMode(mode: DarkMode) {
        _darkMode.value = mode
        if (::dataStore.isInitialized) {
            scope.launch {
                dataStore.edit { preferences ->
                    preferences[KEY_DARK_MODE] = mode.name
                }
            }
        }
    }
}
