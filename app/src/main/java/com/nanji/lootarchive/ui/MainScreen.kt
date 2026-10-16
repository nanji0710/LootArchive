package com.nanji.lootarchive.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
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
import com.nanji.lootarchive.ui.detail.DetailViewModel
import com.nanji.lootarchive.ui.home.HomeScreen
import com.nanji.lootarchive.ui.search.SearchScreen
import com.nanji.lootarchive.ui.settings.SettingsScreen
import com.nanji.lootarchive.ui.nav.NavKeys
import com.nanji.lootarchive.ui.nav.ProvideHomeDissolve
import com.nanji.lootarchive.ui.nav.slideAway
import com.nanji.lootarchive.ui.nav.NavSharedSurface
import com.nanji.lootarchive.ui.nav.ProvideNavShared
import com.nanji.lootarchive.ui.statistics.StatisticsScreen
import coil.compose.AsyncImage
import com.nanji.lootarchive.data.repository.SettingsRepository
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun MainScreen() {
    var currentTab by rememberSaveable { mutableIntStateOf(0) }
    var currentRoute by rememberSaveable { mutableStateOf(Route.HOME) }
    var detailItemId by rememberSaveable { mutableStateOf(0L) }
    var editItemId by rememberSaveable { mutableStateOf<Long?>(null) }

    val mainContext = LocalContext.current
    var cameraSession by remember { mutableIntStateOf(0) }

    var drawerCategoryFilter by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var showCategorySheet by remember { mutableStateOf(false) }
    val bgBrush = backgroundBrush()
    val backdrop = rememberLayerBackdrop {
        drawRect(brush = bgBrush)
        drawContent()
    }
    val hostState = rememberLiquidDialogHostState()
    var backStack by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }

    // 这一跳是「进」还是「出」。AnimatedContent 只认 targetState，
    // 同一个路由既可能是压栈进来的、也可能是弹栈回来的，它自己分不清；
    // 少了这个记号，返回时就只能复用前进那一套位移。
    var navForward by remember { mutableStateOf(true) }

    // 本次转场里「会飞」的那些元素是谁（配套实现见 ui/nav/NavShared.kt）。
    // 只在两端都真的存在对应元素时才赋值——否则会退化成只有一端的空配对，
    // 白白把页面动画从整屏平移换成淡入淡出。
    var navSharedKeys by remember { mutableStateOf<Set<String>>(emptySet()) }

    /** 一个页面自己挂载的共享元素集合；没有元素挂载点的路由返回空集。 */
    fun pageSharedKeys(route: String, itemId: Long?): Set<String> = when (route) {
        // 详情页是「元素各自飞」的重点：卡片照片 → 整屏首图、卡片名称 → 详情标题、
        // 卡片金额 → 「购入价格」数值、卡片标签 → 详情标签，四个元素各走各的轨迹。
        Route.DETAIL -> itemId?.let {
            setOf(NavKeys.itemPhoto(it), NavKeys.itemTitle(it), NavKeys.itemPrice(it), NavKeys.itemTag(it))
        } ?: emptySet()
        Route.SETTINGS -> setOf(NavKeys.SETTINGS)
        Route.CATEGORY -> setOf(NavKeys.CATEGORY)
        Route.BACKUP -> setOf(NavKeys.BACKUP)
        Route.RECYCLEBIN -> setOf(NavKeys.RECYCLEBIN)
        Route.ABOUT -> setOf(NavKeys.ABOUT)
        else -> emptySet()
    }

    // 另一端（源/宿主页）是否真的长着配对的元素：物品那四个元素在首页卡片和搜索结果卡上，
    // 我的页那几个菜单行只在「我的」页。不满足就返回空集，走原来的整屏平移。
    fun pairsWith(host: String, keys: Set<String>): Set<String> = when {
        keys.isEmpty() -> emptySet()
        keys.first().startsWith("item:") -> keys.takeIf { host == Route.HOME || host == Route.SEARCH } ?: emptySet()
        else -> keys.takeIf { host == Route.MY } ?: emptySet()
    }

    // 详情页的共享元素（首图 / 名称 / 金额 / 标签）要等物品数据到位才存在。数据没来就切路由，
    // SharedTransitionLayout 在首帧配不上对，这次转场就退化成「没有飞行的淡入淡出」——
    // 冷启动后第一次点卡片看到的「闪一下」就是这么来的。所以先把数据取回来再切页。
    val detailViewModel: DetailViewModel = hiltViewModel()
    val navScope = rememberCoroutineScope()
    // 非 0 = 有一张卡片正在等数据、还没真正切到详情页；此时的返回键视为「取消这次展开」。
    var pendingDetailId by remember { mutableStateOf(0L) }

    fun navigate(route: String, id: Long? = null) {
        if (id != null) { if (route == Route.ADD) editItemId = id; if (route == Route.DETAIL) detailItemId = id }
        navForward = true
        val from = currentRoute
        navSharedKeys = pairsWith(from, pageSharedKeys(route, id ?: detailItemId))
        if (route == Route.DETAIL && id != null &&
            detailViewModel.uiState.value.itemWithPhotos?.item?.id != id) {
            val target = id
            pendingDetailId = target
            navScope.launch {
                detailViewModel.loadItem(target)
                val ok = withTimeoutOrNull(1200) {
                    detailViewModel.uiState.first { it.itemWithPhotos?.item?.id == target }
                }
                if (pendingDetailId == target && currentRoute == from) {
                    pendingDetailId = 0L
                    backStack = backStack + from; currentRoute = Route.DETAIL
                }
            }
            return
        }
        backStack = backStack + from; currentRoute = route
    }
    fun goBack() {
        // 还没落定的那次展开点击：返回等于取消它（行进中打断）。
        pendingDetailId = 0L
        editItemId = null
        if (backStack.isNotEmpty()) {
            navForward = false
            // 返回时「飞走」的是正在退场这一页自己的元素，所以 key 取当前页，宿主页换成要回到的那一页。
            navSharedKeys = pairsWith(backStack.last(), pageSharedKeys(currentRoute, detailItemId))
            currentRoute = backStack.last(); backStack = backStack.dropLast(1)
        }
    }
    fun switchTab(tab: Int) {
        currentTab = tab
        backStack = emptyList()
        editItemId = null
        detailItemId = 0L
        cameraSession = 0
        pendingDetailId = 0L
        navSharedKeys = emptySet()
        currentRoute = when(tab) { 0->Route.HOME; 1->Route.STATS; 2->Route.MY; else->Route.HOME }
    }

    val isSubPage = currentRoute !in listOf(Route.HOME, Route.STATS, Route.MY)
    val tabStateHolder = rememberSaveableStateHolder()

    // ── 三主页切换 ──
    // currentTab 是唯一事实来源，页面直接按 currentTab 渲染；
    // 原先承载横滑切换的 HorizontalPager 已移除（A-5 交互装饰）。
    val isHome = !isSubPage && currentTab == 0

    // 首页固定 UI（搜索框 / 新增按钮 / 底栏）与首页其余内容的统一离场进度：
    // 0 = 首页常态，1 = 完全离场。时长与缓动和 sharedBounds 的 boundsTransform 完全一致
    // （Hero + 方向化缓动），所以和卡片 Hero 严格同步，不存在谁先谁后、谁先消失。
    val chromeProgress by animateFloatAsState(
        targetValue = if (isSubPage) 1f else 0f,
        animationSpec = tween(
            MotionDuration.Hero,
            easing = if (navForward) MotionCurve.Standard else MotionCurve.Reverse
        ),
        label = "homeChromeSlide"
    )

    BackHandler(enabled = isSubPage || pendingDetailId != 0L) { goBack() }

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
                    // 共享元素作用域：卡片 ↔ 详情、菜单行 ↔ 二级页的「从哪来到哪去」都在这里发生。
                    // SharedTransitionLayout 只做两端测量配对，不新建绘制层，所以碰不到液态玻璃的图层红线。
                    SharedTransitionLayout {
                    AnimatedContent(
                        targetState = if (isSubPage) currentRoute else "tabs",
                        transitionSpec = {
                            // 没有共享元素：整屏平移。
                            // 前进：新页从右推进、旧页向左退场；返回：当前页向右退出、
                            // 上一层页面从左边跟进（前进的镜像）。关键是两个页面整段动画
                            // 「同向、同速、左右相邻」：旧页的左边缘与新页的右边缘始终
                            // 重合在同一个 x 上，合起来正好铺满屏幕——既不重叠也不留缝，
                            // 像同一张胶片在平移。位移取整屏宽度，滑就滑干净，
                            // 进场退场共用同一条 tween，两页才不会各走各的。
                            //
                            // 有共享元素：元素自己在飞，页面再来一次整屏平移就成了
                            // 「又滑又飞」两套动作打架，所以页面退成一次轻淡入淡出，
                            // 把注意力全交给飞行的那块内容。
                            val slideSpec = tween<IntOffset>(MotionDuration.Page, easing = MotionCurve.Standard)
                            if (navSharedKeys.isNotEmpty()) {
                                // 参考小红书的观感：飞行期间页面本体先别露面。目标页在飞行的大半段时间里
                                // 保持全透明，等元素快落地了才淡入（延迟 Hero×3/5，用 Hero×2/5 淡完，
                                // 正好在元素落位那一刻结束）。否则目标页的内容会和飞在半路的那一份
                                // 同时出现在屏幕上，看着就是一层重影 —— 这是「不像小红书」的主因。
                                fadeIn(animationSpec = tween(MotionDuration.Hero * 2 / 5, delayMillis = MotionDuration.Hero * 3 / 5, easing = MotionCurve.Standard)) togetherWith
                                    // 源页面要**整段慢慢解体**，不能「啪」地消失：首页的其余元素（头卡、
                                    // 分类 chips、「最近添加」标题、其他卡片）由 chromeProgress 驱动做
                                    // 缩小 + 上浮 + 淡出，如果这里的 fadeOut 还是 Quick(180ms)，整页会在
                                    // 解体刚开头就整体透明掉 —— 用户看到的仍然是「突然消失」。所以退场
                                    // 用满一个 Hero，并且用与飞行同一条（方向化的）曲线。
                                    fadeOut(animationSpec = tween(MotionDuration.Hero, easing = if (navForward) MotionCurve.Standard else MotionCurve.Reverse))
                            } else if (navForward) {
                                slideIntoContainer(SlideDirection.Left, animationSpec = slideSpec) togetherWith
                                    slideOutOfContainer(SlideDirection.Left, animationSpec = slideSpec)
                            } else {
                                slideIntoContainer(SlideDirection.Right, animationSpec = slideSpec) togetherWith
                                    slideOutOfContainer(SlideDirection.Right, animationSpec = slideSpec)
                            }
                        },
                        label = "page"
                    ) { state ->
                        // 把「谁在飞」下发给两端元素；navSharedKeys 为空时整条管线不生效。
                        ProvideNavShared(
                            this@SharedTransitionLayout,
                            this@AnimatedContent,
                            navSharedKeys,
                            navForward,
                            // 转场进度：飞行中由 chromeProgress（Hero + 方向化曲线）驱动，进入/返回
                            // 都会平滑走完全程；不在共享元素转场里时给出「已落位」的稳态值。
                            progress = { if (navSharedKeys.isNotEmpty()) chromeProgress else if (isSubPage) 1f else 0f },
                        ) {
                        if (state == "tabs") {
                            // 首页其余内容（除正在飞的那张卡片）用同一个进度做「解体 / 重新形成」；
                            // 非共享元素导航（新增 / 搜索 / 相机）保持原样，不解体。
                            ProvideHomeDissolve({ if (navSharedKeys.isNotEmpty()) chromeProgress else 0f }) {
                            Box(Modifier.fillMaxSize()) {
                                tabStateHolder.SaveableStateProvider(currentTab) {
                                    when (currentTab) {
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
                                            onNavigateToSettings = { navigate(Route.SETTINGS) },
                                            onNavigateToCategory = { navigate(Route.CATEGORY) },
                                            onNavigateToBackup = { navigate(Route.BACKUP) },
                                            onNavigateToRecycleBin = { navigate(Route.RECYCLEBIN) },
                                            onNavigateToAbout = { navigate(Route.ABOUT) }
                                        )
                                    }
                                }
                            }
                            } // close ProvideHomeDissolve
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
                                // 六个二级页整页就是共享元素的一端：从被点的那张卡片 / 那一行原地撑开，
                                // 返回时原路收回。详情页的对端是首页（或搜索结果）里的那张卡片本身。
                                Route.SETTINGS -> NavSharedSurface(NavKeys.SETTINGS) {
                                    SettingsScreen(
                                        onNavigateBack={goBack()},
                                        onNavigateToCategory={navigate(Route.CATEGORY)}
                                    )
                                }
                                Route.CATEGORY -> NavSharedSurface(NavKeys.CATEGORY) { CategoryScreen(onNavigateBack={goBack()}) }
                                Route.BACKUP -> NavSharedSurface(NavKeys.BACKUP) { BackupScreen(onNavigateBack={goBack()}) }
                                Route.RECYCLEBIN -> NavSharedSurface(NavKeys.RECYCLEBIN) { RecycleBinScreen(onNavigateBack={goBack()}) }
                                Route.ABOUT -> NavSharedSurface(NavKeys.ABOUT) { AboutScreen(onNavigateBack={goBack()}) }
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
                        } // close ProvideNavShared
                    } // close AnimatedContent
                    } // close SharedTransitionLayout
                    } // close layerBackdrop Box

                // ── 首页固定 UI 与卡片转场的联动位移 ──
                // 搜索栏 / 新增按钮 / 底部导航都**不**参与 Hero 共享元素（它们是首页固定 UI），
                // 而是与卡片共用同一条时间轴；chromeProgress 在上面统一声明，首页其余内容的
                // 「解体」进度也复用它，所以整页只有一个进度源。
                // 只有共享元素导航期间才保持挂载（真的滑出去，且不会出现「先卸载一帧再挂回」的闪动）；
                // 其它导航沿用原来的即时显隐。
                val chromeFlying = isSubPage && navSharedKeys.isNotEmpty()

                // ── 首页悬浮搜索栏（液态玻璃，采样页面 backdrop）──
                if (currentTab == 0 && (isHome || chromeFlying)) {
                    val backdrop = LocalPageBackdrop.current
                    if (backdrop != null) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .slideAway({ chromeProgress }, down = false, extra = 96.dp)
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
                        Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 90.dp).slideAway({ chromeProgress }, down = true, extra = 140.dp)) {
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
                if (!isSubPage || chromeFlying) {
                    val backdrop = LocalPageBackdrop.current
                    if (backdrop != null) {
                        LiquidGlassBottomBar(
                            tabs = MainTab.entries.map { LiquidGlassTab(it.label, it.unselectedIcon, it.selectedIcon) },
                            selectedTabIndex = currentTab,
                            onTabSelected = ::switchTab,
                            backdrop = backdrop,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 16.dp, start = 20.dp, end = 20.dp)
                                .slideAway({ chromeProgress }, down = true, extra = 96.dp)
                        )
                    }
                }
                LiquidDialogHost(hostState, backdrop)
                } // close ProvideLiquidDialogHost
            } // close ProvidePageBackdrop
        }
    }
}
