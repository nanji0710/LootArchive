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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
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
    HOME("首页", Icons.Rounded.Home, Icons.Outlined.Home),
    STATS("统计", Icons.Rounded.PieChart, Icons.Outlined.PieChart),
    MY("我的", Icons.Rounded.Person, Icons.Outlined.Person)
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
            // 内容区：hazeSource + 提供 LocalHazeState 给子组件
            Box(Modifier.fillMaxSize().hazeSource(hazeState)) {
                CompositionLocalProvider(LocalHazeState provides hazeState) {
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
                            StatisticsScreen(
                                onNavigateBack={goBack()},
                                onNavigateToDetail={navigate(Route.DETAIL, it)},
                                isTabMode=true
                            )
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
                } // close CompositionLocalProvider
            } // close hazeSource Box

            // ── v5.1.4 首页悬浮搜索栏（Haze 玻璃模糊，在 hazeSource 之上）──
            if (isHome) {
                val dark = LocalDarkTheme.current
                val searchGlassColor = if (dark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.55f)
                val searchGlassStyle = HazeStyle(
                    backgroundColor = Color.Transparent,
                    tints = listOf(
                        HazeTint(searchGlassColor),
                        HazeTint(if (dark) Color.White.copy(alpha = 0.02f) else Color.White.copy(alpha = 0.06f))
                    ),
                    blurRadius = 20.dp,
                    noiseFactor = 0f,
                    fallbackTint = HazeTint(searchGlassColor)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(44.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(22.dp),
                            ambientColor = if (dark) Color.Black.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f),
                            spotColor = if (dark) Color.Black.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.04f)
                        )
                        .clip(RoundedCornerShape(22.dp))
                        .hazeEffect(state = hazeState, style = searchGlassStyle)
                        .clickable { navigate(Route.SEARCH) }
                ) {
                    Row(
                        Modifier.fillMaxSize().padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Search, "搜索", Modifier.size(18.dp), tint = TextAuxiliary())
                        Spacer(Modifier.width(8.dp))
                        Text("搜索物品...", fontSize = 14.sp, color = TextAuxiliary())
                    }
                }

                // v5.1.4 胶囊形 FAB（缩小版）
                Surface(
                    onClick = { navigate(Route.ADD) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 90.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Primary(),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Add, "新增物品", Modifier.size(18.dp), tint = Color.White)
                        Spacer(Modifier.width(4.dp))
                        Text("新增物品", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { switchTab(index) }
                                    .padding(vertical = 3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(if (selected) 30.dp else 26.dp)
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
                                        modifier = Modifier.size(20.dp),
                                        tint = if (selected) Primary() else TextAuxiliary()
                                    )
                                }
                                Spacer(Modifier.height(1.dp))
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
