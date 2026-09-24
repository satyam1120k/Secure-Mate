package com.example.securemate.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String) {
    object Dashboard : Screen("dashboard", "Dashboard")
    object AppList : Screen("apps", "App Security")
    object AppDetail : Screen("app_detail/{packageName}", "App Details") {
        fun createRoute(packageName: String): String = "app_detail/$packageName"
    }
    object Scan : Screen("scan", "Security Scan")
    object SecurityTools : Screen("security_tools", "Security Hub")
    object Permissions : Screen("permissions", "Permissions")
    object Phishing : Screen("phishing", "Phishing Checker")
    object Passwords : Screen("passwords", "Password Safety")
    object Checklist : Screen("checklist", "Security Checklist")
    object Recommendations : Screen("recommendations", "Recommendations")
    object DeviceSecurity : Screen("device_security", "Device Security")
    object History : Screen("history", "Scan History")
    object SettingsScreen : Screen("settings", "Settings")
    object Privacy : Screen("privacy", "Privacy Center")
}

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : BottomNavItem(
        route = Screen.Dashboard.route,
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    object Apps : BottomNavItem(
        route = Screen.AppList.route,
        title = "Apps",
        selectedIcon = Icons.Filled.Apps,
        unselectedIcon = Icons.Outlined.Apps
    )

    object Scan : BottomNavItem(
        route = Screen.Scan.route,
        title = "Scan",
        selectedIcon = Icons.Filled.Shield,
        unselectedIcon = Icons.Outlined.Shield
    )

    object Security : BottomNavItem(
        route = Screen.SecurityTools.route,
        title = "Security",
        selectedIcon = Icons.Filled.Security,
        unselectedIcon = Icons.Outlined.Security
    )

    object Settings : BottomNavItem(
        route = Screen.SettingsScreen.route,
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
}
