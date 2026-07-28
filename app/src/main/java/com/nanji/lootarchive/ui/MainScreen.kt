package com.nanji.lootarchive.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
    STATS("统计", Icons.Filled.PieChart, Icons.Outlined.PieChart),
    MY("我的", Icons.Filled.Person, Icons.Outlined.Person)
}

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

                // ── v5.0 首页：Hero 搜索栏 + 分类入口 + 胶囊FAB ──
                if (isHome) {
                    // 顶部搜索栏（精简圆角玻璃框）
                    Row(
                        Modifier.fillMaxWidth().align(Alignment.TopCenter)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { navigate(Route.SEARCH) },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(22.dp),
                            color = if (LocalDarkTheme.current)
                                Color.White.copy(alpha = 0.12f)
                            else
                                Color.White.copy(alpha = 0.85f),
                            shadowElevation = 2.dp
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
                        Box {
                            Surface(
                                onClick = { showCategorySheet = true },
                                modifier = Modifier.size(44.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = if (LocalDarkTheme.current)
                                    Primary().copy(alpha = 0.18f)
                                else
                                    Primary().copy(alpha = 0.10f),
                                shadowElevation = 1.dp
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Outlined.FilterAlt, "分类筛选",
                                        Modifier.size(20.dp), tint = Primary()
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showCategorySheet,
                                onDismissRequest = { showCategorySheet = false },
                                containerColor = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("全部物品") },
                                    onClick = {
                                        drawerCategoryFilter = null
                                        showCategorySheet = false
                                    },
                                    leadingIcon = {
                                        if (drawerCategoryFilter == null)
                                            Icon(Icons.Filled.Check, null, Modifier.size(18.dp), tint = Primary())
                                    }
                                )
                                val viewModel: CategoryDrawerViewModel = hiltViewModel()
                                val catState by viewModel.uiState.collectAsState()
                                catState.categories.forEach { cat ->
                                    val color = ChartColors[catState.categories.indexOf(cat) % ChartColors.size]
                                    val selected = drawerCategoryFilter?.first == cat.id
                                    DropdownMenuItem(
                                        text = { Text(cat.name) },
                                        onClick = {
                                            drawerCategoryFilter = Pair(cat.id, cat.name)
                                            showCategorySheet = false
                                        },
                                        leadingIcon = {
                                            Surface(
                                                Modifier.size(10.dp), RoundedCornerShape(5.dp), color = color
                                            ) {}
                                        },
                                        trailingIcon = {
                                            if (selected)
                                                Icon(Icons.Filled.Check, null, Modifier.size(16.dp), tint = Primary())
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // v5.0 胶囊形 FAB（居中，简洁）
                    Surface(
                        onClick = { navigate(Route.ADD) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 100.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = Primary(),
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Add, "新增物品", Modifier.size(22.dp), tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text("新增物品", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // ── v5.0 浮动胶囊式底部导航 ──
            if (!isSubPage) {
                GlassPanel(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp, start = 20.dp, end = 20.dp),
                    hazeState = hazeState,
                    shape = RoundedCornerShape(24.dp),
                    containerColor = if (LocalDarkTheme.current)
                        Color.Black.copy(alpha = 0.25f)
                    else
                        Color.White.copy(alpha = 0.35f),
                    borderColor = if (LocalDarkTheme.current)
                        Color.White.copy(alpha = 0.10f)
                    else
                        Color.White.copy(alpha = 0.50f),
                    shadowElevation = 8.dp,
                    blurRadius = 24.dp
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
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { switchTab(index) }
                                    .padding(vertical = 4.dp)
                            ) {
                                // v5.0: 选中态加圆形背景
                                Box(
                                    modifier = Modifier
                                        .size(if (selected) 36.dp else 28.dp)
                                        .then(
                                            if (selected)
                                                Modifier.background(
                                                    Primary().copy(alpha = 0.12f),
                                                    RoundedCornerShape(12.dp)
                                                )
                                            else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        tab.label,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (selected) Primary() else TextAuxiliary()
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    tab.label,
                                    fontSize = 10.sp,
                                    color = if (selected) Primary() else TextAuxiliary(),
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
