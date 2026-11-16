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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanji.lootarchive.ui.additem.AddItemScreen
import com.nanji.lootarchive.ui.about.AboutScreen
import com.nanji.lootarchive.ui.assistant.AssistantScreen
import com.nanji.lootarchive.ui.backup.BackupScreen
import com.nanji.lootarchive.ui.camera.CameraScreen
import com.nanji.lootarchive.ui.recyclebin.RecycleBinScreen
import com.nanji.lootarchive.ui.category.CategoryScreen
import com.nanji.lootarchive.ui.detail.DetailScreen
import com.nanji.lootarchive.ui.detail.DetailViewModel
import com.nanji.lootarchive.ui.home.HomeScreen
import com.nanji.lootarchive.ui.search.SearchScreen
import com.nanji.lootarchive.ui.settings.AssistantSettingsScreen
import com.nanji.lootarchive.ui.settings.GeneralSettingsScreen
import com.nanji.lootarchive.ui.settings.SettingsScreen
import com.nanji.lootarchive.ui.update.UpdateScreen
import com.nanji.lootarchive.ui.nav.NavKeys
import com.nanji.lootarchive.ui.nav.navSharedElement
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
import com.nanji.lootarchive.ui.component.AmbientOrbsBackground
import com.nanji.lootarchive.ui.component.CategoryDrawerViewModel
import com.nanji.lootarchive.ui.theme.*
import com.nanji.lootarchive.util.PhotoQueue
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.nanji.lootarchive.ui.liquidglass.*

enum class MainTab(val label: String, val icon: AppIconVariants) {
    HOME("首页", AppIcons.home),
    STATS("统计", AppIcons.stats),
    // v7.6：助理从「藏在设置里的开关」升级为一等入口。
    ASSISTANT("助手", AppIcons.assistant),
    // v7.6.1：底栏第四项由「我的」改为「设置」——设置页直接承担一级页面，
    // 不再经过「我的」中转，避免同一个功能在两级页面各出现一次。
    SETTINGS("设置", AppIcons.settings)
}

/** 底栏单个 Tab 的图标：跟随「图标风格」设置，三档必须**肉眼可区分**。 */
private fun AppIconVariants.tabIcon(selected: Boolean, style: IconStyle): ImageVector = when (style) {
    // 圆润：全部实心，选中与否只靠底垫高亮区分
    IconStyle.ROUNDED -> rounded
    // 线性：全部线框
    IconStyle.OUTLINED -> outlined
    // 双色：选中实心 + 未选线框，形成层次（底栏无法叠两层，取最接近的表达）
    IconStyle.DUOTONE -> if (selected) rounded else outlined
}

private object Route {
    const val HOME="home"; const val STATS="stats"; const val MY="my"
    const val ADD="add"; const val DETAIL="detail"; const val SEARCH="search"
    const val SETTINGS="settings"; const val CATEGORY="category"
    const val BACKUP="backup"; const val CAMERA="camera"; const val RECYCLEBIN="recyclebin"
    const val ABOUT="about"
    // v7.6 新增二级页
    const val GENERAL="general"
    const val ASSISTANT_SETTINGS="assistant_settings"; const val UPDATE="update"
    /** 助理 Tab 的路由名（与二级页区分开：它在 tabStateHolder 里，不走二级页转场）。 */
    const val ASSISTANT="assistant"
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun MainScreen(assistantEnabled: Boolean = true) {
    // key 带版本号：v7.6 的 Tab 顺序从 3 个变 4 个，旧存档里的 index=2 会从「我的」变成「助手」。
    // 换一个 key 让老的 savedInstanceState 失效，升级后统一回到首页。
    var currentTab by rememberSaveable("mainTab_v76") { mutableIntStateOf(0) }
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

    // 「视效模式」目前只影响两件事：共享元素飞行是否启用、玻璃是否走模糊。
    // 玻璃部分在主题层统一下发（LootArchiveTheme 选色板），这里负责前者。
    val visualEffect = LocalVisualEffect.current
    val iconStyle = LocalIconStyle.current

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
        // 「我的」页那六个入口回到整屏左右平移：共享元素在这条路径上（一行 120dp → 整页）
        // 的起飞帧抽搐消不干净，用户决定退回原来的平移。只有详情页与新增页保留共享元素。
        // 首页「新增物品」按钮 → 新增页：从按钮位置向四周撑开铺满整屏。
        Route.ADD -> setOf(NavKeys.ADD)
        else -> emptySet()
    }

    // 另一端（源/宿主页）是否真的长着配对的元素：物品那四个元素在首页卡片和搜索结果卡上，
    // 我的页那几个菜单行只在「我的」页。不满足就返回空集，走原来的整屏平移。
    fun pairsWith(host: String, keys: Set<String>): Set<String> = when {
        keys.isEmpty() -> emptySet()
        // 「省电」视效模式：共享元素飞行整条管线不参与，退回整屏平移（见 ui/theme/Appearance.kt）。
        !visualEffect.heroMotionEnabled -> emptySet()
        keys.first().startsWith("item:") -> keys.takeIf { host == Route.HOME || host == Route.SEARCH } ?: emptySet()
        // 「新增物品」按钮只长在首页，所以新增页的源端只能是首页（从详情页点编辑进来仍走整屏平移）。
        keys.first() == NavKeys.ADD -> keys.takeIf { host == Route.HOME } ?: emptySet()
        else -> emptySet()
    }

    // 详情页的共享元素（首图 / 名称 / 金额 / 标签）要等物品数据到位才存在。数据没来就切路由，
    // SharedTransitionLayout 在首帧配不上对，这次转场就退化成「没有飞行的淡入淡出」——
    // 冷启动后第一次点卡片看到的「闪一下」就是这么来的。所以先把数据取回来再切页。
    val detailViewModel: DetailViewModel = hiltViewModel()
    val navScope = rememberCoroutineScope()
    // 非 0 = 有一张卡片正在等数据、还没真正切到详情页；此时的返回键视为「取消这次展开」。
    var pendingDetailId by remember { mutableStateOf(0L) }

    /**
     * 把详情页首图按「整屏尺寸」预解码一次。
     *
     * Coil 的内存缓存 key 含尺寸，卡片缩略图那份用不上；冷启动后第一次点卡片时，飞行的那块
     * 内容在头几百毫秒里还是空白的，落位那一刻图片才弹出来 —— 这是「冷启动闪现」的一半。
     */
    suspend fun preloadDetailCover(id: Long) {
        val state = detailViewModel.uiState.value.itemWithPhotos ?: return
        if (state.item.id != id) return
        val photoPath = state.photos.firstOrNull()?.photoPath ?: return
        runCatching {
            val dm = mainContext.resources.displayMetrics
            coil.Coil.imageLoader(mainContext).execute(
                coil.request.ImageRequest.Builder(mainContext)
                    .data(java.io.File(photoPath))
                    .size(dm.widthPixels, dm.heightPixels)
                    .build()
            )
        }
    }

    fun navigate(route: String, id: Long? = null) {
        if (id != null) { if (route == Route.ADD) editItemId = id; if (route == Route.DETAIL) detailItemId = id }
        val from = currentRoute
        val keys = pairsWith(from, pageSharedKeys(route, id ?: detailItemId))
        // 「谁在飞 / 往哪飞」与「切到哪一页」必须在同一帧写进状态。先写状态再等数据的话，
        // 首页会停在「以为自己正在转场、目标端却还不存在」的残缺配对里多挂几帧 —— 冷启动
        // 读库慢，这几帧就是用户看到的「点一下先闪一下，才开始动」。
        fun commit() {
            navForward = true
            navSharedKeys = keys
            backStack = backStack + from
            currentRoute = route
        }
        if (route == Route.DETAIL && id != null &&
            detailViewModel.uiState.value.itemWithPhotos?.item?.id != id) {
            val target = id
            pendingDetailId = target
            navScope.launch {
                detailViewModel.loadItem(target)
                withTimeoutOrNull(1200) {
                    detailViewModel.uiState.first { it.itemWithPhotos?.item?.id == target }
                }
                preloadDetailCover(target)
                if (pendingDetailId == target && currentRoute == from) {
                    pendingDetailId = 0L
                    commit()
                }
            }
            return
        }
        commit()
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
        currentRoute = when(tab) { 0->Route.HOME; 1->Route.STATS; 2->Route.ASSISTANT; else->Route.MY }
    }

    // 关闭 AI 助理后，助手 Tab 立即消失；如果当前正停在助手页，退回首页
    LaunchedEffect(assistantEnabled) {
        if (!assistantEnabled && currentTab == 2) switchTab(0)
    }

    val isSubPage = currentRoute !in listOf(Route.HOME, Route.STATS, Route.ASSISTANT, Route.MY)
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
        animationSpec = tween(MotionDuration.Hero, easing = MotionCurve.Hero),
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
                    // 「沉浸」视效模式：全屏暖色光斑缓慢漂移，作为玻璃的活画布。
                    // 放在捕获层内部，玻璃采样到的就是「渐变 + 光斑」，而不是静态底色。
                    if (visualEffect.ambientEnabled) {
                        AmbientOrbsBackground(Modifier.fillMaxSize())
                    }
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
                                            onBackupData = { navigate(Route.BACKUP) },
                                            onNavigateToAssistant = { switchTab(2) }
                                        )
                                        1 -> StatisticsScreen(
                                            onNavigateBack={goBack()},
                                            onNavigateToDetail={navigate(Route.DETAIL, it)},
                                            isTabMode=true
                                        )
                                        2 -> AssistantScreen(
                                            isTabMode = true,
                                            onNavigateToDetail = { navigate(Route.DETAIL, it) },
                                        )
                                        else -> SettingsScreen(
                                            isTabMode = true,
                                            onNavigateToCategory = { navigate(Route.CATEGORY) },
                                            onNavigateToGeneral = { navigate(Route.GENERAL) },
                                            onNavigateToAssistantSettings = { navigate(Route.ASSISTANT_SETTINGS) },
                                            onNavigateToAssistant = { switchTab(2) },
                                            onNavigateToBackup = { navigate(Route.BACKUP) },
                                            onNavigateToRecycleBin = { navigate(Route.RECYCLEBIN) },
                                            onNavigateToUpdate = { navigate(Route.UPDATE) },
                                            onNavigateToAbout = { navigate(Route.ABOUT) }
                                        )
                                    }
                                }
                            }
                            } // close ProvideHomeDissolve
                        } else {
                            when (state) {
                                Route.ADD -> NavSharedSurface(NavKeys.ADD) {                                    AddItemScreen(
                                        editItemId = editItemId,
                                        onNavigateBack = { editItemId = null; goBack() },
                                        onNavigateToCamera = { navigate(Route.CAMERA) },
                                        photoSession = cameraSession
                                    )
                                }
                                Route.DETAIL -> DetailScreen(
                                    itemId=detailItemId,
                                    onNavigateBack={goBack()},
                                    onNavigateToEdit={navigate(Route.ADD, it)}
                                )
                                Route.SEARCH -> SearchScreen(
                                    onNavigateBack={goBack()},
                                    onNavigateToDetail={navigate(Route.DETAIL, it)}
                                )
                                // 二级页回到整屏左右平移（不再是整页共享元素）。
                                Route.GENERAL -> GeneralSettingsScreen(onNavigateBack = { goBack() })
                                Route.ASSISTANT_SETTINGS -> AssistantSettingsScreen(onNavigateBack={goBack()})
                                Route.UPDATE -> UpdateScreen(onNavigateBack = { goBack() })
                                Route.CATEGORY -> CategoryScreen(onNavigateBack={goBack()})
                                Route.BACKUP -> BackupScreen(onNavigateBack={goBack()})
                                Route.RECYCLEBIN -> RecycleBinScreen(onNavigateBack={goBack()})
                                Route.ABOUT -> AboutScreen(
                                    onNavigateBack = { goBack() },
                                    onNavigateToUpdate = { navigate(Route.UPDATE) },
                                )
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
                    }
                }

                // ── 液态玻璃底部导航 + 独立「新增物品」按钮（同一行最右侧）──
                // 参考 SleepDown 的底栏：导航胶囊与圆形加号是两个独立控件。
                // 加号不占 Tab 位、不参与选中态 —— 点它永远只有「新增物品」一个含义，
                // 不再像上一版那样把 FAB 悬浮在内容之上、和底栏视觉打架。
                if (!isSubPage || chromeFlying) {
                    val backdrop = LocalPageBackdrop.current
                    if (backdrop != null) {
                        Row(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                                .fillMaxWidth()
                                .slideAway({ chromeProgress }, down = true, extra = 96.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.weight(1f)) {
                                val visibleTabs = MainTab.entries.filter {
                                    it != MainTab.ASSISTANT || assistantEnabled
                                }
                                val selectedIndex = visibleTabs.indexOfFirst { it.ordinal == currentTab }
                                    .coerceAtLeast(0)
                                LiquidGlassBottomBar(
                                    tabs = visibleTabs.map {
                                        LiquidGlassTab(
                                            it.label,
                                            it.icon.tabIcon(false, iconStyle),
                                            it.icon.tabIcon(true, iconStyle),
                                        )
                                    },
                                    selectedTabIndex = selectedIndex,
                                    onTabSelected = { index ->
                                        visibleTabs.getOrNull(index)?.let { switchTab(it.ordinal) }
                                    },
                                    backdrop = backdrop,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Box(Modifier.size(56.dp)) {
                                // 扩散光环：新增页撑开时从加号位置荡开两圈（纯绘制，不建图层）。
                                if (navSharedKeys.contains(NavKeys.ADD)) {
                                    val haloColor = Primary()
                                    Box(Modifier.matchParentSize().drawBehind {
                                        val p = chromeProgress.coerceIn(0f, 1f)
                                        if (p <= 0f || p >= 1f) return@drawBehind
                                        val baseRadius = size.height * 0.5f
                                        listOf(0f, 0.16f).forEach { delay ->
                                            val t = ((p - delay) / (1f - delay)).coerceIn(0f, 1f)
                                            if (t <= 0f) return@forEach
                                            drawCircle(
                                                color = haloColor.copy(alpha = (1f - t) * 0.20f),
                                                radius = baseRadius + t * size.width * 2.4f,
                                                center = center,
                                            )
                                        }
                                    })
                                }
                                // 共享元素的「源」锚点：与按钮同尺寸的透明底板，只提供起飞矩形。
                                // 玻璃按钮本身不进飞行容器 —— 7.2.1 的渲染线程递归就是玻璃件进图层链造成的。
                                Box(Modifier.matchParentSize().navSharedElement(NavKeys.ADD))
                                LiquidIconButton(
                                    onClick = { navigate(Route.ADD) },
                                    backdrop = backdrop,
                                    modifier = Modifier.matchParentSize(),
                                    size = 56.dp,
                                ) {
                                    Icon(Icons.Rounded.Add, "新增物品", Modifier.size(26.dp), tint = Primary())
                                }
                            }
                        }
                    }
                }
                LiquidDialogHost(hostState, backdrop)
                } // close ProvideLiquidDialogHost
            } // close ProvidePageBackdrop
        }
    }
}
