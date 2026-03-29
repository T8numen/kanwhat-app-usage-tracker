package com.abhinavvaidya.appusagetracker.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.abhinavvaidya.appusagetracker.R

sealed class BottomNavItem(
    val route: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector
) {
    data object Home : BottomNavItem("home", R.string.nav_today, Icons.Filled.Home)
    data object Weekly : BottomNavItem("weekly", R.string.nav_weekly, Icons.Filled.DateRange)
    data object Settings : BottomNavItem("settings", R.string.nav_widget, Icons.Filled.Settings)
}
