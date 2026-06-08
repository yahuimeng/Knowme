package com.knowme.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.knowme.app.ui.screens.AskScreen
import com.knowme.app.ui.screens.SettingsScreen
import com.knowme.app.ui.screens.TimelineScreen
import com.knowme.app.ui.screens.TodayScreen
import com.knowme.app.ui.screens.TodoScreen

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Today("today", "今日", Icons.Outlined.Home),
    Timeline("timeline", "时间线", Icons.Outlined.Timeline),
    Todo("todo", "待办", Icons.Outlined.CheckCircle),
    Ask("ask", "问问", Icons.AutoMirrored.Outlined.Chat),
    Mine("mine", "我的", Icons.Outlined.Person),
}

@Composable
fun KnowmeRoot(vm: MainViewModel) {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStack by navController.currentBackStackEntryAsState()
                val current = backStack?.destination
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = current?.hierarchy?.any { it.route == tab.route } == true,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Today.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Tab.Today.route) { TodayScreen(vm) }
            composable(Tab.Timeline.route) { TimelineScreen(vm) }
            composable(Tab.Todo.route) { TodoScreen(vm) }
            composable(Tab.Ask.route) { AskScreen(vm) }
            composable(Tab.Mine.route) { SettingsScreen(vm) }
        }
    }
}
