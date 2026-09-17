package com.anika.applock.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anika.applock.domain.DisguiseOption
import java.time.Duration

/**
 * Settings screen for timeout, recovery code, and stealth mode.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToStealth: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Current disguise indicator
            DisguiseIndicator(
                currentDisguise = state.currentDisguise
            )

            Divider()

            // Idle timeout setting
            IdleTimeoutSetting(
                currentTimeout = state.idleTimeout,
                onTimeoutChange = viewModel::setIdleTimeout
            )

            Divider()

            // Recovery code section
            RecoveryCodeSection(
                recoveryCode = state.recoveryCode,
                onReveal = viewModel::revealRecoveryCode
            )

            Divider()

            // Stealth mode section
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = onNavigateToStealth
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Stealth Mode",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Change app icon and name",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("→")
                }
            }
        }
    }
}

@Composable
private fun DisguiseIndicator(
    currentDisguise: DisguiseOption
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (currentDisguise != DisguiseOption.APP_LOCK)
                MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (currentDisguise != DisguiseOption.APP_LOCK) "🥸" else "🔒",
                style = MaterialTheme.typography.headlineMedium
            )
            Column {
                Text(
                    text = "Currently disguised as:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = currentDisguise.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun IdleTimeoutSetting(
    currentTimeout: Duration,
    onTimeoutChange: (Duration) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { showDialog = true }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Idle Timeout",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Apps relock after ${currentTimeout.toMinutes()} minutes in background",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${currentTimeout.toMinutes()} min",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (showDialog) {
        IdleTimeoutDialog(
            currentMinutes = currentTimeout.toMinutes().toInt(),
            onConfirm = { minutes ->
                onTimeoutChange(Duration.ofMinutes(minutes.toLong()))
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun IdleTimeoutDialog(
    currentMinutes: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMinutes by remember { mutableStateOf(currentMinutes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Idle Timeout") },
        text = {
            Column {
                Text("Apps will relock after this many minutes in the background:")
                Spacer(Modifier.height(16.dp))

                listOf(1, 2, 5, 10, 15, 30).forEach { minutes ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMinutes = minutes }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedMinutes == minutes,
                            onClick = { selectedMinutes = minutes }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("$minutes minute${if (minutes > 1) "s" else ""}")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedMinutes) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun RecoveryCodeSection(
    recoveryCode: String?,
    onReveal: () -> Unit
) {
    var revealed by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Recovery Code",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Use this code if you forget your PIN",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (revealed && recoveryCode != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = recoveryCode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = androidx.compose.ui.unit.sp(2)
                    )
                }
            } else {
                Button(
                    onClick = {
                        onReveal()
                        revealed = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reveal Recovery Code")
                }
            }
        }
    }
}
