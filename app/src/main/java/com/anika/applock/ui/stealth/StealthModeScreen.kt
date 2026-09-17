package com.anika.applock.ui.stealth

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anika.applock.domain.DisguiseOption

/**
 * Stealth mode screen - choose app disguise.
 */
@Composable
fun StealthModeScreen(
    viewModel: StealthModeViewModel
) {
    val state by viewModel.state.collectAsState()
    var showWarning by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stealth Mode") },
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
        ) {
            // Warning card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⚠️ How Stealth Mode Works",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = """
                            • Your launcher icon and name will change
                            • The old icon will disappear, the new one will appear
                            • This takes 5-30 seconds (launcher refresh)
                            • The package name stays com.anika.applock
                            • This hides from casual browsing, not developer tools
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Disguise options
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(DisguiseOption.entries) { disguise ->
                    DisguiseOption(
                        disguise = disguise,
                        isSelected = disguise == state.currentDisguise,
                        isApplying = state.isApplying && disguise == state.selectedDisguise,
                        onClick = {
                            viewModel.selectDisguise(disguise)
                            showWarning = true
                        }
                    )
                }
            }
        }
    }

    // Confirmation dialog
    if (showWarning) {
        AlertDialog(
            onDismissRequest = { showWarning = false },
            title = { Text("Apply Disguise?") },
            text = {
                Text(
                    """
                        The launcher icon will change to "${state.selectedDisguise.displayName}".

                        This will take a few moments. The old icon will disappear and the new one will appear.
                    """.trimIndent()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.applyDisguise()
                        showWarning = false
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DisguiseOption(
    disguise: DisguiseOption,
    isSelected: Boolean,
    isApplying: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon placeholder (TODO: actual icons)
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = disguise.emoji,
                    style = MaterialTheme.typography.displayMedium
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = disguise.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = "Launcher will show this name and icon",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (isApplying) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
    }
}

private val DisguiseOption.emoji: String
    get() = when (this) {
        DisguiseOption.APP_LOCK -> "🔒"
        DisguiseOption.WEATHER -> "🌤️"
        DisguiseOption.CALCULATOR -> "🔢"
    }
