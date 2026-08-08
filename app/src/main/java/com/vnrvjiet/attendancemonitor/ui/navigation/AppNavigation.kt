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
import com.vnrvjiet.attendancemonitor.ui.screens.notifications.NotificationsScreen
import com.vnrvjiet.attendancemonitor.ui.screens.setup.SemesterSetupScreen
import com.vnrvjiet.attendancemonitor.ui.screens.settings.AboutScreen
import com.vnrvjiet.attendancemonitor.ui.screens.settings.LicensesScreen
import com.vnrvjiet.attendancemonitor.ui.screens.settings.PrivacyPolicyScreen
import com.vnrvjiet.attendancemonitor.ui.screens.settings.SettingsScreen
import com.vnrvjiet.attendancemonitor.ui.screens.timetable.TimetableSetupScreen
import com.vnrvjiet.attendancemonitor.ui.screens.statistics.StatisticsScreen
import com.vnrvjiet.attendancemonitor.ui.screens.sync.SyncStatusScreen
import com.vnrvjiet.attendancemonitor.util.safeNavigate
import com.vnrvjiet.attendancemonitor.util.safePopBackStack

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScaffold(
                navController = navController,
                currentScreen = Screen.Dashboard,
                onNavigateToSetup = { navController.safeNavigate(Screen.TimetableSetup.route) },
                onNavigateToRestore = { navController.safeNavigate(Screen.Settings.route) },
                onNavigateToLogin = { navController.safeNavigate(Screen.Settings.route) }
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
                onBack = { navController.safePopBackStack() },
                onSettingsClick = { navController.safeNavigate(Screen.Settings.route) }
            )
        }
        composable(Screen.SyncStatus.route) {
            SyncStatusScreen(
                onBack = { navController.safePopBackStack() },
                onNavigateToSetup = { navController.safeNavigate(Screen.SemesterSetup.route) }
            )
        }
        composable(Screen.SemesterSetup.route) {
            SemesterSetupScreen(onComplete = { navController.safePopBackStack() })
        }
        composable(Screen.TimetableSetup.route) {
            val canPop = navController.previousBackStackEntry != null
            TimetableSetupScreen(
                onComplete = { 
                    navController.safePopBackStack(Screen.Dashboard.route, false) 
                },
                onBack = if (canPop) { { navController.safePopBackStack() } } else null
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.safePopBackStack() },
                onManageTimetableClick = { navController.safeNavigate(Screen.TimetableSetup.route) },
                onAboutClick = { navController.safeNavigate(Screen.About.route) },
                onPrivacyPolicyClick = { navController.safeNavigate(Screen.PrivacyPolicy.route) },
                onLicensesClick = { navController.safeNavigate(Screen.Licenses.route) }
            )
        }
        composable(Screen.About.route) {
            AboutScreen(onBack = { navController.safePopBackStack() })
        }
        composable(Screen.PrivacyPolicy.route) {
            PrivacyPolicyScreen(onBack = { navController.safePopBackStack() })
        }
        composable(Screen.Licenses.route) {
            LicensesScreen(onBack = { navController.safePopBackStack() })
        }
    }
}

@Composable
fun DashboardScaffold(
    navController: androidx.navigation.NavHostController,
    currentScreen: Screen,
    onNavigateToSetup: () -> Unit = {},
    onNavigateToRestore: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            DashboardTopBar(
                onSyncClick = { 
                    navController.safeNavigate(Screen.SyncStatus.route) {
                        launchSingleTop = true
                    }
                },
                onNotificationClick = { 
                    navController.safeNavigate(Screen.Notifications.route) {
                        launchSingleTop = true
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentScreen) {
                Screen.Dashboard -> DashboardScreen(
                    onNavigateToSetup = onNavigateToSetup,
                    onNavigateToRestore = onNavigateToRestore,
                    onNavigateToLogin = onNavigateToLogin
                )
                Screen.History -> HistoryScreen()
                Screen.Statistics -> StatisticsScreen()
                else -> DashboardScreen(onNavigateToSetup = {}, onNavigateToRestore = {}, onNavigateToLogin = {})
            }
        }
    }
}
