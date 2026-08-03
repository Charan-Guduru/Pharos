package com.vnrvjiet.attendancemonitor.util

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder

/**
 * Extension to prevent duplicate navigation events if multiple clicks happen rapidly.
 */
fun NavController.safeNavigate(
    route: String,
    builder: NavOptionsBuilder.() -> Unit = {}
) {
    val currentRoute = currentDestination?.route
    if (currentRoute != route) {
        navigate(route, builder)
    }
}

/**
 * Extension to prevent multiple popBackStack calls if multiple clicks happen rapidly.
 */
fun NavController.safePopBackStack() {
    if (previousBackStackEntry != null) {
        popBackStack()
    }
}

/**
 * Extension to prevent multiple popBackStack calls to a specific route.
 */
fun NavController.safePopBackStack(route: String, inclusive: Boolean) {
    if (previousBackStackEntry != null) {
        popBackStack(route, inclusive)
    }
}
