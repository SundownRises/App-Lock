package com.anika.applock.platform

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.anika.applock.admin.LockDeviceAdminReceiver
import com.anika.applock.service.AppWatchAccessibilityService
import com.anika.applock.service.NotificationPrivacyService

/**
 * Checks and provides intents for all required permissions.
 */
class PermissionChecker(private val context: Context) {

    /**
     * Checks if the Accessibility Service is enabled.
     */
    fun isAccessibilityEnabled(): Boolean {
        val service = "${context.packageName}/${AppWatchAccessibilityService::class.java.name}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains(service)
    }

    /**
     * Intent to open accessibility settings.
     */
    fun getAccessibilityIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }

    /**
     * Checks if the Notification Listener Service is enabled.
     */
    fun isNotificationListenerEnabled(): Boolean {
        val listeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        return listeners.contains(context.packageName)
    }

    /**
     * Intent to open notification listener settings.
     */
    fun getNotificationListenerIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        } else {
            Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
        }
    }

    /**
     * Checks if camera permission is granted.
     */
    fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if battery optimization is disabled (app is whitelisted).
     */
    fun isBatteryOptimizationDisabled(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Intent to request battery optimization exemption.
     */
    fun getBatteryOptimizationIntent(): Intent {
        return Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    }

    /**
     * Checks if Device Admin is active.
     */
    fun isDeviceAdminActive(): Boolean {
        val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(context, LockDeviceAdminReceiver::class.java)
        return devicePolicyManager.isAdminActive(adminComponent)
    }

    /**
     * Intent to activate Device Admin.
     */
    fun getDeviceAdminIntent(): Intent {
        val adminComponent = ComponentName(context, LockDeviceAdminReceiver::class.java)
        return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "App Lock needs Device Admin to prevent uninstallation. This protects your locked apps from being bypassed."
            )
        }
    }

    /**
     * Intent to app info page (for "Allow restricted settings" on Android 13+).
     */
    fun getAppInfoIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.parse("package:${context.packageName}")
        }
    }

    /**
     * Checks all permissions at once.
     */
    fun getAllPermissionStates(): PermissionStates {
        return PermissionStates(
            accessibility = isAccessibilityEnabled(),
            notificationListener = isNotificationListenerEnabled(),
            camera = isCameraPermissionGranted(),
            batteryOptimization = isBatteryOptimizationDisabled(),
            deviceAdmin = isDeviceAdminActive()
        )
    }
}

data class PermissionStates(
    val accessibility: Boolean,
    val notificationListener: Boolean,
    val camera: Boolean,
    val batteryOptimization: Boolean,
    val deviceAdmin: Boolean
) {
    val allGranted: Boolean
        get() = accessibility && notificationListener && camera && batteryOptimization && deviceAdmin

    val essentialGranted: Boolean
        get() = accessibility && batteryOptimization && deviceAdmin
}
