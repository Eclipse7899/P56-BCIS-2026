package com.p56.safereach

import android.app.Application
import com.p56.safereach.ui.home.MapCameraPreferences
import com.p56.safereach.ui.profile.NotificationPreferences
import com.p56.safereach.ui.theme.ThemePreferences
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SafeReachApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemePreferences.initialize(this)
        NotificationPreferences.initialize(this)
        MapCameraPreferences.initialize(this)
    }
}