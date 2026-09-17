package com.anika.applock.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/**
 * Device Admin receiver to block uninstallation.
 *
 * When active, Android prevents uninstalling the app unless the user first
 * deactivates device admin in Settings. Since Settings is locked by the app,
 * this creates the anti-tamper loop.
 */
class LockDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        // Device admin enabled
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        // Device admin disabled - user is probably about to uninstall
    }
}
