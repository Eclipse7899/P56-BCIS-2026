package com.p56.safereach

import android.app.Application
import com.p56.safereach.service.AlertSyncManager
import com.p56.safereach.ui.home.MapCameraPreferences
import com.p56.safereach.ui.profile.NotificationPreferences
import com.p56.safereach.ui.theme.ThemePreferences
import com.p56.safereach.util.ApplicationScope
import dagger.hilt.android.HiltAndroidApp
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class SafeReachApplication : Application() {

    @Inject
    lateinit var alertSyncManager: AlertSyncManager

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        ThemePreferences.initialize(this)
        NotificationPreferences.initialize(this)
        MapCameraPreferences.initialize(this)
        applicationScope.launch { alertSyncManager.sync() }
    }
}