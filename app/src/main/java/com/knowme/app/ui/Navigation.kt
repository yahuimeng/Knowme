package com.knowme.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

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
    val hazeState = rememberHazeState()

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // 内容铺满（含底栏下方），作为模糊源；顶部留状态栏
        NavHost(
            navController = navController,
            startDestination = Tab.Today.route,
            modifier = Modifier.fillMaxSize().statusBarsPadding().hazeSource(hazeState),
        ) {
            composable(Tab.Today.route) { TodayScreen(vm) }
            composable(Tab.Timeline.route) { TimelineScreen(vm) }
            composable(Tab.Todo.route) { TodoScreen(vm) }
            composable(Tab.Ask.route) { AskScreen(vm) }
            composable(Tab.Mine.route) { SettingsScreen(vm) }
        }

        // 磨砂玻璃底部导航：内容从下方穿过、被真实模糊
        val surface = MaterialTheme.colorScheme.surface
        NavigationBar(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .hazeEffect(state = hazeState) {
                    blurRadius = 28.dp
                    tints = listOf(HazeTint(surface.copy(alpha = 0.6f)))
                },
        ) {
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
    }
}
