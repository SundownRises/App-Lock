package com.anika.applock.domain

import kotlinx.coroutines.flow.Flow
import java.time.Duration

/**
 * Manages the set of protected apps and their notification privacy settings.
 *
 * Pure domain interface with no Android dependencies.
 */
interface AppLockRepository {
    /**
     * Checks if a package is protected.
     */
    suspend fun isProtected(packageName: String): Boolean

    /**
     * Gets all protected package names.
     */
    fun getProtectedApps(): Flow<Set<String>>

    /**
     * Adds a package to the protected set.
     */
    suspend fun protect(packageName: String)

    /**
     * Removes a package from the protected set.
     */
    suspend fun unprotect(packageName: String)

    /**
     * Gets the notification privacy mode for a package.
     * Returns SHOW_NORMALLY if not explicitly set.
     */
    suspend fun getNotificationPrivacy(packageName: String): NotificationPrivacy

    /**
     * Sets the notification privacy mode for a package.
     */
    suspend fun setNotificationPrivacy(packageName: String, privacy: NotificationPrivacy)

    /**
     * Gets the idle timeout duration.
     */
    suspend fun getIdleTimeout(): Duration

    /**
     * Sets the idle timeout duration.
     */
    suspend fun setIdleTimeout(timeout: Duration)

    /**
     * Gets the current stealth disguise.
     */
    suspend fun getCurrentDisguise(): DisguiseOption

    /**
     * Sets the current stealth disguise.
     */
    suspend fun setCurrentDisguise(disguise: DisguiseOption)
}

/**
 * Stealth mode disguise options.
 */
enum class DisguiseOption(val aliasName: String, val displayName: String) {
    APP_LOCK("AppLockAlias", "App Lock"),
    WEATHER("WeatherAlias", "Weather"),
    CALCULATOR("CalculatorAlias", "Calculator")
}
