package com.abhinavvaidya.appusagetracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.abhinavvaidya.appusagetracker.ui.about.AboutScreen
import com.abhinavvaidya.appusagetracker.ui.home.HomeScreen
import com.abhinavvaidya.appusagetracker.ui.permission.PermissionScreen
import com.abhinavvaidya.appusagetracker.ui.settings.WidgetSettingsScreen
import com.abhinavvaidya.appusagetracker.ui.theme.AppUsageTrackerTheme
import com.abhinavvaidya.appusagetracker.ui.theme.DarkBackground
import com.abhinavvaidya.appusagetracker.ui.weekly.WeeklyScreen
import com.abhinavvaidya.appusagetracker.viewmodel.HomeViewModel

private object AppRoute {
    const val Home = "home"
    const val Weekly = "weekly"
    const val Settings = "settings"
    const val About = "about"
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContent {
            AppUsageTrackerTheme {
                UsageFlowApp()
            }
        }
    }
}

@Composable
fun UsageFlowApp() {
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = viewModel()
    val uiState by homeViewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                homeViewModel.onResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (!uiState.hasPermission) {
        PermissionScreen(
            onPermissionGranted = {
                homeViewModel.onResume()
            }
        )
    } else {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkBackground
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = AppRoute.Home
                ) {
                    composable(AppRoute.Home) {
                        HomeScreen(
                            viewModel = homeViewModel,
                            onOpenSettings = {
                                navController.navigate(AppRoute.Settings) {
                                    launchSingleTop = true
                                }
                            },
                            onOpenWeekly = {
                                navController.navigate(AppRoute.Weekly) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable(AppRoute.Weekly) {
                        WeeklyScreen(
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable(AppRoute.Settings) {
                        WidgetSettingsScreen(
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            onNavigateToAbout = {
                                navController.navigate(AppRoute.About)
                            }
                        )
                    }

                    composable(AppRoute.About) {
                        AboutScreen(
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}
