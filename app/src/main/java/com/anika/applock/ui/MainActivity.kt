package com.anika.applock.ui

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.anika.applock.domain.PinRepository
import com.anika.applock.platform.DataStorePinStorage
import com.anika.applock.ui.applist.AppListScreen
import com.anika.applock.ui.applist.AppListViewModel
import com.anika.applock.ui.intruder.IntruderLogScreen
import com.anika.applock.ui.intruder.IntruderLogViewModel
import com.anika.applock.ui.settings.SettingsScreen
import com.anika.applock.ui.settings.SettingsViewModel
import com.anika.applock.ui.setup.OnboardingScreen
import com.anika.applock.ui.setup.OnboardingViewModel
import com.anika.applock.ui.stealth.StealthModeScreen
import com.anika.applock.ui.stealth.StealthModeViewModel
import kotlinx.coroutines.launch

/**
 * Main activity with bottom navigation.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                val scope = rememberCoroutineScope()

                // Check if PIN is set (onboarding needed)
                var isPinSet by remember { mutableStateOf<Boolean?>(null) }

                LaunchedEffect(Unit) {
                    val pinRepo = PinRepository(DataStorePinStorage(applicationContext))
                    isPinSet = pinRepo.isPinSet()
                }

                when (isPinSet) {
                    null -> {
                        // Loading
                        Box(modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator()
                        }
                    }
                    false -> {
                        // Show onboarding
                        OnboardingScreen(
                            viewModel = viewModel {
                                OnboardingViewModel(applicationContext)
                            },
                            onComplete = {
                                isPinSet = true
                            }
                        )
                    }
                    true -> {
                        // Show main app
                        AppScaffold(navController)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppScaffold(navController: NavHostController) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute !in listOf("stealth")) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Lock, contentDescription = "Apps") },
                        label = { Text("Apps") },
                        selected = currentRoute == "apps",
                        onClick = { navController.navigate("apps") { launchSingleTop = true } }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        selected = currentRoute == "settings",
                        onClick = { navController.navigate("settings") { launchSingleTop = true } }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Camera, contentDescription = "Intruders") },
                        label = { Text("Intruders") },
                        selected = currentRoute == "intruders",
                        onClick = { navController.navigate("intruders") { launchSingleTop = true } }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "apps",
            modifier = Modifier.padding(padding)
        ) {
            composable("apps") {
                AppListScreen(
                    viewModel = viewModel {
                        AppListViewModel(navController.context)
                    }
                )
            }

            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel {
                        SettingsViewModel(navController.context)
                    },
                    onNavigateToStealth = {
                        navController.navigate("stealth")
                    }
                )
            }

            composable("intruders") {
                IntruderLogScreen(
                    viewModel = viewModel {
                        IntruderLogViewModel(navController.context)
                    }
                )
            }

            composable("stealth") {
                StealthModeScreen(
                    viewModel = viewModel {
                        StealthModeViewModel(navController.context)
                    }
                )
            }
        }
    }
}
