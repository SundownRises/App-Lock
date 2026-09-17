package com.anika.applock.platform

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.anika.applock.domain.PinRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant

private val Context.pinDataStore: DataStore<Preferences> by preferencesDataStore(name = "pin_storage")

/**
 * DataStore-backed implementation of PinStorage.
 */
class DataStorePinStorage(private val context: Context) : PinRepository.PinStorage {

    private val hashKey = byteArrayPreferencesKey("pin_hash")
    private val saltKey = byteArrayPreferencesKey("pin_salt")
    private val recoveryCodeKey = stringPreferencesKey("recovery_code")
    private val failedAttemptsKey = intPreferencesKey("failed_attempts")
    private val cooldownDeadlineKey = longPreferencesKey("cooldown_deadline_epoch_millis")

    override suspend fun savePin(hash: ByteArray, salt: ByteArray) {
        context.pinDataStore.edit { prefs ->
            prefs[hashKey] = hash
            prefs[saltKey] = salt
        }
    }

    override suspend fun getHash(): ByteArray? {
        return context.pinDataStore.data.map { it[hashKey] }.first()
    }

    override suspend fun getSalt(): ByteArray? {
        return context.pinDataStore.data.map { it[saltKey] }.first()
    }

    override suspend fun saveRecoveryCode(code: String) {
        context.pinDataStore.edit { prefs ->
            prefs[recoveryCodeKey] = code
        }
    }

    override suspend fun getRecoveryCode(): String? {
        return context.pinDataStore.data.map { it[recoveryCodeKey] }.first()
    }

    override suspend fun incrementFailedAttempts(): Int {
        var newCount = 0
        context.pinDataStore.edit { prefs ->
            val current = prefs[failedAttemptsKey] ?: 0
            newCount = current + 1
            prefs[failedAttemptsKey] = newCount
        }
        return newCount
    }

    override suspend fun clearFailedAttempts() {
        context.pinDataStore.edit { prefs ->
            prefs.remove(failedAttemptsKey)
            prefs.remove(cooldownDeadlineKey)
        }
    }

    override suspend fun getCooldownDeadline(): Instant? {
        val millis = context.pinDataStore.data.map { it[cooldownDeadlineKey] }.first()
        return millis?.let { Instant.ofEpochMilli(it) }
    }

    override suspend fun setCooldownDeadline(deadline: Instant) {
        context.pinDataStore.edit { prefs ->
            prefs[cooldownDeadlineKey] = deadline.toEpochMilli()
        }
    }
}
