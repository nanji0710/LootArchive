package com.nanji.lootarchive.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nanji.lootarchive.ui.additem.AddItemScreen
import com.nanji.lootarchive.ui.component.CategoryDrawer
import com.nanji.lootarchive.ui.detail.DetailScreen
import com.nanji.lootarchive.ui.home.HomeScreen
import com.nanji.lootarchive.ui.search.SearchScreen
import com.nanji.lootarchive.ui.settings.SettingsScreen
import com.nanji.lootarchive.ui.statistics.StatisticsScreen

enum class MainTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("首页", Icons.Filled.Home, Icons.Outlined.Home),
    STATS("资产汇总", Icons.Filled.PieChart, Icons.Outlined.PieChart),
    MY("我的", Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun MainScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
    // 抽屉选中的分类 ID（null = 全部）
    var drawerCategoryFilter by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var showDrawer by remember { mutableStateOf(false) }

    val subNavController = rememberNavController()

    ModalNavigationDrawer(
        drawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
        gesturesEnabled = selectedTab == 0,
        drawerContent = {
            ModalDrawerSheet {
                CategoryDrawer(
                    selectedFilter = drawerCategoryFilter,
                    onCategorySelected = { categoryId, categoryName ->
                        drawerCategoryFilter = if (categoryId == -1L) null
                        else Pair(categoryId, categoryName)
                        selectedTab = 0
                        showDrawer = false
                    },
                    onClose = { showDrawer = false }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                // 子页面（非Tab）不显示 TopBar，由各自页面管理
                val currentRoute = subNavController.currentBackStackEntry?.destination?.route
                if (currentRoute == null || currentRoute in listOf("home", "stats", "my")) {
                    when (MainTab.entries[selectedTab]) {
                        MainTab.HOME -> HomeTopBar(
                            filterLabel = drawerCategoryFilter?.second,
                            onMenuClick = { showDrawer = true },
                            onSearchClick = {
                                subNavController.navigate("search")
                            },
                            onClearFilter = { drawerCategoryFilter = null }
                        )
                        MainTab.STATS -> StatsTopBar()
                        MainTab.MY -> MyTopBar()
                    }
                }
            },
            floatingActionButton = {
                if (selectedTab == 0) {
                    FloatingActionButton(
                        onClick = { subNavController.navigate("add_item") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "新增物品")
                    }
                }
            },
            bottomBar = {
                // 子页面隐藏底部 Tab
                val currentRoute = subNavController.currentBackStackEntry?.destination?.route
                if (currentRoute == null || currentRoute in listOf("home", "stats", "my")) {
                    NavigationBar {
                        MainTab.entries.forEachIndexed { index, tab ->
                            NavigationBarItem(
                                selected = selectedTab == index,
                                onClick = {
                                    selectedTab = index
                                    subNavController.navigate(
                                        when (index) { 0 -> "home"; 1 -> "stats"; 2 -> "my"; else -> "home" }
                                    ) {
                                        popUpTo("home") { inclusive = false }
                                        launchSingleTop = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        if (selectedTab == index) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                NavHost(
                    navController = subNavController,
                    startDestination = "home"
                ) {
                    composable("home") {
                        HomeScreen(
                            categoryFilter = drawerCategoryFilter,
                            onNavigateToAddItem = { subNavController.navigate("add_item") },
                            onNavigateToDetail = { id -> subNavController.navigate("detail/$id") },
                            onNavigateToSearch = { subNavController.navigate("search") }
                        )
                    }
                    composable("stats") {
                        StatisticsScreen(
                            onNavigateBack = { subNavController.popBackStack() },
                            onNavigateToDetail = { id -> subNavController.navigate("detail/$id") }
                        )
                    }
                    composable("my") {
                        SettingsScreen(
                            onNavigateBack = { subNavController.popBackStack() }
                        )
                    }
                    composable("add_item?itemId={itemId}",
                        arguments = listOf(navArgument("itemId") { type = NavType.LongType; defaultValue = -1L })
                    ) { entry ->
                        val itemId = entry.arguments?.getLong("itemId") ?: -1L
                        AddItemScreen(
                            editItemId = if (itemId > 0) itemId else null,
                            onNavigateBack = { subNavController.popBackStack() }
                        )
                    }
                    composable("detail/{itemId}",
                        arguments = listOf(navArgument("itemId") { type = NavType.LongType })
                    ) { entry ->
                        val itemId = entry.arguments?.getLong("itemId") ?: return@composable
                        DetailScreen(
                            itemId = itemId,
                            onNavigateBack = { subNavController.popBackStack() },
                            onNavigateToEdit = { id ->
                                subNavController.navigate("add_item?itemId=$id")
                            }
                        )
                    }
                    composable("search") {
                        SearchScreen(
                            onNavigateBack = { subNavController.popBackStack() },
                            onNavigateToDetail = { id -> subNavController.navigate("detail/$id") }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    filterLabel: String?,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    onClearFilter: (() -> Unit)?
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("拾物集")
                if (filterLabel != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    AssistChip(
                        onClick = { onClearFilter?.invoke() },
                        label = { Text(filterLabel, style = MaterialTheme.typography.labelSmall) },
                        trailingIcon = {
                            Icon(Icons.Filled.Close, null, modifier = Modifier.size(14.dp))
                        }
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Filled.Menu, contentDescription = "分类抽屉")
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Filled.Search, contentDescription = "搜索")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatsTopBar() {
    TopAppBar(title = { Text("资产汇总") })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyTopBar() {
    TopAppBar(title = { Text("我的") })
}
