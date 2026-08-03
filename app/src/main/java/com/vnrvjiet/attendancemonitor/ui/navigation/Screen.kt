package com.vnrvjiet.attendancemonitor.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Login : Screen("login", "Login", Icons.Default.Login)
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object History : Screen("history", "History", Icons.Default.History)
    object Statistics : Screen("statistics", "Statistics", Icons.Default.Info)
    object Notifications : Screen("notifications", "Notifications", Icons.Default.Notifications)
    object SyncStatus : Screen("sync_status", "Sync Status", Icons.Default.Sync)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object SemesterSetup : Screen("semester_setup", "Semester Setup", Icons.Default.ListAlt)
    object TimetableSetup : Screen("timetable_setup", "Timetable Setup", Icons.Default.CalendarToday)
    object About : Screen("about", "About", Icons.Default.Info)
    object PrivacyPolicy : Screen("privacy_policy", "Privacy Policy", Icons.Default.PrivacyTip)
    object Licenses : Screen("licenses", "Open Source Licenses", Icons.Default.Description)
}
