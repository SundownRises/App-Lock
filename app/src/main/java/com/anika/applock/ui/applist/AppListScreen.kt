package com.anika.applock.ui.applist

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
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.anika.applock.domain.NotificationPrivacy
import com.anika.applock.platform.AppInfo

/**
 * Screen to select which apps to protect and set notification privacy.
 */
@Composable
fun AppListScreen(
    viewModel: AppListViewModel
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Protected Apps") },
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
            // Header with count
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${state.protectedApps.size} apps protected",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Select apps to lock. Tap a protected app to configure notification privacy.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.apps) { app ->
                        AppListItem(
                            app = app,
                            isProtected = app.packageName in state.protectedApps,
                            notificationPrivacy = state.notificationPrivacy[app.packageName]
                                ?: NotificationPrivacy.SHOW_NORMALLY,
                            onToggleProtection = { viewModel.toggleProtection(app.packageName) },
                            onNotificationPrivacyChange = { privacy ->
                                viewModel.setNotificationPrivacy(app.packageName, privacy)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppListItem(
    app: AppInfo,
    isProtected: Boolean,
    notificationPrivacy: NotificationPrivacy,
    onToggleProtection: () -> Unit,
    onNotificationPrivacyChange: (NotificationPrivacy) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Main row with checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleProtection() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // App icon
                Image(
                    bitmap = app.icon.toBitmap(64, 64).asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )

                // App name
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (isProtected) {
                        Text(
                            text = "Notifications: ${notificationPrivacy.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Checkbox
                Checkbox(
                    checked = isProtected,
                    onCheckedChange = { onToggleProtection() }
                )
            }

            // Notification privacy dropdown (shown when protected)
            if (isProtected) {
                Divider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Notifications:",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(100.dp)
                    )

                    NotificationPrivacyDropdown(
                        current = notificationPrivacy,
                        onSelect = onNotificationPrivacyChange
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationPrivacyDropdown(
    current: NotificationPrivacy,
    onSelect: (NotificationPrivacy) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(current.displayName)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            NotificationPrivacy.entries.forEach { privacy ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(privacy.displayName)
                            Text(
                                text = privacy.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onSelect(privacy)
                        expanded = false
                    }
                )
            }
        }
    }
}

private val NotificationPrivacy.displayName: String
    get() = when (this) {
        NotificationPrivacy.SHOW_NORMALLY -> "Show normally"
        NotificationPrivacy.HIDE_CONTENT -> "Hide content"
        NotificationPrivacy.HIDE_COMPLETELY -> "Hide completely"
    }

private val NotificationPrivacy.description: String
    get() = when (this) {
        NotificationPrivacy.SHOW_NORMALLY -> "Full notification content visible"
        NotificationPrivacy.HIDE_CONTENT -> "Generic placeholder shown"
        NotificationPrivacy.HIDE_COMPLETELY -> "No notification shown"
    }
