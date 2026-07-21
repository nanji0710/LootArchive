package com.nanji.lootarchive.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
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
import coil.compose.AsyncImage
import com.nanji.lootarchive.data.repository.SettingsRepository
import androidx.hilt.navigation.compose.hiltViewModel
import com.nanji.lootarchive.ui.component.CategoryDrawerViewModel
import com.nanji.lootarchive.ui.theme.ChartColors
import com.nanji.lootarchive.ui.theme.Primary
import com.nanji.lootarchive.ui.theme.TextAuxiliary
import com.nanji.lootarchive.ui.theme.TextPrimary

enum class MainTab(val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    HOME("首页", Icons.Filled.Home, Icons.Outlined.Home),
    STATS("资产汇总", Icons.Filled.PieChart, Icons.Outlined.PieChart),
    MY("我的", Icons.Filled.Person, Icons.Outlined.Person)
}

// 简易页面路由（替代 NavHost，根除闪退）
private object Route { const val HOME="home"; const val STATS="stats"; const val MY="my"; const val ADD="add"; const val DETAIL="detail"; const val SEARCH="search"; const val SETTINGS="settings"; const val CATEGORY="category"; const val BACKUP="backup" }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var currentTab by remember { mutableIntStateOf(0) }
    var currentRoute by remember { mutableStateOf(Route.HOME) }
    var detailItemId by remember { mutableStateOf(0L) }
    var editItemId by remember { mutableStateOf<Long?>(null) }
    var drawerCategoryFilter by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var showCategorySheet by remember { mutableStateOf(false) }
    val backStack = remember { mutableListOf<String>() }

    fun navigate(route: String, id: Long? = null) {
        if (id != null) { if (route == Route.ADD) editItemId = id; if (route == Route.DETAIL) detailItemId = id }
        backStack.add(currentRoute); currentRoute = route
    }
    fun goBack() { editItemId = null; if (backStack.isNotEmpty()) currentRoute = backStack.removeLast() }
    fun switchTab(tab: Int) {
        currentTab = tab
        backStack.clear()
        currentRoute = when(tab) { 0->Route.HOME; 1->Route.STATS; 2->Route.MY; else->Route.HOME }
    }

    val isSubPage = currentRoute !in listOf(Route.HOME, Route.STATS, Route.MY)

    // 系统返回键：子页面回退，主页不拦截
    BackHandler(enabled = isSubPage) { goBack() }

    Scaffold(
        containerColor = Color.Transparent, // 透明背景，让全局背景图透出
        topBar = {
            // 所有Tab页面取消TopBar，按钮改为悬浮
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (currentRoute) {
                Route.HOME -> {
                    HomeScreen(
                        categoryFilter = drawerCategoryFilter,
                        onNavigateToAddItem = { navigate(Route.ADD) },
                        onNavigateToDetail = { navigate(Route.DETAIL, it) },
                        onNavigateToSearch = { navigate(Route.SEARCH) },
                        onNavigateToStats = { switchTab(1) },
                        onNavigateToCategory = { navigate(Route.CATEGORY) },
                        onExportExcel = { navigate(Route.BACKUP) },
                        onImportExcel = { navigate(Route.BACKUP) },
                        onBackupData = { navigate(Route.BACKUP) }
                    )
                }
                Route.STATS -> {
                    var showTimeFilter by remember { mutableStateOf(false) }
                    var timeFilterLabel by remember { mutableStateOf("全部时间") }
                    val statsViewModel: com.nanji.lootarchive.ui.statistics.StatisticsViewModel = hiltViewModel()
                    Box(Modifier.fillMaxSize()) {
                        StatisticsScreen(onNavigateBack={goBack()}, onNavigateToDetail={navigate(Route.DETAIL, it)}, isTabMode=true)
                        // 悬浮时间筛选按钮
                        Row(Modifier.align(Alignment.TopEnd).padding(top=4.dp, end=12.dp)) {
                            Box {
                                TextButton(onClick={showTimeFilter=true}) {
                                    Text(timeFilterLabel, fontSize=14.sp, color=Primary())
                                    Icon(Icons.Filled.ArrowDropDown, null, tint=Primary())
                                }
                                DropdownMenu(expanded=showTimeFilter, onDismissRequest={showTimeFilter=false},
                                    containerColor = MaterialTheme.colorScheme.surface
                                ) {
                                    listOf("all" to "全部时间", "3months" to "近三月", "6months" to "近半年", "1year" to "近一年").forEach{(key,label)->
                                        DropdownMenuItem(text={Text(label)}, onClick={
                                            timeFilterLabel=label
                                            statsViewModel.setTimeFilter(key)
                                            showTimeFilter=false
                                        })
                                    }
                                }
                            }
                        }
                    }
                }
                Route.MY -> MyLandingScreen(
                    onNavigateToSettings = { navigate(Route.SETTINGS) },
                    onNavigateToCategory = { navigate(Route.CATEGORY) },
                    onNavigateToBackup = { navigate(Route.BACKUP) }
                )
                Route.ADD -> AddItemScreen(editItemId=editItemId, onNavigateBack={editItemId=null;goBack()})
                Route.DETAIL -> DetailScreen(itemId=detailItemId, onNavigateBack={goBack()}, onNavigateToEdit={navigate(Route.ADD, it)})
                Route.SEARCH -> SearchScreen(onNavigateBack={goBack()}, onNavigateToDetail={navigate(Route.DETAIL, it)})
                Route.SETTINGS -> SettingsScreen(onNavigateBack={goBack()}, onNavigateToCategory={navigate(Route.CATEGORY)})
                Route.CATEGORY -> CategoryScreen(onNavigateBack={goBack()})
                Route.BACKUP -> BackupScreen(onNavigateBack={goBack()})
            }

            // ─── 全局悬浮：Tab胶囊（下方）+ 操作按钮（上方） ───
            if (!isSubPage) {
                // 操作按钮：分类(左) / 新增(中) / 搜索(右)
                Row(
                    Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 105.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom
                ) {
                    FloatingActionButton(onClick = { showCategorySheet = true }, containerColor = Primary(), modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.Menu, "分类", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    FloatingActionButton(onClick = { navigate(Route.ADD) }, containerColor = Primary(), modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Filled.Add, "新增", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    FloatingActionButton(onClick = { navigate(Route.SEARCH) }, containerColor = Primary(), modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.Search, "搜索", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
                // Tab胶囊（操作按钮下方）
                Row(
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 50.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically
                ) {
                    MainTab.entries.forEachIndexed { index, tab ->
                        val selected = currentTab == index
                        Surface(
                            onClick = { switchTab(index) },
                            shape = RoundedCornerShape(20.dp),
                            color = if (selected) Primary() else Primary().copy(alpha = 0.2f)
                        ) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if(selected)tab.selectedIcon else tab.unselectedIcon, tab.label,
                                    tint = if(selected) Color.White else TextPrimary().copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                if (selected) { Spacer(Modifier.width(4.dp)); Text(tab.label, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium) }
                            }
                        }
                    }
                }
            }
            // 分类筛选标签
            if (drawerCategoryFilter != null) {
                AssistChip(onClick={drawerCategoryFilter=null}, label={Text(drawerCategoryFilter!!.second,style=MaterialTheme.typography.labelSmall)},
                    trailingIcon={Icon(Icons.Filled.Close,null,Modifier.size(14.dp))}, modifier=Modifier.align(Alignment.TopCenter).padding(top=4.dp))
            }
        }
    }

    // 分类筛选底部弹出
    if (showCategorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showCategorySheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            CategoryFilterSheet(
                selectedFilter = drawerCategoryFilter,
                onCategorySelected = { id, name ->
                    drawerCategoryFilter = if (id == -1L) null else Pair(id, name)
                    showCategorySheet = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilterSheet(
    selectedFilter: Pair<Long, String>?,
    onCategorySelected: (Long, String) -> Unit
) {
    val viewModel: CategoryDrawerViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsState()

    Column(Modifier.padding(24.dp).fillMaxWidth()) {
        Text("物品分类", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary())
        Spacer(Modifier.height(16.dp))

        // 全部物品
        Surface(
            onClick = { onCategorySelected(-1L, "全部物品") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = if (selectedFilter == null) Primary().copy(alpha = 0.12f) else Color.Transparent
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("全部物品", fontSize = 16.sp, color = TextPrimary(), modifier = Modifier.weight(1f))
                Text("${state.totalItemCount}", fontSize = 13.sp, color = TextAuxiliary())
            }
        }

        Spacer(Modifier.height(8.dp))

        state.categories.forEachIndexed { index, cat ->
            val color = ChartColors[index % ChartColors.size]
            val isSelected = selectedFilter?.first == cat.id
            Surface(
                onClick = { onCategorySelected(cat.id, cat.name) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) color.copy(alpha = 0.12f) else Color.Transparent
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.width(4.dp).height(30.dp), RoundedCornerShape(2.dp), color = color) {}
                    Spacer(Modifier.width(10.dp))
                    Text(cat.name, fontSize = 16.sp, color = TextPrimary(), modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

