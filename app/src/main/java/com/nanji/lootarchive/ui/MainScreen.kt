package com.nanji.lootarchive.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanji.lootarchive.ui.additem.AddItemScreen
import com.nanji.lootarchive.ui.backup.BackupScreen
import com.nanji.lootarchive.ui.camera.CameraScreen
import com.nanji.lootarchive.ui.recyclebin.RecycleBinScreen
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
import com.nanji.lootarchive.ui.component.GlassPanel
import com.nanji.lootarchive.ui.theme.*
import com.nanji.lootarchive.util.PhotoQueue
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

enum class MainTab(val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    HOME("首页", Icons.Filled.Home, Icons.Outlined.Home),
    STATS("汇总", Icons.Filled.PieChart, Icons.Outlined.PieChart),
    MY("我的", Icons.Filled.Person, Icons.Outlined.Person)
}

// 简易页面路由
private object Route {
    const val HOME="home"; const val STATS="stats"; const val MY="my"
    const val ADD="add"; const val DETAIL="detail"; const val SEARCH="search"
    const val SETTINGS="settings"; const val CATEGORY="category"
    const val BACKUP="backup"; const val CAMERA="camera"; const val RECYCLEBIN="recyclebin"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var currentTab by remember { mutableIntStateOf(0) }
    var currentRoute by remember { mutableStateOf(Route.HOME) }
    var detailItemId by remember { mutableStateOf(0L) }
    var editItemId by remember { mutableStateOf<Long?>(null) }

    val mainContext = LocalContext.current
    var cameraSession by remember { mutableIntStateOf(0) }

    val settingsVM: com.nanji.lootarchive.ui.settings.SettingsViewModel = hiltViewModel()
    val avatarUri by settingsVM.uiState.collectAsState()
    var drawerCategoryFilter by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var showCategorySheet by remember { mutableStateOf(false) }
    val hazeState = remember { HazeState() }
    val backStack = remember { mutableListOf<String>() }

    fun navigate(route: String, id: Long? = null) {
        if (id != null) { if (route == Route.ADD) editItemId = id; if (route == Route.DETAIL) detailItemId = id }
        backStack.add(currentRoute); currentRoute = route
    }
    fun goBack() { editItemId = null; if (backStack.isNotEmpty()) currentRoute = backStack.removeLast() }
    fun switchTab(tab: Int) {
        currentTab = tab
        backStack.clear()
        editItemId = null
        detailItemId = 0L
        cameraSession = 0
        currentRoute = when(tab) { 0->Route.HOME; 1->Route.STATS; 2->Route.MY; else->Route.HOME }
    }

    val isSubPage = currentRoute !in listOf(Route.HOME, Route.STATS, Route.MY)
    val isHome = currentRoute == Route.HOME

    BackHandler(enabled = isSubPage) { goBack() }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { /* 不使用 TopAppBar */ },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            // 内容区：hazeSource
            Box(Modifier.fillMaxSize().hazeSource(hazeState)) {
                AnimatedContent(
                    targetState = currentRoute,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220)) + slideInHorizontally { it / 10 }) togetherWith
                        (fadeOut(animationSpec = tween(180)) + slideOutHorizontally { -it / 10 })
                    },
                    label = "page"
                ) { route ->
                    when (route) {
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
                                StatisticsScreen(
                                    onNavigateBack={goBack()},
                                    onNavigateToDetail={navigate(Route.DETAIL, it)},
                                    isTabMode=true
                                )
                                Row(Modifier.align(Alignment.TopEnd).padding(top=4.dp, end=12.dp)) {
                                    Box {
                                        TextButton(onClick={showTimeFilter=true}) {
                                            Text(timeFilterLabel, fontSize=14.sp, color=Primary())
                                            Icon(Icons.Filled.ArrowDropDown, null, tint=Primary())
                                        }
                                        DropdownMenu(
                                            expanded=showTimeFilter,
                                            onDismissRequest={showTimeFilter=false},
                                            containerColor = MaterialTheme.colorScheme.surface
                                        ) {
                                            listOf(
                                                "all" to "全部时间",
                                                "3months" to "近三月",
                                                "6months" to "近半年",
                                                "1year" to "近一年"
                                            ).forEach{(key,label)->
                                                DropdownMenuItem(
                                                    text={Text(label)},
                                                    onClick={
                                                        timeFilterLabel=label
                                                        statsViewModel.setTimeFilter(key)
                                                        showTimeFilter=false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Route.MY -> MyLandingScreen(
                            avatarUri = avatarUri.avatarUri,
                            onNavigateToSettings = { navigate(Route.SETTINGS) },
                            onNavigateToCategory = { navigate(Route.CATEGORY) },
                            onNavigateToBackup = { navigate(Route.BACKUP) },
                            onNavigateToRecycleBin = { navigate(Route.RECYCLEBIN) }
                        )
                        Route.ADD -> AddItemScreen(
                            editItemId = editItemId,
                            onNavigateBack = { editItemId = null; goBack() },
                            onNavigateToCamera = { navigate(Route.CAMERA) },
                            photoSession = cameraSession
                        )
                        Route.DETAIL -> DetailScreen(
                            itemId=detailItemId,
                            onNavigateBack={goBack()},
                            onNavigateToEdit={navigate(Route.ADD, it)}
                        )
                        Route.SEARCH -> SearchScreen(
                            onNavigateBack={goBack()},
                            onNavigateToDetail={navigate(Route.DETAIL, it)}
                        )
                        Route.SETTINGS -> SettingsScreen(
                            onNavigateBack={goBack()},
                            onNavigateToCategory={navigate(Route.CATEGORY)}
                        )
                        Route.CATEGORY -> CategoryScreen(onNavigateBack={goBack()})
                        Route.BACKUP -> BackupScreen(onNavigateBack={goBack()})
                        Route.RECYCLEBIN -> RecycleBinScreen(onNavigateBack={goBack()})
                        Route.CAMERA -> CameraScreen(
                            onBack = { goBack() },
                            onPhotoTaken = { paths ->
                                PhotoQueue.enqueue(paths)
                                cameraSession++
                                goBack()
                            }
                        )
                    }
                }

                // ── 首页：仅保留一个居中新增大按钮 + 顶部搜索/分类入口 ──
                if (isHome) {
                    // 顶部：搜索栏 + 分类筛选
                    Row(
                        Modifier.fillMaxWidth().align(Alignment.TopCenter)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 搜索栏（轻量圆角框）
                        Surface(
                            onClick = { navigate(Route.SEARCH) },
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(21.dp),
                            color = if (LocalDarkTheme.current)
                                Color.White.copy(alpha = 0.14f)
                            else
                                Color.Black.copy(alpha = 0.08f)
                        ) {
                            Row(
                                Modifier.fillMaxSize().padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.Search, "搜索",
                                    Modifier.size(18.dp), tint = TextAuxiliary()
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "搜索物品...",
                                    fontSize = 14.sp,
                                    color = TextAuxiliary()
                                )
                            }
                        }

                        // 分类按钮
                        Surface(
                            onClick = { showCategorySheet = true },
                            modifier = Modifier.size(42.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = if (LocalDarkTheme.current)
                                Primary().copy(alpha = 0.20f)
                            else
                                Primary().copy(alpha = 0.10f)
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.Category, "分类",
                                    Modifier.size(20.dp), tint = Primary()
                                )
                            }
                        }
                    }

                    // 居中：唯一的新增 FAB
                    FloatingActionButton(
                        onClick = { navigate(Route.ADD) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 100.dp)
                            .size(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        containerColor = Primary(),
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 10.dp
                        )
                    ) {
                        Icon(Icons.Filled.Add, "新增物品", Modifier.size(26.dp))
                    }
                }

                // 分类筛选标签
                if (drawerCategoryFilter != null) {
                    AssistChip(
                        onClick={drawerCategoryFilter=null},
                        label={Text(drawerCategoryFilter!!.second,style=MaterialTheme.typography.labelSmall)},
                        trailingIcon={Icon(Icons.Filled.Close,null,Modifier.size(14.dp))},
                        modifier=Modifier.align(Alignment.TopCenter).padding(top=60.dp)
                    )
                }
            }

            // ── 底部轻量导航（仅主Tab显示） ──
            if (!isSubPage) {
                GlassPanel(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp, start = 28.dp, end = 28.dp),
                    hazeState = hazeState,
                    shape = RoundedCornerShape(22.dp),
                    containerColor = if (LocalDarkTheme.current)
                        Color.Black.copy(alpha = 0.20f)
                    else
                        Color.White.copy(alpha = 0.30f),
                    borderColor = if (LocalDarkTheme.current)
                        Color.White.copy(alpha = 0.12f)
                    else
                        Color.White.copy(alpha = 0.60f),
                    shadowElevation = 12.dp,
                    blurRadius = 20.dp
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MainTab.entries.forEachIndexed { index, tab ->
                            val selected = currentTab == index
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(72.dp)
                                    .clickable { switchTab(index) }
                                    .padding(vertical = 2.dp)
                            ) {
                                Icon(
                                    if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    tab.label,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (selected) Primary() else TextAuxiliary()
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    tab.label,
                                    fontSize = 10.sp,
                                    color = if (selected) Primary() else TextAuxiliary(),
                                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 分类筛选底部弹出
    if (showCategorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showCategorySheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
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
        Text(
            "物品分类",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary(),
            fontFamily = FredokaFont
        )
        Spacer(Modifier.height(16.dp))

        Surface(
            onClick = { onCategorySelected(-1L, "全部物品") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = if (selectedFilter == null) Primary().copy(alpha = 0.10f) else Color.Transparent
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
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
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) color.copy(alpha = 0.10f) else Color.Transparent
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        Modifier.width(4.dp).height(26.dp),
                        RoundedCornerShape(2.dp),
                        color = color
                    ) {}
                    Spacer(Modifier.width(12.dp))
                    Text(cat.name, fontSize = 16.sp, color = TextPrimary(), modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}
