package com.anika.applock.domain

import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * Manages lock/unlock sessions for protected apps.
 *
 * Core logic: apps unlock on correct PIN and remain unlocked until either:
 * - Screen turns off (cleared by ScreenStateReceiver calling onScreenOff)
 * - They've been in the background longer than the idle timeout
 *
 * Critical subtlety: the idle clock starts when you LEAVE an app, not when you unlock it.
 * Otherwise a 1-minute timeout would eject you mid-use.
 *
 * This class has ZERO Android dependencies so it's fully unit-testable with a fake Clock.
 */
class LockSessionManager(private val clock: Clock = Clock.systemDefaultZone()) {

    private sealed interface Session {
        /** App is currently in the foreground, cannot expire */
        data object Active : Session

        /** App left the foreground at this timestamp */
        data class Idle(val since: Instant) : Session
    }

    private val sessions = mutableMapOf<String, Session>()
    private var foreground: String? = null

    /**
     * Single entry point, called for EVERY foreground app change (not just protected apps —
     * we need exit timestamps for all apps).
     *
     * Returns true if [packageName] must be gated behind the PIN screen.
     *
     * @param packageName The package that came to the foreground
     * @param idleTimeout Grace period after leaving an app before it relocks
     */
    fun onForegroundApp(packageName: String, idleTimeout: Duration): Boolean {
        val previous = foreground

        // Stamp exit time for the app we just left
        if (previous != null && previous != packageName && sessions[previous] == Session.Active) {
            sessions[previous] = Session.Idle(clock.instant())
        }

        foreground = packageName

        return when (val session = sessions[packageName]) {
            null -> true  // Never unlocked this session

            Session.Active -> false  // Repeat event for the same app, already in it

            is Session.Idle -> {
                val idleFor = Duration.between(session.since, clock.instant())
                if (idleFor > idleTimeout) {
                    sessions.remove(packageName)
                    true  // Idled past grace period, must re-lock
                } else {
                    sessions[packageName] = Session.Active
                    false  // Still within grace period, stays unlocked
                }
            }
        }
    }

    /**
     * Marks [packageName] as unlocked after successful PIN entry.
     */
    fun markUnlocked(packageName: String) {
        sessions[packageName] = Session.Active
        foreground = packageName
    }

    /**
     * Clears all sessions when the screen turns off.
     * Called by ScreenStateReceiver on ACTION_SCREEN_OFF.
     */
    fun onScreenOff() {
        sessions.clear()
        foreground = null
    }

    /**
     * For testing: exposes current session count
     */
    internal fun sessionCount() = sessions.size
}
