package com.vnrvjiet.attendancemonitor.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vnrvjiet.attendancemonitor.ui.components.BottomNavigationBar
import com.vnrvjiet.attendancemonitor.ui.components.DashboardTopBar
import com.vnrvjiet.attendancemonitor.ui.screens.dashboard.DashboardScreen
import com.vnrvjiet.attendancemonitor.ui.screens.history.HistoryScreen
import com.vnrvjiet.attendancemonitor.ui.screens.login.LoginScreen
import com.vnrvjiet.attendancemonitor.ui.screens.notifications.NotificationsScreen
import com.vnrvjiet.attendancemonitor.ui.screens.settings.SettingsScreen
import com.vnrvjiet.attendancemonitor.ui.screens.statistics.StatisticsScreen
import com.vnrvjiet.attendancemonitor.ui.screens.sync.SyncStatusScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen()
        }
        composable(Screen.Dashboard.route) {
            DashboardScaffold(navController, Screen.Dashboard)
        }
        composable(Screen.History.route) {
            DashboardScaffold(navController, Screen.History)
        }
        composable(Screen.Statistics.route) {
            DashboardScaffold(navController, Screen.Statistics)
        }
        composable(Screen.Notifications.route) {
            NotificationsScreen(
                onBack = { navController.popBackStack() },
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.SyncStatus.route) {
            SyncStatusScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
fun DashboardScaffold(navController: androidx.navigation.NavHostController, currentScreen: Screen) {
    Scaffold(
        topBar = {
            DashboardTopBar(
                onSyncClick = { navController.navigate(Screen.SyncStatus.route) },
                onNotificationClick = { navController.navigate(Screen.Notifications.route) }
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentScreen) {
                Screen.Dashboard -> DashboardScreen()
                Screen.History -> HistoryScreen()
                Screen.Statistics -> StatisticsScreen()
                else -> DashboardScreen()
            }
        }
    }
}
