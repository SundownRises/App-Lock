package com.anika.applock.domain

import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Manages PIN hashing, verification, and rate limiting.
 *
 * The PIN is never stored. Instead we store:
 * - PBKDF2-HMAC-SHA256 hash (210k iterations)
 * - Random 16-byte salt (generated once at setup)
 * - Failed attempt count + cooldown deadline
 *
 * Rate limiting: 5 failed attempts → 30s cooldown, doubling each time.
 * The cooldown deadline is persisted so force-stopping the app doesn't reset it.
 *
 * Pure domain logic with no Android dependencies for testability.
 */
class PinRepository(
    private val storage: PinStorage,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    companion object {
        private const val PBKDF2_ITERATIONS = 210_000
        private const val HASH_LENGTH_BITS = 256
        private const val SALT_LENGTH_BYTES = 16
        private const val MAX_ATTEMPTS_BEFORE_COOLDOWN = 5
        private val BASE_COOLDOWN = Duration.ofSeconds(30)
    }

    /**
     * Sets the PIN for the first time (during onboarding).
     * Generates a random salt and stores the hash.
     */
    suspend fun setPin(pin: String) {
        require(pin.length in 4..6 && pin.all { it.isDigit() }) {
            "PIN must be 4-6 digits"
        }

        val salt = generateSalt()
        val hash = hashPin(pin, salt)

        storage.savePin(hash, salt)
        storage.clearFailedAttempts()
    }

    /**
     * Verifies the entered PIN.
     * Returns a sealed result indicating success, wrong PIN, or rate-limited.
     */
    suspend fun verify(enteredPin: String): VerifyResult {
        // Check cooldown first
        val cooldownDeadline = storage.getCooldownDeadline()
        if (cooldownDeadline != null && clock.instant() < cooldownDeadline) {
            val remaining = Duration.between(clock.instant(), cooldownDeadline)
            return VerifyResult.RateLimited(remaining)
        }

        val storedHash = storage.getHash() ?: return VerifyResult.NotSet
        val salt = storage.getSalt() ?: return VerifyResult.NotSet

        val enteredHash = hashPin(enteredPin, salt)

        return if (enteredHash.contentEquals(storedHash)) {
            storage.clearFailedAttempts()
            VerifyResult.Success
        } else {
            val failCount = storage.incrementFailedAttempts()

            if (failCount >= MAX_ATTEMPTS_BEFORE_COOLDOWN) {
                val cooldown = calculateCooldown(failCount)
                val deadline = clock.instant().plus(cooldown)
                storage.setCooldownDeadline(deadline)
                return VerifyResult.RateLimited(cooldown)
            }

            VerifyResult.Wrong(
                attemptsRemaining = MAX_ATTEMPTS_BEFORE_COOLDOWN - failCount
            )
        }
    }

    /**
     * Verifies the recovery code (separate from PIN, used when locked out).
     */
    suspend fun verifyRecoveryCode(code: String): Boolean {
        val stored = storage.getRecoveryCode() ?: return false
        return code == stored
    }

    /**
     * Generates and stores a recovery code during setup.
     * Returns the code to display to the user (shown once).
     */
    suspend fun generateRecoveryCode(): String {
        val code = generateReadableCode(length = 12)
        storage.saveRecoveryCode(code)
        return code
    }

    /**
     * Checks if a PIN has been set.
     */
    suspend fun isPinSet(): Boolean {
        return storage.getHash() != null
    }

    /**
     * Hashes a PIN with PBKDF2-HMAC-SHA256.
     */
    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(
            pin.toCharArray(),
            salt,
            PBKDF2_ITERATIONS,
            HASH_LENGTH_BITS
        )
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun generateSalt(): ByteArray {
        return ByteArray(SALT_LENGTH_BYTES).apply {
            SecureRandom().nextBytes(this)
        }
    }

    /**
     * Exponential backoff: 30s, 60s, 120s, 240s, ...
     */
    private fun calculateCooldown(failCount: Int): Duration {
        val multiplier = 1 shl (failCount - MAX_ATTEMPTS_BEFORE_COOLDOWN)
        return BASE_COOLDOWN.multipliedBy(multiplier.toLong())
    }

    /**
     * Generates a human-readable recovery code (alphanumeric, no ambiguous chars).
     */
    private fun generateReadableCode(length: Int): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // no I, O, 0, 1
        return (1..length)
            .map { chars.random() }
            .joinToString("")
            .chunked(4)
            .joinToString("-")
    }

    sealed interface VerifyResult {
        data object Success : VerifyResult
        data object NotSet : VerifyResult
        data class Wrong(val attemptsRemaining: Int) : VerifyResult
        data class RateLimited(val remaining: Duration) : VerifyResult
    }

    /**
     * Storage interface - implemented by DataStore wrapper in platform layer.
     */
    interface PinStorage {
        suspend fun savePin(hash: ByteArray, salt: ByteArray)
        suspend fun getHash(): ByteArray?
        suspend fun getSalt(): ByteArray?
        suspend fun saveRecoveryCode(code: String)
        suspend fun getRecoveryCode(): String?
        suspend fun incrementFailedAttempts(): Int
        suspend fun clearFailedAttempts()
        suspend fun getCooldownDeadline(): Instant?
        suspend fun setCooldownDeadline(deadline: Instant)
    }
}
