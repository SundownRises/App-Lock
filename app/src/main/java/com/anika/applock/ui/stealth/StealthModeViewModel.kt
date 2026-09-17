package com.anika.applock.ui.stealth

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anika.applock.domain.AppLockRepository
import com.anika.applock.domain.DisguiseOption
import com.anika.applock.platform.DataStoreAppLockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StealthModeState(
    val currentDisguise: DisguiseOption = DisguiseOption.APP_LOCK,
    val selectedDisguise: DisguiseOption = DisguiseOption.APP_LOCK,
    val isApplying: Boolean = false
)

class StealthModeViewModel(private val context: Context) : ViewModel() {

    private val _state = MutableStateFlow(StealthModeState())
    val state: StateFlow<StealthModeState> = _state.asStateFlow()

    private val repository: AppLockRepository = DataStoreAppLockRepository(context)

    init {
        loadCurrentDisguise()
    }

    private fun loadCurrentDisguise() {
        viewModelScope.launch {
            val current = repository.getCurrentDisguise()
            _state.value = _state.value.copy(
                currentDisguise = current,
                selectedDisguise = current
            )
        }
    }

    fun selectDisguise(disguise: DisguiseOption) {
        _state.value = _state.value.copy(selectedDisguise = disguise)
    }

    fun applyDisguise() {
        val newDisguise = _state.value.selectedDisguise
        if (newDisguise == _state.value.currentDisguise) return

        _state.value = _state.value.copy(isApplying = true)

        viewModelScope.launch {
            try {
                val pm = context.packageManager
                val pkgName = context.packageName

                // Disable current alias
                val currentAlias = _state.value.currentDisguise
                pm.setComponentEnabledSetting(
                    ComponentName(pkgName, "$pkgName.${currentAlias.aliasName}"),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )

                // Enable new alias
                pm.setComponentEnabledSetting(
                    ComponentName(pkgName, "$pkgName.${newDisguise.aliasName}"),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )

                // Save to repository
                repository.setCurrentDisguise(newDisguise)

                _state.value = _state.value.copy(
                    currentDisguise = newDisguise,
                    isApplying = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isApplying = false)
            }
        }
    }
}
