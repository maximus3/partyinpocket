package com.m3games.partyinpocket.presentation.navigation

import androidx.navigation.NavController

/**
 * Safe popBackStack that checks if navigation is possible
 */
fun NavController.safePopBackStack(): Boolean {
    return if (previousBackStackEntry != null) {
        popBackStack()
    } else {
        false
    }
}

/**
 * Safe navigate that prevents double navigation
 */
fun NavController.safeNavigate(
    route: String,
    builder: (androidx.navigation.NavOptionsBuilder.() -> Unit)? = null
) {
    // Check if we're not already on this route
    if (currentDestination?.route != route) {
        if (builder != null) {
            navigate(route, builder)
        } else {
            navigate(route)
        }
    }
}
