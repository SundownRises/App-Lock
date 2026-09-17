package com.anika.applock.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.anika.applock.domain.LockSessionManager
import com.anika.applock.platform.DataStoreAppLockRepository
import com.anika.applock.ui.lock.LockActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Accessibility service that detects foreground app changes and triggers the lock screen.
 *
 * Event-driven (not polling) for battery efficiency.
 */
class AppWatchAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var sessionManager: LockSessionManager
    private lateinit var repository: DataStoreAppLockRepository

    companion object {
        // Packages to ignore (ourselves + system UI)
        private val IGNORED_PACKAGES = setOf(
            "com.android.systemui",
            "android"
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        sessionManager = LockSessionManager()
        repository = DataStoreAppLockRepository(applicationContext)

        // Start the foreground guard service
        startService(Intent(this, LockGuardService::class.java))
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // Ignore our own package and system UI
        if (packageName == this.packageName || packageName in IGNORED_PACKAGES) return

        scope.launch {
            // Called unconditionally: the manager needs exit timestamps for every app
            val timeout = repository.getIdleTimeout()
            val mustLock = sessionManager.onForegroundApp(packageName, timeout)

            // Only show lock screen if this package is protected
            if (mustLock && repository.isProtected(packageName)) {
                val intent = Intent(this@AppWatchAccessibilityService, LockActivity::class.java).apply {
                    putExtra(LockActivity.EXTRA_TARGET_PACKAGE, packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(intent)
            }
        }
    }

    override fun onInterrupt() {
        // Required override, no-op
    }

    override fun onDestroy() {
        super.onDestroy()
        // Service destroyed, will be restarted by LockGuardService
    }
}
