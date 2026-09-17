package com.anika.applock.ui.applist

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anika.applock.domain.AppLockRepository
import com.anika.applock.domain.NotificationPrivacy
import com.anika.applock.platform.AppInfo
import com.anika.applock.platform.DataStoreAppLockRepository
import com.anika.applock.platform.InstalledAppsProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class AppListState(
    val apps: List<AppInfo> = emptyList(),
    val protectedApps: Set<String> = emptySet(),
    val notificationPrivacy: Map<String, NotificationPrivacy> = emptyMap(),
    val isLoading: Boolean = true
)

class AppListViewModel(private val context: Context) : ViewModel() {

    private val _state = MutableStateFlow(AppListState())
    val state: StateFlow<AppListState> = _state.asStateFlow()

    private val repository: AppLockRepository = DataStoreAppLockRepository(context)
    private val appsProvider = InstalledAppsProvider(context)

    init {
        loadApps()
        observeProtectedApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val apps = appsProvider.getInstalledApps()
            _state.value = _state.value.copy(
                apps = apps,
                isLoading = false
            )

            // Load notification privacy for all apps
            val privacyMap = apps.associate { app ->
                app.packageName to repository.getNotificationPrivacy(app.packageName)
            }
            _state.value = _state.value.copy(notificationPrivacy = privacyMap)
        }
    }

    private fun observeProtectedApps() {
        viewModelScope.launch {
            repository.getProtectedApps().collectLatest { protected ->
                _state.value = _state.value.copy(protectedApps = protected)
            }
        }
    }

    fun toggleProtection(packageName: String) {
        viewModelScope.launch {
            if (packageName in _state.value.protectedApps) {
                repository.unprotect(packageName)
            } else {
                repository.protect(packageName)
            }
        }
    }

    fun setNotificationPrivacy(packageName: String, privacy: NotificationPrivacy) {
        viewModelScope.launch {
            repository.setNotificationPrivacy(packageName, privacy)
            _state.value = _state.value.copy(
                notificationPrivacy = _state.value.notificationPrivacy + (packageName to privacy)
            )
        }
    }
}
