package com.p56.safereach.ui.profile

import android.content.Context

object NotificationPreferences {

    private const val PREFS_NAME = "safereach_preferences"
    private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"

    fun areNotificationsEnabled(context: Context): Boolean {
        val preferences = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        return preferences.getBoolean(
            KEY_NOTIFICATIONS_ENABLED,
            true
        )
    }

    fun setNotificationsEnabled(
        context: Context,
        enabled: Boolean
    ) {
        val preferences = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
        preferences.edit()
            .putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled)
            .apply()
    }
}