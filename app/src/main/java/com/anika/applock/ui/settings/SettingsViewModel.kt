package com.anika.applock.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anika.applock.domain.AppLockRepository
import com.anika.applock.domain.DisguiseOption
import com.anika.applock.domain.PinRepository
import com.anika.applock.platform.DataStoreAppLockRepository
import com.anika.applock.platform.DataStorePinStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration

data class SettingsState(
    val idleTimeout: Duration = Duration.ofMinutes(5),
    val recoveryCode: String? = null,
    val currentDisguise: DisguiseOption = DisguiseOption.APP_LOCK
)

class SettingsViewModel(private val context: Context) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private val repository: AppLockRepository = DataStoreAppLockRepository(context)
    private val pinRepository = PinRepository(DataStorePinStorage(context))

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val timeout = repository.getIdleTimeout()
            val disguise = repository.getCurrentDisguise()

            _state.value = _state.value.copy(
                idleTimeout = timeout,
                currentDisguise = disguise
            )
        }
    }

    fun setIdleTimeout(timeout: Duration) {
        viewModelScope.launch {
            repository.setIdleTimeout(timeout)
            _state.value = _state.value.copy(idleTimeout = timeout)
        }
    }

    fun revealRecoveryCode() {
        viewModelScope.launch {
            // Access storage through the public property
            val storedCode = pinRepository.storage.getRecoveryCode()
            _state.value = _state.value.copy(recoveryCode = storedCode)
        }
    }

    fun navigateToStealthMode() {
        // Navigation handled by MainActivity - trigger via event
    }
}
