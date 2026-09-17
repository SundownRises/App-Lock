package com.anika.applock.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Foreground service that keeps the app process alive.
 *
 * This prevents the system from killing the accessibility service.
 * Battery impact is minimal - it's a persistent notification with no background work.
 */
class LockGuardService : Service() {

    companion object {
        private const val GUARD_CHANNEL_ID = "applock_guard_service"
        private const val GUARD_CHANNEL_NAME = "App Lock Protection"
        private const val GUARD_NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, GUARD_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)  // TODO: use actual lock icon
            .setContentTitle("App Lock active")
            .setContentText("Protecting your apps")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setShowWhen(false)
            .build()

        startForeground(
            GUARD_NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )

        return START_STICKY  // auto-restart if killed
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            GUARD_CHANNEL_ID,
            GUARD_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps App Lock running in the background"
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
