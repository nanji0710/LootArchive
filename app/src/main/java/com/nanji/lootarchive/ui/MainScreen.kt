package com.nanji.lootarchive.ui

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
import com.nanji.lootarchive.ui.backup.BackupScreen
import com.nanji.lootarchive.ui.category.CategoryScreen
import com.nanji.lootarchive.ui.detail.DetailScreen
import com.nanji.lootarchive.ui.home.HomeScreen
import com.nanji.lootarchive.ui.search.SearchScreen
import com.nanji.lootarchive.ui.settings.SettingsScreen
import com.nanji.lootarchive.ui.statistics.StatisticsScreen
import com.nanji.lootarchive.ui.theme.Primary
import com.nanji.lootarchive.ui.theme.TextPrimary

enum class MainTab(val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    HOME("首页", Icons.Filled.Home, Icons.Outlined.Home),
    STATS("资产汇总", Icons.Filled.PieChart, Icons.Outlined.PieChart),
    MY("我的", Icons.Filled.Person, Icons.Outlined.Person)
}

// 简易页面路由（替代 NavHost，根除闪退）
private object Route { const val HOME="home"; const val STATS="stats"; const val MY="my"; const val ADD="add"; const val DETAIL="detail"; const val SEARCH="search"; const val SETTINGS="settings"; const val CATEGORY="category"; const val BACKUP="backup" }

@Composable
fun MainScreen() {
    var currentTab by remember { mutableIntStateOf(0) }
    var currentRoute by remember { mutableStateOf(Route.HOME) }
    var detailItemId by remember { mutableStateOf(0L) }
    var editItemId by remember { mutableStateOf<Long?>(null) }
    var drawerCategoryFilter by remember { mutableStateOf<Pair<Long, String>?>(null) }
    val backStack = remember { mutableListOf<String>() }

    fun navigate(route: String) { backStack.add(currentRoute); currentRoute = route }
    fun goBack() { if (backStack.isNotEmpty()) currentRoute = backStack.removeLast() }
    fun switchTab(tab: Int) {
        currentTab = tab
        backStack.clear()
        currentRoute = when(tab) { 0->Route.HOME; 1->Route.STATS; 2->Route.MY; else->Route.HOME }
    }

    val isSubPage = currentRoute !in listOf(Route.HOME, Route.STATS, Route.MY)

    Scaffold(
        topBar = {
            if (!isSubPage) {
                when (currentRoute) {
                    Route.HOME -> { /* 首页无 TopBar，悬浮按钮替代 */ }
                    Route.STATS -> StatsTopBar()
                    Route.MY -> MyTopBar()
                }
            }
        },
        floatingActionButton = {
            if (currentRoute == Route.HOME) {
                FloatingActionButton(onClick = { navigate(Route.ADD) }, containerColor = Primary) {
                    Icon(Icons.Filled.Add, "新增物品")
                }
            }
        },
        bottomBar = {
            if (!isSubPage) {
                NavigationBar {
                    MainTab.entries.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = currentTab == index,
                            onClick = { switchTab(index) },
                            icon = { Icon(if(currentTab==index)tab.selectedIcon else tab.unselectedIcon, tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (currentRoute) {
                Route.HOME -> {
                    Box(Modifier.fillMaxSize()) {
                        HomeScreen(
                            categoryFilter = drawerCategoryFilter,
                            onNavigateToAddItem = { navigate(Route.ADD) },
                            onNavigateToDetail = { detailItemId = it; navigate(Route.DETAIL) },
                            onNavigateToSearch = { navigate(Route.SEARCH) },
                            onNavigateToStats = { switchTab(1) },
                            onNavigateToCategory = { navigate(Route.CATEGORY) },
                            onExportExcel = { navigate(Route.BACKUP) },
                            onImportExcel = { navigate(Route.BACKUP) },
                            onBackupData = { navigate(Route.BACKUP) }
                        )
                        // 悬浮菜单（左上）
                        IconButton(onClick={}, modifier=Modifier.align(Alignment.TopStart).padding(top=4.dp,start=8.dp).size(40.dp)) {
                            Icon(Icons.Filled.Menu,"菜单",tint=TextPrimary.copy(alpha=0.30f),modifier=Modifier.size(26.dp))
                        }
                        // 悬浮搜索（右上）
                        IconButton(onClick={navigate(Route.SEARCH)}, modifier=Modifier.align(Alignment.TopEnd).padding(top=4.dp,end=12.dp).size(40.dp)) {
                            Icon(Icons.Filled.Search,"搜索",tint=TextPrimary.copy(alpha=0.30f),modifier=Modifier.size(26.dp))
                        }
                        if (drawerCategoryFilter != null) {
                            AssistChip(onClick={drawerCategoryFilter=null}, label={Text(drawerCategoryFilter!!.second,style=MaterialTheme.typography.labelSmall)},
                                trailingIcon={Icon(Icons.Filled.Close,null,Modifier.size(14.dp))},
                                modifier=Modifier.align(Alignment.TopCenter).padding(top=4.dp))
                        }
                    }
                }
                Route.STATS -> StatisticsScreen(onNavigateBack={goBack()}, onNavigateToDetail={detailItemId=it;navigate(Route.DETAIL)}, isTabMode=true)
                Route.MY -> MyLandingScreen(
                    onNavigateToSettings = { navigate(Route.SETTINGS) },
                    onNavigateToCategory = { navigate(Route.CATEGORY) },
                    onNavigateToBackup = { navigate(Route.BACKUP) }
                )
                Route.ADD -> AddItemScreen(editItemId=editItemId, onNavigateBack={editItemId=null;goBack()})
                Route.DETAIL -> DetailScreen(itemId=detailItemId, onNavigateBack={goBack()}, onNavigateToEdit={editItemId=it;navigate(Route.ADD)})
                Route.SEARCH -> SearchScreen(onNavigateBack={goBack()}, onNavigateToDetail={detailItemId=it;navigate(Route.DETAIL)})
                Route.SETTINGS -> SettingsScreen(onNavigateBack={goBack()})
                Route.CATEGORY -> CategoryScreen(onNavigateBack={goBack()})
                Route.BACKUP -> BackupScreen(onNavigateBack={goBack()})
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatsTopBar() {
    TopAppBar(title={Text("资产汇总",fontSize=24.sp,fontWeight=FontWeight.Bold,color=TextPrimary)},
        actions={TextButton(onClick={}){Text("全部时间",fontSize=16.sp,color=Primary);Icon(Icons.Filled.ArrowDropDown,null,tint=Primary)}})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyTopBar() {
    TopAppBar(title={Text("我的",fontSize=24.sp,fontWeight=FontWeight.Bold,color=TextPrimary)})
}
