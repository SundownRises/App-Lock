package com.anika.applock.ui.lock

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap

/**
 * Adaptive lock screen that switches between portrait (single column) and
 * landscape (two-pane split) layouts based on measured window dimensions.
 *
 * Critical: we compare width to height, not use WindowSizeClass, because
 * on a 12.7" tablet both orientations exceed the EXPANDED threshold.
 */
@Composable
fun LockScreen(
    state: LockUiState,
    onDigit: (Int) -> Unit,
    onBiometric: () -> Unit,
    onBackspace: () -> Unit
) {
    if (state !is LockUiState.Locked) return

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Landscape AND wide enough that two panes each get usable room
        val twoPane = maxWidth > maxHeight && maxWidth >= 720.dp

        if (twoPane) {
            // Two-pane split for landscape
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("twoPane")
            ) {
                LockedAppIdentity(
                    state = state,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("identityPane")
                )
                KeypadPane(
                    state = state,
                    onDigit = onDigit,
                    onBiometric = onBiometric,
                    onBackspace = onBackspace,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("keypadPane")
                )
            }
        } else {
            // Single column for portrait
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("singleColumn"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.weight(1.4f))  // Bias keypad low for thumb reach
                LockedAppIdentity(state)
                Spacer(Modifier.weight(0.6f))
                KeypadPane(state, onDigit, onBiometric, onBackspace)
                Spacer(Modifier.weight(0.4f))
            }
        }
    }
}

@Composable
private fun LockedAppIdentity(
    state: LockUiState.Locked,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App icon
        state.appIcon?.let { icon ->
            Image(
                bitmap = icon.toBitmap(128, 128).asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
            )
            Spacer(Modifier.height(24.dp))
        }

        // App name
        Text(
            text = state.appName,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "is locked",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        // Error message
        if (state.error != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = state.error,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Red,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun KeypadPane(
    state: LockUiState.Locked,
    onDigit: (Int) -> Unit,
    onBiometric: () -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 340.dp)  // Cap width so keys don't spread too far
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // PIN dots
        PinDots(
            enteredCount = state.enteredDigits,
            totalCount = state.pinLength
        )

        Spacer(Modifier.height(32.dp))

        // Keypad grid
        Keypad(
            onDigit = onDigit,
            onBiometric = if (state.canUseBiometric) onBiometric else null,
            onBackspace = onBackspace,
            enabled = state.cooldownRemaining == null || state.cooldownRemaining <= java.time.Duration.ZERO
        )
    }
}

@Composable
private fun PinDots(
    enteredCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(totalCount) { index ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        if (index < enteredCount) Color.White
                        else Color.White.copy(alpha = 0.3f)
                    )
            )
        }
    }
}

@Composable
private fun Keypad(
    onDigit: (Int) -> Unit,
    onBiometric: (() -> Unit)?,
    onBackspace: () -> Unit,
    enabled: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Rows 1-3
        for (row in 0..2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                for (col in 0..2) {
                    val digit = row * 3 + col + 1
                    KeypadButton(
                        label = digit.toString(),
                        onClick = { if (enabled) onDigit(digit) },
                        enabled = enabled
                    )
                }
            }
        }

        // Bottom row: biometric / 0 / backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
        ) {
            // Biometric button (if available)
            if (onBiometric != null) {
                KeypadButton(
                    label = "👆",  // fingerprint emoji
                    onClick = onBiometric,
                    enabled = enabled
                )
            } else {
                Spacer(Modifier.weight(1f).aspectRatio(1f))
            }

            // 0
            KeypadButton(
                label = "0",
                onClick = { if (enabled) onDigit(0) },
                enabled = enabled
            )

            // Backspace
            KeypadButton(
                label = "⌫",
                onClick = { if (enabled) onBackspace() },
                enabled = enabled
            )
        }
    }
}

@Composable
private fun KeypadButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .weight(1f)
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(
                if (enabled) Color.White.copy(alpha = 0.1f)
                else Color.White.copy(alpha = 0.05f)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.headlineLarge,
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.3f)
        )
    }
}
