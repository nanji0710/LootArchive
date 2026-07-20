package com.nanji.lootarchive.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanji.lootarchive.ui.additem.AddItemScreen
import com.nanji.lootarchive.ui.detail.DetailScreen
import com.nanji.lootarchive.ui.home.HomeScreen
import com.nanji.lootarchive.ui.search.SearchScreen
import com.nanji.lootarchive.ui.settings.SettingsScreen
import com.nanji.lootarchive.ui.statistics.StatisticsScreen
import com.nanji.lootarchive.ui.theme.Primary
import com.nanji.lootarchive.ui.theme.TextPrimary
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

enum class MainTab(val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
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

    Scaffold(
        topBar = {
            if (!isSubPage) {
                when (MainTab.entries[selectedTab]) {
                    MainTab.HOME -> HomeTopBar(
                        filterLabel = drawerCategoryFilter?.second,
                        onMenuClick = {},
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
                    containerColor = Primary
                ) {
                    Icon(Icons.Filled.Add, "新增物品")
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
                                subNavController.navigate(when (index) { 0 -> "home"; 1 -> "stats"; else -> "my" }) {
                                    popUpTo("home") { inclusive = false }; launchSingleTop = true
                                }
                            },
                            icon = { Icon(if (selectedTab == index) tab.selectedIcon else tab.unselectedIcon, tab.label) },
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
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(tween(300)) + slideInHorizontally(tween(300)) { it / 4 } },
            exitTransition = { fadeOut(tween(300)) + slideOutHorizontally(tween(300)) { -it / 4 } },
            popEnterTransition = { fadeIn(tween(300)) + slideInHorizontally(tween(300)) { -it / 4 } },
            popExitTransition = { fadeOut(tween(300)) + slideOutHorizontally(tween(300)) { it / 4 } }
        ) {
            composable("home") {
                Box(modifier = Modifier.fillMaxSize()) {
                    HomeScreen(
                        categoryFilter = drawerCategoryFilter,
                        onNavigateToAddItem = { subNavController.navigate("add_item") },
                        onNavigateToDetail = { subNavController.navigate("detail/$it") },
                        onNavigateToSearch = { subNavController.navigate("search") },
                        onNavigateToStats = { selectedTab = 1; subNavController.navigate("stats") { launchSingleTop = true } }
                    )
                    // 悬浮半透明搜索图标
                    IconButton(
                        onClick = { subNavController.navigate("search") },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 4.dp, end = 12.dp)
                            .size(40.dp)
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "搜索",
                            tint = TextPrimary.copy(alpha = 0.35f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
            composable("stats") {
                StatisticsScreen(
                    onNavigateBack = { subNavController.popBackStack() },
                    onNavigateToDetail = { subNavController.navigate("detail/$it") },
                    isTabMode = true
                )
            }
            composable("my") {
                SettingsScreen(
                    onNavigateBack = { subNavController.popBackStack() },
                    isTabMode = true
                )
            }
            composable("add_item?itemId={itemId}",
                arguments = listOf(navArgument("itemId") { type = NavType.LongType; defaultValue = -1L })
            ) { entry ->
                val id = entry.arguments?.getLong("itemId") ?: -1L
                AddItemScreen(editItemId = if (id > 0) id else null, onNavigateBack = { subNavController.popBackStack() })
            }
            composable("detail/{itemId}",
                arguments = listOf(navArgument("itemId") { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong("itemId") ?: return@composable
                DetailScreen(itemId = id, onNavigateBack = { subNavController.popBackStack() }, onNavigateToEdit = { subNavController.navigate("add_item?itemId=$it") })
            }
            composable("search") {
                SearchScreen(onNavigateBack = { subNavController.popBackStack() }, onNavigateToDetail = { subNavController.navigate("detail/$it") })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    filterLabel: String?,
    onMenuClick: () -> Unit,
    onClearFilter: (() -> Unit)?
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("拾物集", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                if (filterLabel != null) {
                    Spacer(Modifier.width(8.dp))
                    AssistChip(onClick = { onClearFilter?.invoke() }, label = { Text(filterLabel, style = MaterialTheme.typography.labelSmall) }, trailingIcon = { Icon(Icons.Filled.Close, null, Modifier.size(14.dp)) })
                }
            }
        },
        navigationIcon = { IconButton(onClick = onMenuClick) { Icon(Icons.Filled.Menu, "分类", tint = TextPrimary) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatsTopBar() {
    TopAppBar(
        title = { Text("资产汇总", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
        actions = { TextButton(onClick = {}) { Text("全部时间", fontSize = 16.sp, color = Primary); Icon(Icons.Filled.ArrowDropDown, null, tint = Primary) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyTopBar() {
    TopAppBar(title = { Text("我的", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary) })
}
