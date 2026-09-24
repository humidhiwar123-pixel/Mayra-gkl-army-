package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.services.AutomationWorker
import com.example.services.MyraForegroundService
import com.example.ui.components.MyraBottomBar
import com.example.ui.components.MyraTopBar
import com.example.ui.navigation.Screen
import com.example.ui.screens.*
import com.example.ui.theme.MyraAiTheme
import com.example.ui.viewmodel.MyraViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MyraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Start Foreground Service & Periodic Automation checks
        try {
            MyraForegroundService.start(this)
            AutomationWorker.schedulePeriodicCheck(this)
        } catch (_: Exception) {}

        setContent {
            MyraAiTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

                // Request essential permissions on first launch
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { /* permissions handled */ }

                LaunchedEffect(Unit) {
                    val perms = mutableListOf(
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        perms.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    permissionLauncher.launch(perms.toTypedArray())
                }

                val currentTitle = when (currentRoute) {
                    Screen.Home.route -> "MYRA AI"
                    Screen.Chat.route -> "NEURAL CHAT"
                    Screen.Voice.route -> "VOICE ASSISTANT"
                    Screen.AiTools.route -> "AI TOOL SUITE"
                    Screen.Settings.route -> "SETTINGS"
                    Screen.Communication.route -> "COMMUNICATION"
                    Screen.DeviceTools.route -> "DEVICE TOOLS"
                    Screen.FileManager.route -> "FILE MANAGER"
                    Screen.Photos.route -> "PHOTOS & VISION"
                    Screen.DeepResearch.route -> "DEEP RESEARCH"
                    Screen.Automations.route -> "AUTOMATIONS"
                    Screen.Maps.route -> "MY WORLD / MAPS"
                    Screen.PcConnect.route -> "PC CONNECT"
                    Screen.Notifications.route -> "NOTIFICATIONS"
                    Screen.ScreenAutomation.route -> "SCREEN AUTOMATION"
                    Screen.Security.route -> "SECURITY"
                    Screen.Memory.route -> "MEMORY CORE"
                    Screen.Developer.route -> "DEV DIAGNOSTICS"
                    Screen.About.route -> "ABOUT MYRA"
                    else -> "MYRA AI"
                }

                val isRootScreen = currentRoute in listOf(
                    Screen.Home.route,
                    Screen.Chat.route,
                    Screen.Voice.route,
                    Screen.AiTools.route,
                    Screen.Settings.route
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        MyraTopBar(
                            title = currentTitle,
                            canNavigateBack = !isRootScreen,
                            onNavigateBack = { navController.popBackStack() },
                            onOpenSecurity = { navController.navigate(Screen.Security.route) },
                            onOpenDeveloper = { navController.navigate(Screen.Developer.route) }
                        )
                    },
                    bottomBar = {
                        if (isRootScreen) {
                            MyraBottomBar(
                                currentRoute = currentRoute,
                                onNavigateToRoute = { route ->
                                    if (route != currentRoute) {
                                        navController.navigate(route) {
                                            popUpTo(Screen.Home.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        composable(Screen.Home.route) {
                            HomeScreen(viewModel = viewModel, onNavigate = { navController.navigate(it) })
                        }
                        composable(Screen.Chat.route) {
                            ChatScreen(viewModel = viewModel)
                        }
                        composable(Screen.Voice.route) {
                            VoiceScreen(viewModel = viewModel)
                        }
                        composable(Screen.AiTools.route) {
                            AiToolsScreen(viewModel = viewModel, onNavigate = { navController.navigate(it) })
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen(viewModel = viewModel, onNavigate = { navController.navigate(it) })
                        }

                        // Feature Hubs
                        composable(Screen.Communication.route) {
                            CommunicationScreen(viewModel = viewModel)
                        }
                        composable(Screen.DeviceTools.route) {
                            DeviceToolsScreen(viewModel = viewModel)
                        }
                        composable(Screen.FileManager.route) {
                            FileManagerScreen(viewModel = viewModel)
                        }
                        composable(Screen.Photos.route) {
                            PhotosVisionScreen(viewModel = viewModel)
                        }
                        composable(Screen.DeepResearch.route) {
                            DeepResearchScreen(viewModel = viewModel)
                        }
                        composable(Screen.Automations.route) {
                            AutomationsScreen(viewModel = viewModel)
                        }
                        composable(Screen.Maps.route) {
                            MapsScreen(viewModel = viewModel)
                        }
                        composable(Screen.PcConnect.route) {
                            PcConnectScreen(viewModel = viewModel)
                        }
                        composable(Screen.Notifications.route) {
                            NotificationCenterScreen(viewModel = viewModel)
                        }
                        composable(Screen.ScreenAutomation.route) {
                            ScreenAutomationScreen(viewModel = viewModel)
                        }
                        composable(Screen.Security.route) {
                            SecurityScreen(viewModel = viewModel)
                        }
                        composable(Screen.Memory.route) {
                            MemoryScreen(viewModel = viewModel)
                        }
                        composable(Screen.Developer.route) {
                            DeveloperScreen(viewModel = viewModel)
                        }
                        composable(Screen.About.route) {
                            AboutScreen()
                        }
                    }
                }
            }
        }
    }
}
