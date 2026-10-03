package com.p56.safereach.ui.profile

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val Context.notificationDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "notification_preferences"
)

object NotificationPreferences {

    private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var dataStore: DataStore<Preferences>

    private val _notificationsEnabled = MutableStateFlow(true)

    val notificationsEnabled = _notificationsEnabled.asStateFlow()

    fun initialize(context: Context) {
        if (::dataStore.isInitialized) return
        dataStore = context.applicationContext.notificationDataStore
        val preferences = runCatching {
            runBlocking { dataStore.data.first() }
        }.getOrNull() ?: return
        _notificationsEnabled.value =
            preferences[KEY_NOTIFICATIONS_ENABLED] ?: true
    }

    fun areNotificationsEnabled(): Boolean = _notificationsEnabled.value

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        if (::dataStore.isInitialized) {
            scope.launch {
                dataStore.edit { preferences ->
                    preferences[KEY_NOTIFICATIONS_ENABLED] = enabled
                }
            }
        }
    }
}
