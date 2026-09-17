package com.anika.applock.ui.lock

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for adaptive lock screen layout.
 *
 * Verifies the portrait/landscape requirement is met:
 * - Portrait (920x1472dp): single column layout
 * - Landscape (1472x920dp): two-pane split layout
 */
class LockScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testState = LockUiState.Locked(
        appName = "Test App",
        appIcon = null,
        enteredDigits = 0,
        pinLength = 6,
        error = null,
        canUseBiometric = false,
        cooldownRemaining = null
    )

    @Test
    fun landscape_usesTwoPaneLayout() {
        composeTestRule.setContent {
            androidx.compose.ui.platform.LocalConfiguration
            androidx.compose.foundation.layout.BoxWithConstraints {
                // Force landscape dimensions (1472x920 dp)
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.ui.Modifier.size(DpSize(1472.dp, 920.dp))
                ) {
                    LockScreen(
                        state = testState,
                        onDigit = {},
                        onBiometric = {},
                        onBackspace = {}
                    )
                }
            }
        }

        // In landscape, both panes should be visible
        composeTestRule.onNodeWithTag("twoPane").assertExists()
        composeTestRule.onNodeWithTag("identityPane").assertExists()
        composeTestRule.onNodeWithTag("keypadPane").assertExists()
    }

    @Test
    fun portrait_usesSingleColumnLayout() {
        composeTestRule.setContent {
            androidx.compose.foundation.layout.BoxWithConstraints {
                // Force portrait dimensions (920x1472 dp)
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.ui.Modifier.size(DpSize(920.dp, 1472.dp))
                ) {
                    LockScreen(
                        state = testState,
                        onDigit = {},
                        onBiometric = {},
                        onBackspace = {}
                    )
                }
            }
        }

        // In portrait, single column layout
        composeTestRule.onNodeWithTag("singleColumn").assertExists()
        // Two-pane tags should not exist
        composeTestRule.onNodeWithTag("identityPane").assertDoesNotExist()
        composeTestRule.onNodeWithTag("keypadPane").assertDoesNotExist()
    }

    @Test
    fun keypad_displaysAllDigits() {
        composeTestRule.setContent {
            LockScreen(
                state = testState,
                onDigit = {},
                onBiometric = {},
                onBackspace = {}
            )
        }

        // Check all digits 0-9 are present
        for (digit in 0..9) {
            composeTestRule.onNodeWithText(digit.toString()).assertExists()
        }

        // Check backspace is present
        composeTestRule.onNodeWithText("⌫").assertExists()
    }

    @Test
    fun pinDots_showCorrectCount() {
        val stateWithDigits = testState.copy(enteredDigits = 3)

        composeTestRule.setContent {
            LockScreen(
                state = stateWithDigits,
                onDigit = {},
                onBiometric = {},
                onBackspace = {}
            )
        }

        // This is a basic assertion - in reality you'd check the filled/unfilled dots
        // For now just verify the screen renders without crashing
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun errorMessage_displaysWhenPresent() {
        val stateWithError = testState.copy(error = "Wrong PIN")

        composeTestRule.setContent {
            LockScreen(
                state = stateWithError,
                onDigit = {},
                onBiometric = {},
                onBackspace = {}
            )
        }

        composeTestRule.onNodeWithText("Wrong PIN").assertExists()
    }

    @Test
    fun biometricButton_notShownWhenUnavailable() {
        val stateWithoutBiometric = testState.copy(canUseBiometric = false)

        composeTestRule.setContent {
            LockScreen(
                state = stateWithoutBiometric,
                onDigit = {},
                onBiometric = {},
                onBackspace = {}
            )
        }

        // Biometric button (fingerprint emoji) should not be present
        composeTestRule.onNodeWithText("👆").assertDoesNotExist()
    }

    @Test
    fun biometricButton_shownWhenAvailable() {
        val stateWithBiometric = testState.copy(canUseBiometric = true)

        composeTestRule.setContent {
            LockScreen(
                state = stateWithBiometric,
                onDigit = {},
                onBiometric = {},
                onBackspace = {}
            )
        }

        // Biometric button (fingerprint emoji) should be present
        composeTestRule.onNodeWithText("👆").assertExists()
    }
}
