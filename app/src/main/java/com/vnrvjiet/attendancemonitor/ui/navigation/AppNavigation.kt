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
import com.vnrvjiet.attendancemonitor.ui.screens.setup.SemesterSetupScreen
import com.vnrvjiet.attendancemonitor.ui.screens.settings.*
import com.vnrvjiet.attendancemonitor.ui.screens.timetable.TimetableSetupScreen
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
            DashboardScaffold(
                navController = navController,
                currentScreen = Screen.Dashboard,
                onNavigateToSetup = { navController.navigate(Screen.TimetableSetup.route) }
            )
        }
        composable(Screen.History.route) {
            DashboardScaffold(navController, Screen.History, {})
        }
        composable(Screen.Statistics.route) {
            DashboardScaffold(navController, Screen.Statistics, {})
        }
        composable(Screen.Notifications.route) {
            NotificationsScreen(
                onBack = { navController.popBackStack() },
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.SyncStatus.route) {
            SyncStatusScreen(
                onBack = { navController.popBackStack() },
                onNavigateToSetup = { navController.navigate(Screen.SemesterSetup.route) }
            )
        }
        composable(Screen.SemesterSetup.route) {
            SemesterSetupScreen(onComplete = { navController.popBackStack() })
        }
        composable(Screen.TimetableSetup.route) {
            val canPop = navController.previousBackStackEntry != null
            TimetableSetupScreen(
                onComplete = { 
                    navController.popBackStack(Screen.Dashboard.route, false) 
                },
                onBack = if (canPop) { { navController.popBackStack() } } else null
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onManageTimetableClick = { navController.navigate(Screen.TimetableSetup.route) },
                onAboutClick = { navController.navigate(Screen.About.route) },
                onPrivacyPolicyClick = { navController.navigate(Screen.PrivacyPolicy.route) },
                onLicensesClick = { navController.navigate(Screen.Licenses.route) }
            )
        }
        composable(Screen.About.route) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.PrivacyPolicy.route) {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Licenses.route) {
            LicensesScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
fun DashboardScaffold(
    navController: androidx.navigation.NavHostController,
    currentScreen: Screen,
    onNavigateToSetup: () -> Unit = {}
) {
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
                Screen.Dashboard -> DashboardScreen(onNavigateToSetup = onNavigateToSetup)
                Screen.History -> HistoryScreen()
                Screen.Statistics -> StatisticsScreen()
                else -> DashboardScreen(onNavigateToSetup = {})
            }
        }
    }
}
