package com.anika.applock.ui.lock

import android.content.Context
import android.content.pm.PackageManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anika.applock.camera.IntruderCameraCapture
import com.anika.applock.domain.LockSessionManager
import com.anika.applock.domain.PinRepository
import com.anika.applock.platform.DataStorePinStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration

/**
 * UI state for the lock screen.
 */
sealed interface LockUiState {
    data class Locked(
        val appName: String,
        val appIcon: android.graphics.drawable.Drawable?,
        val enteredDigits: Int,
        val pinLength: Int,
        val error: String? = null,
        val canUseBiometric: Boolean = false,
        val cooldownRemaining: Duration? = null
    ) : LockUiState

    data object Unlocked : LockUiState
}

/**
 * ViewModel for the lock screen.
 */
class LockViewModel(
    private val targetPackage: String,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<LockUiState>(
        LockUiState.Locked(
            appName = "Loading...",
            appIcon = null,
            enteredDigits = 0,
            pinLength = 6,
            canUseBiometric = false
        )
    )
    val uiState: StateFlow<LockUiState> = _uiState.asStateFlow()

    private val pinRepository = PinRepository(DataStorePinStorage(context))
    private val sessionManager = LockSessionManager()  // TODO: inject singleton
    private val cameraCapture = IntruderCameraCapture(context)

    private val enteredPin = StringBuilder()
    private var failCount = 0

    companion object {
        private const val INTRUDER_PHOTO_THRESHOLD = 3
        private const val PIN_LENGTH = 6
    }

    init {
        loadAppInfo()
        checkBiometricAvailability()
    }

    fun onDigit(digit: Int) {
        val currentState = _uiState.value as? LockUiState.Locked ?: return

        // Check cooldown
        if (currentState.cooldownRemaining != null && currentState.cooldownRemaining > Duration.ZERO) {
            return  // Still in cooldown
        }

        enteredPin.append(digit)

        _uiState.value = currentState.copy(
            enteredDigits = enteredPin.length,
            error = null
        )

        // Auto-verify when PIN length reached
        if (enteredPin.length == PIN_LENGTH) {
            verifyPin()
        }
    }

    fun onBackspace() {
        if (enteredPin.isNotEmpty()) {
            enteredPin.deleteCharAt(enteredPin.length - 1)
            val currentState = _uiState.value as? LockUiState.Locked ?: return
            _uiState.value = currentState.copy(
                enteredDigits = enteredPin.length,
                error = null
            )
        }
    }

    fun onBiometric() {
        // Biometric authentication will be triggered from the UI (FragmentActivity needed)
        // This is a placeholder - actual implementation needs BiometricPrompt
    }

    private fun verifyPin() {
        viewModelScope.launch {
            when (val result = pinRepository.verify(enteredPin.toString())) {
                is PinRepository.VerifyResult.Success -> {
                    // Unlock successful
                    sessionManager.markUnlocked(targetPackage)
                    _uiState.value = LockUiState.Unlocked
                }

                is PinRepository.VerifyResult.Wrong -> {
                    failCount++
                    enteredPin.clear()

                    // Capture intruder selfie after threshold
                    if (failCount >= INTRUDER_PHOTO_THRESHOLD) {
                        cameraCapture.captureAsync(targetPackage)
                    }

                    val currentState = _uiState.value as? LockUiState.Locked ?: return@launch
                    _uiState.value = currentState.copy(
                        enteredDigits = 0,
                        error = "Wrong PIN. ${result.attemptsRemaining} attempts remaining."
                    )
                }

                is PinRepository.VerifyResult.RateLimited -> {
                    enteredPin.clear()
                    val currentState = _uiState.value as? LockUiState.Locked ?: return@launch
                    _uiState.value = currentState.copy(
                        enteredDigits = 0,
                        error = "Too many attempts. Try again in ${result.remaining.seconds}s.",
                        cooldownRemaining = result.remaining
                    )
                }

                PinRepository.VerifyResult.NotSet -> {
                    // PIN not set - should not happen in normal flow
                    enteredPin.clear()
                }
            }
        }
    }

    private fun loadAppInfo() {
        try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(targetPackage, 0)
            val appName = pm.getApplicationLabel(appInfo).toString()
            val appIcon = pm.getApplicationIcon(appInfo)

            val currentState = _uiState.value as? LockUiState.Locked ?: return
            _uiState.value = currentState.copy(
                appName = appName,
                appIcon = appIcon
            )
        } catch (e: PackageManager.NameNotFoundException) {
            val currentState = _uiState.value as? LockUiState.Locked ?: return
            _uiState.value = currentState.copy(appName = "Unknown App")
        }
    }

    private fun checkBiometricAvailability() {
        val biometricManager = BiometricManager.from(context)
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.BIOMETRIC_STRONG
        )

        val available = canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS

        val currentState = _uiState.value as? LockUiState.Locked ?: return
        _uiState.value = currentState.copy(canUseBiometric = available)
    }
}
