package com.anika.applock.platform

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.anika.applock.domain.AppLockRepository
import com.anika.applock.domain.DisguiseOption
import com.anika.applock.domain.NotificationPrivacy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Duration

private val Context.appLockDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_lock_storage")

/**
 * DataStore-backed implementation of AppLockRepository.
 */
class DataStoreAppLockRepository(private val context: Context) : AppLockRepository {

    private val protectedAppsKey = stringSetPreferencesKey("protected_apps")
    private val idleTimeoutMinutesKey = intPreferencesKey("idle_timeout_minutes")
    private val currentDisguiseKey = stringPreferencesKey("current_disguise")

    private fun notificationPrivacyKey(packageName: String) =
        stringPreferencesKey("notif_privacy_$packageName")

    override suspend fun isProtected(packageName: String): Boolean {
        val protected = context.appLockDataStore.data.map { it[protectedAppsKey] }.first()
        return protected?.contains(packageName) == true
    }

    override fun getProtectedApps(): Flow<Set<String>> {
        return context.appLockDataStore.data.map { prefs ->
            prefs[protectedAppsKey] ?: emptySet()
        }
    }

    override suspend fun protect(packageName: String) {
        context.appLockDataStore.edit { prefs ->
            val current = prefs[protectedAppsKey] ?: emptySet()
            prefs[protectedAppsKey] = current + packageName
        }
    }

    override suspend fun unprotect(packageName: String) {
        context.appLockDataStore.edit { prefs ->
            val current = prefs[protectedAppsKey] ?: emptySet()
            prefs[protectedAppsKey] = current - packageName
            // Also clear notification privacy setting
            prefs.remove(notificationPrivacyKey(packageName))
        }
    }

    override suspend fun getNotificationPrivacy(packageName: String): NotificationPrivacy {
        val value = context.appLockDataStore.data
            .map { it[notificationPrivacyKey(packageName)] }
            .first()
        return value?.let { NotificationPrivacy.valueOf(it) } ?: NotificationPrivacy.SHOW_NORMALLY
    }

    override suspend fun setNotificationPrivacy(packageName: String, privacy: NotificationPrivacy) {
        context.appLockDataStore.edit { prefs ->
            prefs[notificationPrivacyKey(packageName)] = privacy.name
        }
    }

    override suspend fun getIdleTimeout(): Duration {
        val minutes = context.appLockDataStore.data
            .map { it[idleTimeoutMinutesKey] }
            .first() ?: 5  // default 5 minutes
        return Duration.ofMinutes(minutes.toLong())
    }

    override suspend fun setIdleTimeout(timeout: Duration) {
        context.appLockDataStore.edit { prefs ->
            prefs[idleTimeoutMinutesKey] = timeout.toMinutes().toInt()
        }
    }

    override suspend fun getCurrentDisguise(): DisguiseOption {
        val value = context.appLockDataStore.data
            .map { it[currentDisguiseKey] }
            .first()
        return value?.let { name ->
            DisguiseOption.entries.find { it.name == name }
        } ?: DisguiseOption.APP_LOCK
    }

    override suspend fun setCurrentDisguise(disguise: DisguiseOption) {
        context.appLockDataStore.edit { prefs ->
            prefs[currentDisguiseKey] = disguise.name
        }
    }
}
