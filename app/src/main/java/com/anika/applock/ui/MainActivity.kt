package com.anika.applock.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Main activity - settings and app list.
 *
 * For now, this is a placeholder to allow testing the lock screen.
 * Full implementation includes:
 * - Onboarding flow for permissions
 * - App list with checkboxes and notification privacy dropdowns
 * - Settings (timeout, recovery code, stealth mode)
 * - Intruder log viewer
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
private fun MainScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "App Lock",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Settings UI coming soon",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        Text(
            text = """
                Core features implemented:
                • Lock session manager
                • PIN repository with rate limiting
                • Accessibility service for detection
                • Notification privacy service
                • Lock screen with adaptive layout
                • Intruder camera capture
                • Device admin receiver

                To test:
                1. Enable Accessibility Service in Settings
                2. Add apps to protect (via DataStore directly for now)
                3. Open a protected app
            """.trimIndent(),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
