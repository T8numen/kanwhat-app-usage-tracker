package com.abhinavvaidya.appusagetracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomNavItem("home", "Today", Icons.Filled.Home)
    object Weekly : BottomNavItem("weekly", "Weekly", Icons.Filled.DateRange)
    object Settings : BottomNavItem("settings", "Widget", Icons.Filled.Settings)
}

