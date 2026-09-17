package com.anika.applock.platform

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppInfo(
    val packageName: String,
    val name: String,
    val icon: Drawable
)

/**
 * Provides list of installed user apps.
 */
class InstalledAppsProvider(private val context: Context) {

    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        // Always include Settings and Play Store
        val essentialPackages = setOf(
            "com.android.settings",
            "com.android.vending"
        )

        apps.filter { appInfo ->
            // User apps or essential system apps
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0) ||
                    appInfo.packageName in essentialPackages
        }
            .filter { it.packageName != context.packageName }  // Exclude ourselves
            .map { appInfo ->
                AppInfo(
                    packageName = appInfo.packageName,
                    name = pm.getApplicationLabel(appInfo).toString(),
                    icon = pm.getApplicationIcon(appInfo)
                )
            }
            .sortedBy { it.name.lowercase() }
    }
}
