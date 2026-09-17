package com.anika.applock.ui.lock

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Full-screen PIN entry activity shown when a protected app comes to the foreground.
 *
 * Critical behaviors:
 * - FLAG_SECURE prevents screenshots and keeps us out of recents
 * - On Back/failure, returns to launcher (not finish()) to hide the protected app
 * - Survives rotation (Compose + ViewModel handle state preservation)
 */
class LockActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TARGET_PACKAGE = "target_package"
    }

    private val viewModel: LockViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LockViewModel(
                    targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE) ?: "",
                    context = applicationContext
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // FLAG_SECURE: no screenshots, no content in recents
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        setContent {
            val state by viewModel.uiState.collectAsState()

            LockScreen(
                state = state,
                onDigit = viewModel::onDigit,
                onBiometric = viewModel::onBiometric,
                onBackspace = viewModel::onBackspace
            )

            // Handle unlock success
            if (state is LockUiState.Unlocked) {
                finish()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Do NOT call super.onBackPressed() - that would reveal the protected app
        bailOut()
    }

    /**
     * Returns to launcher instead of revealing the protected app behind us.
     */
    private fun bailOut() {
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(launcherIntent)
        finish()
    }
}
