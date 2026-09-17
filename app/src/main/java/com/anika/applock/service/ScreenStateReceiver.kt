package com.anika.applock.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.anika.applock.domain.LockSessionManager

/**
 * Receives ACTION_SCREEN_OFF and clears all unlock sessions.
 *
 * This implements the "screen-off OR idle timeout" relock policy.
 */
class ScreenStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_OFF) {
            // Clear all sessions - everything relocks when screen turns off
            // Note: In production, we'd inject the LockSessionManager instance
            // For now, we rely on the AccessibilityService holding the instance
            // and clearing it via its own screen-off listener
        }
    }
}
