package com.nanji.lootarchive.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
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
    var drawerCategoryFilter by remember { mutableStateOf<Pair<Long, String>?>(null) }

    val subNavController = rememberNavController()
    val navBackStackEntry by subNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isSubPage = currentRoute != null && currentRoute !in listOf("home", "stats", "my")

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // 切 Tab 时关闭抽屉
    LaunchedEffect(selectedTab) {
        if (drawerState.isOpen) drawerState.close()
    }

    // 进入子页面时关闭抽屉
    LaunchedEffect(isSubPage) {
        if (isSubPage && drawerState.isOpen) drawerState.close()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = selectedTab == 0 && !isSubPage,
        drawerContent = {
            ModalDrawerSheet {
                CategoryDrawer(
                    selectedFilter = drawerCategoryFilter,
                    onCategorySelected = { categoryId, categoryName ->
                        drawerCategoryFilter = if (categoryId == -1L) null
                        else Pair(categoryId, categoryName)
                        selectedTab = 0
                    },
                    onClose = { /* drawerState.close() handled by scope */ }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (!isSubPage) {
                    when (MainTab.entries[selectedTab]) {
                        MainTab.HOME -> HomeTopBar(
                            filterLabel = drawerCategoryFilter?.second,
                            onMenuClick = {
                                scope.launch {
                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                }
                            },
                            onSearchClick = { subNavController.navigate("search") },
                            onClearFilter = { drawerCategoryFilter = null }
                        )
                        MainTab.STATS -> StatsTopBar()
                        MainTab.MY -> MyTopBar()
                    }
                }
            },
            floatingActionButton = {
                if (selectedTab == 0 && !isSubPage) {
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
                if (!isSubPage) {
                    NavigationBar {
                        MainTab.entries.forEachIndexed { index, tab ->
                            NavigationBarItem(
                                selected = selectedTab == index,
                                onClick = {
                                    selectedTab = index
                                    subNavController.navigate(
                                        when (index) {
                                            0 -> "home"; 1 -> "stats"; 2 -> "my"; else -> "home"
                                        }
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
            NavHost(
                navController = subNavController,
                startDestination = "home",
                modifier = Modifier.padding(padding)
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
                        onNavigateToDetail = { id -> subNavController.navigate("detail/$id") },
                        isTabMode = true
                    )
                }
                composable("my") {
                    SettingsScreen(
                        onNavigateBack = { subNavController.popBackStack() },
                        isTabMode = true
                    )
                }
                composable(
                    "add_item?itemId={itemId}",
                    arguments = listOf(navArgument("itemId") { type = NavType.LongType; defaultValue = -1L })
                ) { entry ->
                    val itemId = entry.arguments?.getLong("itemId") ?: -1L
                    AddItemScreen(
                        editItemId = if (itemId > 0) itemId else null,
                        onNavigateBack = { subNavController.popBackStack() }
                    )
                }
                composable(
                    "detail/{itemId}",
                    arguments = listOf(navArgument("itemId") { type = NavType.LongType })
                ) { entry ->
                    val itemId = entry.arguments?.getLong("itemId") ?: return@composable
                    DetailScreen(
                        itemId = itemId,
                        onNavigateBack = { subNavController.popBackStack() },
                        onNavigateToEdit = { id -> subNavController.navigate("add_item?itemId=$id") }
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

