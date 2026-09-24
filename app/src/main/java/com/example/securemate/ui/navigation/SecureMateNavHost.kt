package com.example.securemate.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.securemate.SecureMateAppContainer
import com.example.securemate.ui.components.SecureMateBottomBar
import com.example.securemate.ui.screens.AppDetailScreen
import com.example.securemate.ui.screens.AppListScreen
import com.example.securemate.ui.screens.DashboardScreen
import com.example.securemate.ui.screens.DeviceSecurityScreen
import com.example.securemate.ui.screens.HistoryScreen
import com.example.securemate.ui.screens.PasswordCheckerScreen
import com.example.securemate.ui.screens.PermissionScreen
import com.example.securemate.ui.screens.PhishingCheckerScreen
import com.example.securemate.ui.screens.PrivacyScreen
import com.example.securemate.ui.screens.RecommendationScreen
import com.example.securemate.ui.screens.ScanScreen
import com.example.securemate.ui.screens.SecurityChecklistScreen
import com.example.securemate.ui.screens.SecurityToolsHubScreen
import com.example.securemate.ui.screens.SettingsScreen
import com.example.securemate.ui.viewmodel.AppSecurityViewModel
import com.example.securemate.ui.viewmodel.DashboardViewModel
import com.example.securemate.ui.viewmodel.HistoryViewModel
import com.example.securemate.ui.viewmodel.PasswordViewModel
import com.example.securemate.ui.viewmodel.PhishingViewModel
import com.example.securemate.ui.viewmodel.ScanViewModel
import com.example.securemate.ui.viewmodel.SecureMateViewModelFactory
import com.example.securemate.ui.viewmodel.SettingsViewModel

@Composable
fun SecureMateApp(
    container: SecureMateAppContainer,
    navController: NavHostController = rememberNavController()
) {
    val factory = SecureMateViewModelFactory(
        assessmentRepository = container.assessmentRepository,
        scanHistoryRepository = container.scanHistoryRepository,
        preferences = container.securityPreferences,
        phishingDetector = container.phishingDetector
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Show bottom bar on primary top-level tabs
    val showBottomBar = currentRoute in listOf(
        Screen.Dashboard.route,
        Screen.AppList.route,
        Screen.Scan.route,
        Screen.SecurityTools.route,
        Screen.SettingsScreen.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                SecureMateBottomBar(
                    currentRoute = currentRoute,
                    onNavigateToRoute = { route ->
                        if (currentRoute != route) {
                            navController.navigate(route) {
                                popUpTo(Screen.Dashboard.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. Dashboard
            composable(Screen.Dashboard.route) {
                val dashboardViewModel: DashboardViewModel = viewModel(factory = factory)
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigate = { route -> navController.navigate(route) }
                )
            }

            // 2. Apps List
            composable(Screen.AppList.route) {
                val appViewModel: AppSecurityViewModel = viewModel(factory = factory)
                AppListScreen(
                    viewModel = appViewModel,
                    onNavigateToDetail = { pkg ->
                        navController.navigate(Screen.AppDetail.createRoute(pkg))
                    }
                )
            }

            // 3. App Details
            composable(
                route = Screen.AppDetail.route,
                arguments = listOf(navArgument("packageName") { type = NavType.StringType })
            ) { backStackEntry ->
                val packageName = backStackEntry.arguments?.getString("packageName")
                val dashboardViewModel: DashboardViewModel = viewModel(factory = factory)
                val assessmentState by dashboardViewModel.assessmentState.collectAsStateWithLifecycle()
                val app = assessmentState.scannedApps.find { it.packageName == packageName }

                AppDetailScreen(
                    app = app,
                    onBack = { navController.popBackStack() }
                )
            }

            // 4. Scan Screen
            composable(Screen.Scan.route) {
                val scanViewModel: ScanViewModel = viewModel(factory = factory)
                ScanScreen(
                    viewModel = scanViewModel,
                    onNavigate = { route -> navController.navigate(route) },
                    onBack = { navController.popBackStack() }
                )
            }

            // 5. Security Hub
            composable(Screen.SecurityTools.route) {
                SecurityToolsHubScreen(
                    onNavigate = { route -> navController.navigate(route) }
                )
            }

            // 6. Permissions Screen
            composable(Screen.Permissions.route) {
                val dashboardViewModel: DashboardViewModel = viewModel(factory = factory)
                val assessmentState by dashboardViewModel.assessmentState.collectAsStateWithLifecycle()
                PermissionScreen(
                    permissions = assessmentState.analyzedPermissions,
                    onBack = { navController.popBackStack() }
                )
            }

            // 7. Phishing URL Screen
            composable(Screen.Phishing.route) {
                val phishingViewModel: PhishingViewModel = viewModel(factory = factory)
                PhishingCheckerScreen(
                    viewModel = phishingViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // 8. Password Screen
            composable(Screen.Passwords.route) {
                val passwordViewModel: PasswordViewModel = viewModel(factory = factory)
                PasswordCheckerScreen(
                    viewModel = passwordViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // 9. Checklist Screen
            composable(Screen.Checklist.route) {
                val dashboardViewModel: DashboardViewModel = viewModel(factory = factory)
                val assessmentState by dashboardViewModel.assessmentState.collectAsStateWithLifecycle()
                SecurityChecklistScreen(
                    items = assessmentState.checklistItems,
                    onBack = { navController.popBackStack() }
                )
            }

            // 10. Recommendations Screen
            composable(Screen.Recommendations.route) {
                val dashboardViewModel: DashboardViewModel = viewModel(factory = factory)
                val assessmentState by dashboardViewModel.assessmentState.collectAsStateWithLifecycle()
                RecommendationScreen(
                    recommendations = assessmentState.recommendations,
                    onNavigateToRoute = { route -> navController.navigate(route) },
                    onBack = { navController.popBackStack() }
                )
            }

            // 11. Device Security Screen
            composable(Screen.DeviceSecurity.route) {
                val dashboardViewModel: DashboardViewModel = viewModel(factory = factory)
                val assessmentState by dashboardViewModel.assessmentState.collectAsStateWithLifecycle()
                DeviceSecurityScreen(
                    deviceInfo = assessmentState.deviceInfo,
                    onBack = { navController.popBackStack() }
                )
            }

            // 12. History Screen
            composable(Screen.History.route) {
                val historyViewModel: HistoryViewModel = viewModel(factory = factory)
                HistoryScreen(
                    viewModel = historyViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // 13. Settings Screen
            composable(Screen.SettingsScreen.route) {
                val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigate = { route -> navController.navigate(route) }
                )
            }

            // 14. Privacy Screen
            composable(Screen.Privacy.route) {
                PrivacyScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
