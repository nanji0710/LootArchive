package com.nanji.lootarchive.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nanji.lootarchive.ui.home.HomeScreen
import com.nanji.lootarchive.ui.additem.AddItemScreen
import com.nanji.lootarchive.ui.detail.DetailScreen
import com.nanji.lootarchive.ui.category.CategoryScreen
import com.nanji.lootarchive.ui.statistics.StatisticsScreen
import com.nanji.lootarchive.ui.backup.BackupScreen
import com.nanji.lootarchive.ui.settings.SettingsScreen
import com.nanji.lootarchive.ui.search.SearchScreen

@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        // 主页
        composable(Screen.Home.route) {
            HomeScreen(
                categoryFilter = null,
                onNavigateToAddItem = { navController.navigate(Screen.AddItem.createRoute()) },
                onNavigateToDetail = { itemId -> navController.navigate(Screen.Detail.createRoute(itemId)) },
                onNavigateToSearch = { navController.navigate(Screen.Search.route) }
            )
        }

        // 新增/编辑物品
        composable(
            route = Screen.AddItem.route,
            arguments = listOf(
                navArgument("itemId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getLong("itemId") ?: -1L
            AddItemScreen(
                editItemId = if (itemId > 0) itemId else null,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 物品详情
        composable(
            route = Screen.Detail.route,
            arguments = listOf(
                navArgument("itemId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getLong("itemId") ?: return@composable
            DetailScreen(
                itemId = itemId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { id ->
                    navController.navigate(Screen.AddItem.createRoute(id))
                }
            )
        }

        // 分类管理
        composable(Screen.Category.route) {
            CategoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 统计图表
        composable(Screen.Statistics.route) {
            StatisticsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { itemId -> navController.navigate(Screen.Detail.createRoute(itemId)) }
            )
        }

        // 备份与恢复
        composable(Screen.Backup.route) {
            BackupScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 设置
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 搜索
        composable(Screen.Search.route) {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { itemId -> navController.navigate(Screen.Detail.createRoute(itemId)) }
            )
        }
    }
}
