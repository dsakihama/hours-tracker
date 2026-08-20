package com.dean.hourstracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dean.hourstracker.data.HoursRepository
import com.dean.hourstracker.ui.calendar.CalendarScreen
import com.dean.hourstracker.ui.calendar.CalendarViewModel
import com.dean.hourstracker.ui.history.HistoryScreen
import com.dean.hourstracker.ui.history.HistoryViewModel
import com.dean.hourstracker.ui.log.LogScreen
import com.dean.hourstracker.ui.log.LogViewModel
import com.dean.hourstracker.ui.projects.ProjectsScreen
import com.dean.hourstracker.ui.projects.ProjectsViewModel
import com.dean.hourstracker.ui.reports.ReportsScreen
import com.dean.hourstracker.ui.reports.ReportsViewModel
import com.dean.hourstracker.ui.settings.SettingsScreen
import com.dean.hourstracker.ui.theme.HoursTrackerTheme

sealed class Screen(val route: String) {
    object Log      : Screen("log")
    object History  : Screen("history")
    object Calendar : Screen("calendar")
    object Reports  : Screen("reports")
    object Projects : Screen("projects")
    object Settings : Screen("settings")
}

private data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Log,      "Log",      Icons.Outlined.EditNote),
    BottomNavItem(Screen.History,  "History",  Icons.Outlined.History),
    BottomNavItem(Screen.Calendar, "Calendar", Icons.Outlined.CalendarMonth),
    BottomNavItem(Screen.Reports,  "Reports",  Icons.Outlined.BarChart),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HoursTrackerTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
private fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomNavItems.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
    }

    val repository: HoursRepository =
        (LocalContext.current.applicationContext as HoursTrackerApp).container.repository

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy
                            ?.any { it.route == item.screen.route } == true
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Log.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Log.route) {
                val vm: LogViewModel = viewModel(factory = LogViewModel.Factory(repository))
                LogScreen(
                    viewModel = vm,
                    onNavigateToProjects = { navController.navigate(Screen.Projects.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                )
            }
            composable(Screen.History.route) {
                val vm: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory(repository))
                HistoryScreen(viewModel = vm)
            }
            composable(Screen.Calendar.route) {
                val vm: CalendarViewModel = viewModel(factory = CalendarViewModel.Factory(repository))
                CalendarScreen(viewModel = vm)
            }
            composable(Screen.Reports.route) {
                val vm: ReportsViewModel = viewModel(factory = ReportsViewModel.Factory(repository))
                ReportsScreen(viewModel = vm)
            }
            composable(Screen.Projects.route) {
                val vm: ProjectsViewModel = viewModel(factory = ProjectsViewModel.Factory(repository))
                ProjectsScreen(viewModel = vm, onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
