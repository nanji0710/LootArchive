package com.nanji.lootarchive.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanji.lootarchive.ui.additem.AddItemScreen
import com.nanji.lootarchive.ui.about.AboutScreen
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
import com.nanji.lootarchive.ui.theme.*
import com.nanji.lootarchive.util.PhotoQueue
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.nanji.lootarchive.ui.liquidglass.*

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
    const val ABOUT="about"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var currentTab by rememberSaveable { mutableIntStateOf(0) }
    var currentRoute by rememberSaveable { mutableStateOf(Route.HOME) }
    var detailItemId by rememberSaveable { mutableStateOf(0L) }
    var editItemId by rememberSaveable { mutableStateOf<Long?>(null) }

    val mainContext = LocalContext.current
    var cameraSession by remember { mutableIntStateOf(0) }

    val settingsVM: com.nanji.lootarchive.ui.settings.SettingsViewModel = hiltViewModel()
    val avatarUri by settingsVM.uiState.collectAsState()
    var drawerCategoryFilter by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var showCategorySheet by remember { mutableStateOf(false) }
    val bgBrush = backgroundBrush()
    val backdrop = rememberLayerBackdrop {
        drawRect(brush = bgBrush)
        drawContent()
    }
    val hostState = rememberLiquidDialogHostState()
    var backStack by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }

    fun navigate(route: String, id: Long? = null) {
        if (id != null) { if (route == Route.ADD) editItemId = id; if (route == Route.DETAIL) detailItemId = id }
        backStack = backStack + currentRoute; currentRoute = route
    }
    fun goBack() { editItemId = null; if (backStack.isNotEmpty()) { currentRoute = backStack.last(); backStack = backStack.dropLast(1) } }
    fun switchTab(tab: Int) {
        currentTab = tab
        backStack = emptyList()
        editItemId = null
        detailItemId = 0L
        cameraSession = 0
        currentRoute = when(tab) { 0->Route.HOME; 1->Route.STATS; 2->Route.MY; else->Route.HOME }
    }

    val isSubPage = currentRoute !in listOf(Route.HOME, Route.STATS, Route.MY)
    val pagerState = rememberPagerState(initialPage = currentTab, pageCount = { 3 })
    LaunchedEffect(currentTab) {
        if (pagerState.currentPage != currentTab) pagerState.animateScrollToPage(currentTab)
    }
    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage != currentTab && !isSubPage) switchTab(pagerState.settledPage)
    }
    val isHome = currentRoute == Route.HOME

    BackHandler(enabled = isSubPage) { goBack() }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { /* 不使用 TopAppBar */ },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            // 捕获层：绘制页面 backdrop 供悬浮玻璃组件采样
            ProvidePageBackdrop(backdrop) {
                ProvideLiquidDialogHost(hostState) {
                    Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                    AnimatedContent(
                        targetState = if (isSubPage) currentRoute else "tabs",
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(220)) + slideInHorizontally { it / 10 }) togetherWith
                            (fadeOut(animationSpec = tween(180)) + slideOutHorizontally { -it / 10 })
                        },
                        label = "page"
                    ) { state ->
                        if (state == "tabs") {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                                key = { it }
                            ) { page ->
                                when (page) {
                                    0 -> HomeScreen(
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
                                    1 -> StatisticsScreen(
                                        onNavigateBack={goBack()},
                                        onNavigateToDetail={navigate(Route.DETAIL, it)},
                                        isTabMode=true
                                    )
                                    else -> MyLandingScreen(
                                        avatarUri = avatarUri.avatarUri,
                                        onNavigateToSettings = { navigate(Route.SETTINGS) },
                                        onNavigateToCategory = { navigate(Route.CATEGORY) },
                                        onNavigateToBackup = { navigate(Route.BACKUP) },
                                        onNavigateToRecycleBin = { navigate(Route.RECYCLEBIN) },
                                        onNavigateToAbout = { navigate(Route.ABOUT) }
                                    )
                                }
                            }
                        } else {
                            when (state) {
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
                                Route.ABOUT -> AboutScreen(onNavigateBack={goBack()})
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
                    }
                    } // close layerBackdrop Box

                // ── 首页悬浮搜索栏（液态玻璃，采样页面 backdrop）──
                if (isHome) {
                    val backdrop = LocalPageBackdrop.current
                    if (backdrop != null) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            LiquidGlassButton(
                                onClick = { navigate(Route.SEARCH) },
                                backdrop = backdrop,
                                modifier = Modifier.fillMaxWidth(),
                                height = 48.dp,
                                horizontalPadding = 14.dp
                            ) {
                                Icon(Icons.Outlined.Search, "搜索", Modifier.size(18.dp), tint = TextAuxiliary())
                                Spacer(Modifier.width(8.dp))
                                Text("搜索物品...", fontSize = 14.sp, color = TextAuxiliary())
                            }
                        }

                        // 液态玻璃 FAB（缩小版）
                        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp)) {
                            LiquidGlassButton(
                                onClick = { navigate(Route.ADD) },
                                backdrop = backdrop,
                                height = 40.dp,
                                horizontalPadding = 18.dp
                            ) {
                                Icon(Icons.Rounded.Add, "新增物品", Modifier.size(18.dp), tint = Primary())
                                Spacer(Modifier.width(4.dp))
                                Text("新增物品", color = TextPrimary(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // ── v5.0 液态玻璃可拖拽底部导航 ──
                if (!isSubPage) {
                    val backdrop = LocalPageBackdrop.current
                    if (backdrop != null) {
                        LiquidGlassBottomBar(
                            tabs = MainTab.entries.map { LiquidGlassTab(it.label, it.unselectedIcon, it.selectedIcon) },
                            selectedTabIndex = currentTab,
                            onTabSelected = ::switchTab,
                            backdrop = backdrop,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp, start = 20.dp, end = 20.dp)
                        )
                    }
                }
                LiquidDialogHost(hostState, backdrop)
                } // close ProvideLiquidDialogHost
            } // close ProvidePageBackdrop
        }
    }
}
