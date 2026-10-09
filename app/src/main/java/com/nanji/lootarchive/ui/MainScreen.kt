package com.nanji.lootarchive.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.collectIsDraggedAsState
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
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
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

    // ── tab 请求序号：每次点击底部导航都自增 ──
    // 它必须能重新触发下面的收敛协程，即使连点的是同一个 tab。
    var tabRequest by remember { mutableIntStateOf(0) }
    var tabRequestTarget by remember { mutableIntStateOf(currentTab) }

    fun navigate(route: String, id: Long? = null) {
        if (id != null) { if (route == Route.ADD) editItemId = id; if (route == Route.DETAIL) detailItemId = id }
        backStack = backStack + currentRoute; currentRoute = route
    }
    fun goBack() { editItemId = null; if (backStack.isNotEmpty()) { currentRoute = backStack.last(); backStack = backStack.dropLast(1) } }
    fun switchTab(tab: Int) {
        currentTab = tab
        tabRequestTarget = tab
        tabRequest++
        backStack = emptyList()
        editItemId = null
        detailItemId = 0L
        cameraSession = 0
        currentRoute = when(tab) { 0->Route.HOME; 1->Route.STATS; 2->Route.MY; else->Route.HOME }
    }

    val isSubPage = currentRoute !in listOf(Route.HOME, Route.STATS, Route.MY)
    val pagerState = rememberPagerState(initialPage = currentTab, pageCount = { 3 })
    val tabStateHolder = rememberSaveableStateHolder()

    // ── tab 与 pager 的同步 ──
    // 设计：currentTab 是唯一事实来源，pager 只是它的视图。
    //
    // 旧实现有两个连锁缺陷，会导致「页面停在某一页、导航栏却高亮另一页」且再也回不来：
    //  1) 收敛只在 currentTab 变化时触发。快速连点时前一次 animateScrollToPage 被取消，
    //     pager 被留在两页之间；此时再点同一个 tab，currentTab 没变 →
    //     LaunchedEffect 不重启 → 永远没有第二次对齐，卡死。
    //  2) animateScrollToPage 内部有「已在该页就直接 return」的守卫，它对
    //     currentPage / currentPageOffsetFraction 做精确浮点比较；而这两个值会被
    //     snapToItem 提前写入、又被每次测量覆写，动画被取消后可能与真实渲染位置不一致，
    //     于是该调用静默不滚 —— 即使触发了收敛也修不好。
    //
    // 修法：每次点击自增 tabRequest 强制重启收敛协程（含连点同一个 tab）；动画结束后
    // 无条件再补一次 scrollToPage 兜底 —— 它走 snapToItem，会写回 currentPage 并
    // forceRemeasure，保证渲染位置真的落在目标页。这里刻意不用 currentPage 做判断，
    // 因为它正是那个会撒谎的值。
    var pagerDraggedByUser by remember { mutableStateOf(false) }
    val isPagerDragged by pagerState.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(isPagerDragged) { if (isPagerDragged) pagerDraggedByUser = true }

    LaunchedEffect(tabRequest, isSubPage) {
        if (isSubPage) return@LaunchedEffect
        pagerDraggedByUser = false
        val target = tabRequestTarget
        if (pagerState.currentPage != target || pagerState.currentPageOffsetFraction != 0f) {
            pagerState.animateScrollToPage(target)
        }
        pagerState.scrollToPage(target)
    }
    // · 只有用户手势滑动才允许用 pager 落点回写 currentTab。程序化滚动期间的
    //   settledPage 停留在「本次滚动开始时的那一页」，拿它回写会把用户刚点的 tab
    //   覆盖掉。用 pager 自己的 interactionSource 区分手势与程序化滚动。
    LaunchedEffect(pagerState.settledPage) {
        if (!isSubPage && pagerDraggedByUser) {
            pagerDraggedByUser = false
            if (pagerState.settledPage != currentTab) switchTab(pagerState.settledPage)
        }
    }
    val isHome = !isSubPage && pagerState.currentPage == 0

    BackHandler(enabled = isSubPage) { goBack() }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                            // 页面转场统一走 Motion token：进场用 Medium、退场用 Quick
                            // （退出更快收场，免得两个页面同时在场太久）。
                            // 注意淡入要 FiniteAnimationSpec<Float>、滑动要
                            // FiniteAnimationSpec<IntOffset>，两者不能共用一个实例。
                            val fadeInSpec = tween<Float>(MotionDuration.Medium, easing = MotionCurve.EaseOut)
                            val fadeOutSpec = tween<Float>(MotionDuration.Quick, easing = MotionCurve.EaseOut)
                            val slideInSpec = tween<IntOffset>(MotionDuration.Medium, easing = MotionCurve.EaseOut)
                            val slideOutSpec = tween<IntOffset>(MotionDuration.Quick, easing = MotionCurve.EaseOut)
                            (fadeIn(animationSpec = fadeInSpec) + slideInHorizontally(animationSpec = slideInSpec) { it / 10 }) togetherWith
                            (fadeOut(animationSpec = fadeOutSpec) + slideOutHorizontally(animationSpec = slideOutSpec) { -it / 10 })
                        },
                        label = "page"
                    ) { state ->
                        if (state == "tabs") {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                                key = { it }
                            ) { page ->
                                tabStateHolder.SaveableStateProvider(page) {
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
                                .statusBarsPadding()
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
                                Text("搜索物品...", style = MaterialTheme.typography.bodyMedium, color = TextAuxiliary())
                            }
                        }

                        // 液态玻璃 FAB（缩小版）
                        Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 90.dp)) {
                            LiquidGlassButton(
                                onClick = { navigate(Route.ADD) },
                                backdrop = backdrop,
                                height = 40.dp,
                                horizontalPadding = 18.dp
                            ) {
                                Icon(Icons.Rounded.Add, "新增物品", Modifier.size(18.dp), tint = Primary())
                                Spacer(Modifier.width(4.dp))
                                Text("新增物品", color = TextPrimary(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
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
                            selectedTabIndex = pagerState.currentPage,
                            onTabSelected = ::switchTab,
                            backdrop = backdrop,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
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
