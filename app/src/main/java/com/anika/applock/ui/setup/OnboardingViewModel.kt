package com.anika.applock.ui.setup

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anika.applock.domain.PinRepository
import com.anika.applock.platform.DataStorePinStorage
import com.anika.applock.platform.PermissionChecker
import com.anika.applock.platform.PermissionStates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class OnboardingStep {
    WELCOME,
    PIN_SETUP,
    RECOVERY_CODE,
    RESTRICTED_SETTINGS,
    ACCESSIBILITY,
    NOTIFICATION_LISTENER,
    CAMERA,
    BATTERY_OPTIMIZATION,
    DEVICE_ADMIN,
    COMPLETE
}

data class OnboardingState(
    val currentStep: OnboardingStep = OnboardingStep.WELCOME,
    val permissions: PermissionStates = PermissionStates(
        accessibility = false,
        notificationListener = false,
        camera = false,
        batteryOptimization = false,
        deviceAdmin = false
    ),
    val recoveryCode: String? = null,
    val cameraPermissionRequested: Boolean = false
) {
    val currentStepNumber: Int
        get() = OnboardingStep.entries.indexOf(currentStep) + 1

    val totalSteps: Int = OnboardingStep.entries.size

    val progress: Float
        get() = currentStepNumber.toFloat() / totalSteps.toFloat()
}

class OnboardingViewModel(private val context: Context) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    private val pinRepository = PinRepository(DataStorePinStorage(context))
    private val permissionChecker = PermissionChecker(context)

    init {
        refreshPermissions()
    }

    fun setPin(pin: String) {
        viewModelScope.launch {
            pinRepository.setPin(pin)
            val recoveryCode = pinRepository.generateRecoveryCode()
            _state.value = _state.value.copy(recoveryCode = recoveryCode)
        }
    }

    fun nextStep() {
        val current = _state.value.currentStep
        val next = when (current) {
            OnboardingStep.WELCOME -> OnboardingStep.PIN_SETUP
            OnboardingStep.PIN_SETUP -> OnboardingStep.RECOVERY_CODE
            OnboardingStep.RECOVERY_CODE -> OnboardingStep.RESTRICTED_SETTINGS
            OnboardingStep.RESTRICTED_SETTINGS -> OnboardingStep.ACCESSIBILITY
            OnboardingStep.ACCESSIBILITY -> OnboardingStep.NOTIFICATION_LISTENER
            OnboardingStep.NOTIFICATION_LISTENER -> OnboardingStep.CAMERA
            OnboardingStep.CAMERA -> OnboardingStep.BATTERY_OPTIMIZATION
            OnboardingStep.BATTERY_OPTIMIZATION -> OnboardingStep.DEVICE_ADMIN
            OnboardingStep.DEVICE_ADMIN -> OnboardingStep.COMPLETE
            OnboardingStep.COMPLETE -> OnboardingStep.COMPLETE
        }
        _state.value = _state.value.copy(currentStep = next)
        refreshPermissions()
    }

    fun skipRestrictedSettings() {
        // Android 13+ only step, skip if not applicable
        nextStep()
    }

    fun skipNotificationListener() {
        nextStep()
    }

    fun skipCamera() {
        nextStep()
    }

    fun requestCamera() {
        _state.value = _state.value.copy(cameraPermissionRequested = true)
        // Actual permission request happens in the composable via ActivityResultLauncher
    }

    fun refreshPermissions() {
        val permissions = permissionChecker.getAllPermissionStates()
        _state.value = _state.value.copy(permissions = permissions)
    }
}
