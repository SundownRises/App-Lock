package com.anika.applock.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.anika.applock.domain.NotificationPrivacy
import com.anika.applock.platform.DataStoreAppLockRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Notification listener service for per-app notification privacy.
 *
 * Intercepts notifications from protected apps and either:
 * - Replaces them with a redacted placeholder (HIDE_CONTENT)
 * - Suppresses them entirely (HIDE_COMPLETELY)
 * - Lets them through unchanged (SHOW_NORMALLY)
 */
class NotificationPrivacyService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var repository: DataStoreAppLockRepository
    private lateinit var notificationManager: NotificationManager

    companion object {
        private const val REDACTED_CHANNEL_ID = "applock_redacted_notifications"
        private const val REDACTED_CHANNEL_NAME = "Hidden Notifications"
    }

    override fun onCreate() {
        super.onCreate()
        repository = DataStoreAppLockRepository(applicationContext)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createRedactedChannel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName

        scope.launch {
            val privacy = repository.getNotificationPrivacy(packageName)

            when (privacy) {
                NotificationPrivacy.SHOW_NORMALLY -> return@launch  // do nothing

                NotificationPrivacy.HIDE_CONTENT -> {
                    // Skip ongoing notifications (music, timers) - always show them
                    if (sbn.notification.flags and Notification.FLAG_ONGOING_EVENT != 0) {
                        return@launch
                    }

                    cancelNotification(sbn.key)
                    postRedactedNotification(sbn)
                }

                NotificationPrivacy.HIDE_COMPLETELY -> {
                    // Suppress entirely
                    cancelNotification(sbn.key)
                }
            }
        }
    }

    private fun postRedactedNotification(original: StatusBarNotification) {
        try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(original.packageName, 0)
            val appName = pm.getApplicationLabel(appInfo).toString()

            val redacted = NotificationCompat.Builder(this, REDACTED_CHANNEL_ID)
                .setSmallIcon(original.notification.smallIcon ?: android.R.drawable.ic_dialog_info)
                .setContentTitle("New notification")
                .setContentText("You have a new notification from $appName")
                .setShowWhen(true)
                .setWhen(original.postTime)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(original.notification.contentIntent)  // tap still opens the app
                .setAutoCancel(true)
                .build()

            notificationManager.notify(original.id, redacted)
        } catch (e: PackageManager.NameNotFoundException) {
            // App uninstalled or package name invalid, skip
        }
    }

    private fun createRedactedChannel() {
        val channel = NotificationChannel(
            REDACTED_CHANNEL_ID,
            REDACTED_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Placeholder notifications for hidden app notifications"
            setSound(null, null)  // no sound
            enableVibration(false)  // no vibration
            enableLights(false)  // no LED
        }
        notificationManager.createNotificationChannel(channel)
    }
}
