package com.anika.applock.domain

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.*

class LockSessionManagerTest {

    private lateinit var fakeClock: FakeClock
    private lateinit var manager: LockSessionManager

    @Before
    fun setup() {
        fakeClock = FakeClock()
        manager = LockSessionManager(fakeClock)
    }

    @Test
    fun `never-unlocked app must lock`() {
        val mustLock = manager.onForegroundApp("com.example.app", Duration.ofMinutes(5))
        assertTrue("Never-unlocked app should require PIN", mustLock)
    }

    @Test
    fun `switch away and return inside timeout stays unlocked`() {
        val timeout = Duration.ofMinutes(1)

        // Unlock the app
        manager.markUnlocked("com.example.gallery")
        manager.onForegroundApp("com.example.gallery", timeout)

        // Switch to another app
        manager.onForegroundApp("com.example.chrome", timeout)

        // Advance time by 30 seconds (within the 1-minute timeout)
        fakeClock.advance(Duration.ofSeconds(30))

        // Return to gallery - should still be unlocked
        val mustLock = manager.onForegroundApp("com.example.gallery", timeout)
        assertFalse("App should stay unlocked within grace period", mustLock)
    }

    @Test
    fun `cross the timeout boundary by one second locks`() {
        val timeout = Duration.ofMinutes(1)

        // Unlock the app
        manager.markUnlocked("com.example.gallery")
        manager.onForegroundApp("com.example.gallery", timeout)

        // Switch to another app
        manager.onForegroundApp("com.example.chrome", timeout)

        // Advance time past the timeout (1 minute + 1 second)
        fakeClock.advance(Duration.ofSeconds(61))

        // Return to gallery - should now be locked
        val mustLock = manager.onForegroundApp("com.example.gallery", timeout)
        assertTrue("App should lock after timeout expires", mustLock)
    }

    @Test
    fun `screen-off clears all sessions`() {
        val timeout = Duration.ofMinutes(5)

        // Unlock and use an app
        manager.markUnlocked("com.example.gallery")
        manager.onForegroundApp("com.example.gallery", timeout)

        // Screen turns off
        manager.onScreenOff()

        // Try to open the app again - should be locked
        val mustLock = manager.onForegroundApp("com.example.gallery", timeout)
        assertTrue("App should lock after screen-off", mustLock)
        assertEquals("All sessions should be cleared", 0, manager.sessionCount())
    }

    @Test
    fun `unlocked app in continuous use never expires`() {
        val timeout = Duration.ofMinutes(1)

        // Unlock the app
        manager.markUnlocked("com.example.gallery")

        // Keep it in the foreground for 10 minutes with periodic checks
        for (i in 1..10) {
            fakeClock.advance(Duration.ofMinutes(1))
            val mustLock = manager.onForegroundApp("com.example.gallery", timeout)
            assertFalse("Continuous use should never expire", mustLock)
        }
    }

    @Test
    fun `repeated onForegroundApp for same package does not pin it unlocked`() {
        val timeout = Duration.ofMinutes(1)

        // This is the regression test: if we call onForegroundApp twice for the same
        // app without unlocking it, it should NOT become permanently unlocked.

        val firstCheck = manager.onForegroundApp("com.example.gallery", timeout)
        assertTrue("First check: app should require unlock", firstCheck)

        // Accidentally call it again (simulating duplicate accessibility events)
        fakeClock.advance(Duration.ofSeconds(5))
        val secondCheck = manager.onForegroundApp("com.example.gallery", timeout)
        assertTrue("Second check: app should still require unlock", secondCheck)

        // Advance time significantly
        fakeClock.advance(Duration.ofMinutes(10))

        // Switch to another app and back
        manager.onForegroundApp("com.example.chrome", timeout)
        val thirdCheck = manager.onForegroundApp("com.example.gallery", timeout)
        assertTrue("Third check: app should still require unlock", thirdCheck)
    }

    @Test
    fun `non-protected app in between correctly stamps protected app exit time`() {
        val timeout = Duration.ofMinutes(1)

        // Unlock and open gallery
        manager.markUnlocked("com.example.gallery")
        manager.onForegroundApp("com.example.gallery", timeout)

        // Switch to chrome (which might not be protected, but we track it anyway)
        manager.onForegroundApp("com.example.chrome", timeout)

        // Advance time by 30 seconds
        fakeClock.advance(Duration.ofSeconds(30))

        // Switch to another app
        manager.onForegroundApp("com.example.email", timeout)

        // Advance another 35 seconds (total 65 seconds since leaving gallery)
        fakeClock.advance(Duration.ofSeconds(35))

        // Return to gallery - should be locked (idle for 65s > 60s timeout)
        val mustLock = manager.onForegroundApp("com.example.gallery", timeout)
        assertTrue("Gallery should lock after total idle time exceeds timeout", mustLock)
    }

    @Test
    fun `multiple apps with different idle states`() {
        val timeout = Duration.ofMinutes(1)

        // Unlock gallery
        manager.markUnlocked("com.example.gallery")
        manager.onForegroundApp("com.example.gallery", timeout)

        // Switch to photos, unlock it
        manager.onForegroundApp("com.example.photos", timeout)
        manager.markUnlocked("com.example.photos")

        // Advance 30 seconds
        fakeClock.advance(Duration.ofSeconds(30))

        // Switch to whatsapp, unlock it
        manager.onForegroundApp("com.example.whatsapp", timeout)
        manager.markUnlocked("com.example.whatsapp")

        // Advance 35 more seconds (total: gallery idle 65s, photos idle 35s, whatsapp active)
        fakeClock.advance(Duration.ofSeconds(35))

        // Check gallery: should be locked (65s > 60s)
        val galleryLocked = manager.onForegroundApp("com.example.gallery", timeout)
        assertTrue("Gallery should be locked", galleryLocked)

        // Check photos: should be unlocked (35s < 60s)
        val photosLocked = manager.onForegroundApp("com.example.photos", timeout)
        assertFalse("Photos should still be unlocked", photosLocked)

        // Check whatsapp: should be unlocked (was just in it)
        val whatsappLocked = manager.onForegroundApp("com.example.whatsapp", timeout)
        assertFalse("WhatsApp should still be unlocked", whatsappLocked)
    }

    /**
     * Fake Clock that can be advanced manually for testing
     */
    private class FakeClock : Clock() {
        private var instant = Instant.parse("2024-01-01T12:00:00Z")

        fun advance(duration: Duration) {
            instant = instant.plus(duration)
        }

        override fun instant() = instant
        override fun getZone() = ZoneId.of("UTC")
        override fun withZone(zone: ZoneId) = this
    }
}
