package com.nanji.lootarchive.ui.navigation

/**
 * 导航路由定义
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object AddItem : Screen("add_item?itemId={itemId}") {
        fun createRoute(itemId: Long? = null): String =
            if (itemId != null) "add_item?itemId=$itemId" else "add_item"
    }
    data object Detail : Screen("detail/{itemId}") {
        fun createRoute(itemId: Long): String = "detail/$itemId"
    }
    data object Category : Screen("category")
    data object Statistics : Screen("statistics")
    data object Backup : Screen("backup")
    data object Settings : Screen("settings")
    data object Search : Screen("search")
}
