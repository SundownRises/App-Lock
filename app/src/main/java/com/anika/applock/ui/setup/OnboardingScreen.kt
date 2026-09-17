package com.anika.applock.ui.setup

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anika.applock.platform.PermissionChecker
import kotlinx.coroutines.delay

/**
 * Onboarding wizard that guides the user through all required permissions.
 */
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onComplete: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.currentStep) {
        // Refresh permission states when returning from settings
        while (true) {
            delay(1000)  // Check every second
            viewModel.refreshPermissions()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "App Lock Setup",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        // Progress
        LinearProgressIndicator(
            progress = { state.progress },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Step ${state.currentStepNumber} of ${state.totalSteps}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        // Current step content
        when (state.currentStep) {
            OnboardingStep.WELCOME -> WelcomeStep(onNext = viewModel::nextStep)
            OnboardingStep.PIN_SETUP -> PinSetupStep(
                viewModel = viewModel,
                onNext = viewModel::nextStep
            )
            OnboardingStep.RECOVERY_CODE -> RecoveryCodeStep(
                recoveryCode = state.recoveryCode ?: "",
                onConfirm = viewModel::nextStep
            )
            OnboardingStep.RESTRICTED_SETTINGS -> RestrictedSettingsStep(
                onOpenSettings = {
                    context.startActivity(PermissionChecker(context).getAppInfoIntent())
                },
                onNext = viewModel::nextStep,
                onSkip = viewModel::skipRestrictedSettings
            )
            OnboardingStep.ACCESSIBILITY -> AccessibilityStep(
                isGranted = state.permissions.accessibility,
                onOpenSettings = {
                    context.startActivity(PermissionChecker(context).getAccessibilityIntent())
                },
                onNext = viewModel::nextStep
            )
            OnboardingStep.NOTIFICATION_LISTENER -> NotificationListenerStep(
                isGranted = state.permissions.notificationListener,
                onOpenSettings = {
                    context.startActivity(PermissionChecker(context).getNotificationListenerIntent())
                },
                onNext = viewModel::nextStep,
                onSkip = viewModel::skipNotificationListener
            )
            OnboardingStep.CAMERA -> CameraStep(
                isGranted = state.permissions.camera,
                onRequestPermission = viewModel::requestCamera,
                onNext = viewModel::nextStep,
                onSkip = viewModel::skipCamera
            )
            OnboardingStep.BATTERY_OPTIMIZATION -> BatteryOptimizationStep(
                isGranted = state.permissions.batteryOptimization,
                onOpenSettings = {
                    context.startActivity(PermissionChecker(context).getBatteryOptimizationIntent())
                },
                onNext = viewModel::nextStep
            )
            OnboardingStep.DEVICE_ADMIN -> DeviceAdminStep(
                isGranted = state.permissions.deviceAdmin,
                onActivate = {
                    context.startActivity(PermissionChecker(context).getDeviceAdminIntent())
                },
                onNext = viewModel::nextStep
            )
            OnboardingStep.COMPLETE -> CompleteStep(
                onFinish = onComplete
            )
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Welcome to App Lock",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Text(
            text = """
                This wizard will guide you through setting up App Lock to protect your apps.

                You'll need to grant several permissions. Each one is essential for the app to work correctly.

                Setup takes about 2-3 minutes.
            """.trimIndent(),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Get Started")
        }
    }
}

@Composable
private fun PinSetupStep(
    viewModel: OnboardingViewModel,
    onNext: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Create Your PIN",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Choose a 4-6 digit PIN to lock your apps",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        OutlinedTextField(
            value = pin,
            onValueChange = {
                if (it.all { char -> char.isDigit() } && it.length <= 6) {
                    pin = it
                    error = null
                }
            },
            label = { Text("Enter PIN") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = confirmPin,
            onValueChange = {
                if (it.all { char -> char.isDigit() } && it.length <= 6) {
                    confirmPin = it
                    error = null
                }
            },
            label = { Text("Confirm PIN") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (error != null) {
            Text(
                text = error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Button(
            onClick = {
                when {
                    pin.length < 4 -> error = "PIN must be at least 4 digits"
                    pin != confirmPin -> error = "PINs do not match"
                    else -> {
                        viewModel.setPin(pin)
                        onNext()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = pin.length >= 4 && confirmPin.length >= 4
        ) {
            Text("Continue")
        }
    }
}

@Composable
private fun RecoveryCodeStep(
    recoveryCode: String,
    onConfirm: () -> Unit
) {
    var confirmed by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Recovery Code",
            style = MaterialTheme.typography.headlineMedium
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "⚠️ IMPORTANT: Save this code!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )

                Text(
                    text = "If you forget your PIN, this is the ONLY way to recover access. Device Admin will prevent uninstallation without it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = recoveryCode,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    letterSpacing = androidx.compose.ui.unit.sp(2)
                )
            }
        }

        Text(
            text = "Write this code down and keep it somewhere safe (not on this tablet).",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = confirmed,
                onCheckedChange = { confirmed = it }
            )
            Text(
                text = "I have saved this recovery code",
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
            enabled = confirmed
        ) {
            Text("Continue")
        }
    }
}

@Composable
private fun RestrictedSettingsStep(
    onOpenSettings: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    // Only show on Android 13+
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        LaunchedEffect(Unit) {
            onSkip()
        }
        return
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Allow Restricted Settings",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Android 13+ requires this step",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = """
                        1. Tap "Open Settings" below
                        2. Tap the ⋮ menu (three dots) in the top right
                        3. Select "Allow restricted settings"
                        4. Return here
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Button(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Open Settings")
        }

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Text("I've Done This → Continue")
        }
    }
}

@Composable
private fun AccessibilityStep(
    isGranted: Boolean,
    onOpenSettings: () -> Unit,
    onNext: () -> Unit
) {
    PermissionStepTemplate(
        title = "Enable Accessibility Service",
        description = """
            App Lock uses the Accessibility Service to detect when you open a protected app.

            It only monitors which app comes to the foreground — it cannot read screen contents.
        """.trimIndent(),
        isGranted = isGranted,
        grantedText = "✓ Accessibility Service enabled",
        buttonText = "Open Accessibility Settings",
        onOpenSettings = onOpenSettings,
        onNext = onNext,
        canSkip = false
    )
}

@Composable
private fun NotificationListenerStep(
    isGranted: Boolean,
    onOpenSettings: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    PermissionStepTemplate(
        title = "Enable Notification Listener",
        description = """
            Optional: allows hiding notification content from locked apps.

            If you skip this, notifications will show normally.
        """.trimIndent(),
        isGranted = isGranted,
        grantedText = "✓ Notification Listener enabled",
        buttonText = "Open Notification Access",
        onOpenSettings = onOpenSettings,
        onNext = onNext,
        canSkip = true,
        onSkip = onSkip
    )
}

@Composable
private fun CameraStep(
    isGranted: Boolean,
    onRequestPermission: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    PermissionStepTemplate(
        title = "Camera Permission",
        description = """
            Optional: captures a photo after wrong PIN attempts (intruder selfie).

            If you skip this, you won't get photos of failed unlock attempts.
        """.trimIndent(),
        isGranted = isGranted,
        grantedText = "✓ Camera permission granted",
        buttonText = "Grant Camera Permission",
        onOpenSettings = onRequestPermission,
        onNext = onNext,
        canSkip = true,
        onSkip = onSkip
    )
}

@Composable
private fun BatteryOptimizationStep(
    isGranted: Boolean,
    onOpenSettings: () -> Unit,
    onNext: () -> Unit
) {
    PermissionStepTemplate(
        title = "Disable Battery Optimization",
        description = """
            Essential for reliability. Prevents Android from killing the app lock service.

            Find "App Lock" in the list and select "Don't optimize".
        """.trimIndent(),
        isGranted = isGranted,
        grantedText = "✓ Battery optimization disabled",
        buttonText = "Open Battery Settings",
        onOpenSettings = onOpenSettings,
        onNext = onNext,
        canSkip = false
    )
}

@Composable
private fun DeviceAdminStep(
    isGranted: Boolean,
    onActivate: () -> Unit,
    onNext: () -> Unit
) {
    PermissionStepTemplate(
        title = "Activate Device Admin",
        description = """
            Prevents uninstallation of App Lock, so protected apps cannot be bypassed by removing the app.

            To uninstall later, you'll need to deactivate Device Admin in Settings first.
        """.trimIndent(),
        isGranted = isGranted,
        grantedText = "✓ Device Admin active",
        buttonText = "Activate Device Admin",
        onOpenSettings = onActivate,
        onNext = onNext,
        canSkip = false
    )
}

@Composable
private fun CompleteStep(
    onFinish: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "✓ Setup Complete!",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )

        Text(
            text = """
                App Lock is now protecting your device.

                Next, you'll select which apps to protect.
            """.trimIndent(),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue to App Selection")
        }
    }
}

@Composable
private fun PermissionStepTemplate(
    title: String,
    description: String,
    isGranted: Boolean,
    grantedText: String,
    buttonText: String,
    onOpenSettings: () -> Unit,
    onNext: () -> Unit,
    canSkip: Boolean,
    onSkip: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        if (isGranted) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = grantedText,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue")
            }
        } else {
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(buttonText)
            }

            if (canSkip && onSkip != null) {
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Skip This Step")
                }
            }
        }
    }
}
