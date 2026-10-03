package com.p56.safereach.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.p56.safereach.Constants.APP_DISPLAY_NAME
import com.p56.safereach.MainActivity
import com.p56.safereach.R
import com.p56.safereach.model.Alert
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import com.p56.safereach.ui.profile.NotificationPreferences

@Singleton
class AlertNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    fun notify(alert: Alert) {
        if (!NotificationPreferences.areNotificationsEnabled(context)) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(manager)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle(alert.title.ifBlank { "${alert.type.displayName} alert" })
            .setContentText("${alert.severity.displayName} severity · ${alert.type.displayName}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(alert.description))
            .setContentIntent(contentIntent(alert))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        manager.notify(alert.id.hashCode(), notification)
    }

    private fun contentIntent(alert: Alert): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(
            context,
            alert.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (manager.getNotificationChannel(CHANNEL_ID) != null) {
            return
        }
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alerts",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "$APP_DISPLAY_NAME emergency alerts"
        }
        manager.createNotificationChannel(channel)
    }

    private companion object {
        private const val CHANNEL_ID = "alerts"
    }
}
