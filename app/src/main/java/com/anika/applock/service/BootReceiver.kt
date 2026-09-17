package com.anika.applock.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-arms the lock guard service after device reboot.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Start the foreground guard service
            val serviceIntent = Intent(context, LockGuardService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}
