# LootArchive 液态玻璃（Liquid Glass）改造实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 LootArchive 全站玻璃效果从 Haze 升级为 Kyant Backdrop 液态玻璃（vibrancy + blur + lens 折射 + 按压高光 + 弹簧物理），保留暖琥珀视觉基调，彻底移除 Haze。

**Architecture:** 捕获层模式替代 hazeSource：MainScreen 用 `rememberLayerBackdrop { drawRect(渐变); drawContent() }` 捕获整页，悬浮搜索栏/FAB/底部导航/弹窗作为兄弟节点 `drawBackdrop` 采样该层；页面内卡片/设置行用各自局部捕获层或半透明 + 高光描边（RiseCard 模式）。

**Tech Stack:** io.github.kyant0:backdrop:1.0.0 + io.github.kyant0:capsule:2.1.1；Compose BOM 2025.06.00；Kotlin 2.1.0；Gradle 8.9。

## Global Constraints

- 移除全部 `dev.chrisbanes.haze:*` 依赖与 import（grep 确认无残留后才算完成）
- minSdk 31 兼容：`useFullGlass = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU`（13+ 全玻璃，12/12L 实体降级），所有液态组件必须有降级分支
- 视觉基调不变：琥珀主色、`GlassColorScheme` token、Fredoka/Nunito 字体、`Primary()/TextAuxiliary()` 等 API 全部保留
- 公开组件 API 不变：`NeoCard/NeoStatCard/HeroStatCard/NeoAlertDialog/GlassAlertDialog/EmptyState/GlassSurface` 签名保持，调用点零改动
- 数据层/逻辑层（data/、di/、worker/、domain/、util/ 下除 UI 工具外）零改动
- 许可证：物理引擎三文件（DampedDragAnimation/DragGestureInspector/InteractiveHighlight）为 AndroidLiquidGlass 的 Apache-2.0 移植，保留原 SPDX/版权头；其余液态组件为本项目自研实现（参照 AndroidLiquidGlass 1.0.0 Demo 公开模式，Apache-2.0），禁止逐字复制 RiseDiary GPL-3.0 源码
- 每个任务以 `.\gradlew.bat :app:compileDebugKotlin` 编译通过为完成标准；任务 10 做全量 `assembleDebug` + `test`
- 提交（commit）仅当用户明确要求时执行

---

### Task 1: 依赖引入与基线构建

> 控制器裁决（2026-09-01）：Haze 依赖保留至 Task 9 才移除。原因：源码头文件中的 Haze import 要分多个任务清理，若提前删依赖，Task 2-8 的编译验证全部失败。本任务只引入 kyant 构件。

**Files:**
- Modify: `app/build.gradle.kts:129-131`
- Modify: `settings.gradle.kts:17-25`

**Interfaces:**
- Produces: 可解析的 `io.github.kyant0:backdrop:1.0.0`、`io.github.kyant0:capsule:2.1.1` 依赖

- [ ] **Step 1: 修改依赖**

`app/build.gradle.kts` 中**保留 Haze 两行**，新增 backdrop + capsule：

```kotlin
    // Liquid Glass (Kyant Backdrop — blur/lens/vibrancy RuntimeShader)
    implementation("io.github.kyant0:backdrop:1.0.0")
    implementation("io.github.kyant0:capsule:2.1.1")
```

- [ ] **Step 2: 验证依赖可解析**

Run: `.\gradlew.bat :app:dependencies --configuration debugRuntimeClasspath`
Expected: 出现 `io.github.kyant0:backdrop:1.0.0` 与 `io.github.kyant0:capsule:2.1.1`，且无 `dev.chrisbanes.haze`

若阿里云镜像解析失败（io.github.kyant0 不在镜像白名单），将 `settings.gradle.kts` 的 `mavenCentral()` 提到 aliyun 之前（保持 google() 在最前）。

- [ ] **Step 3: 基线构建**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL（Haze 仍保留，编译旧代码；仅验证新依赖 jar 下载与类路径无冲突）

---

### Task 2: 移植液态玻璃物理基础（Apache-2.0）

**Files:**
- Create: `app/src/main/java/com/nanji/lootarchive/ui/liquidglass/DampedDragAnimation.kt`
- Create: `app/src/main/java/com/nanji/lootarchive/ui/liquidglass/DragGestureInspector.kt`
- Create: `app/src/main/java/com/nanji/lootarchive/ui/liquidglass/InteractiveHighlight.kt`

**Interfaces:**
- Produces:
  - `internal class DampedDragAnimation(animationScope, initialValue, valueRange, visibilityThreshold, initialScale, pressedScale, onDragStarted, onDragStopped, onDrag, consumeDragChanges=false, consumeInitialDown=true)`，属性 `value/progress/targetValue/pressProgress/scaleX/scaleY/velocity`，方法 `press()/release()/updateValue(Float)/animateToValue(Float)`，`val modifier: Modifier`
  - `internal suspend fun PointerInputScope.inspectDragGestures(onDragStart, onDragEnd, onDragCancel, onDrag)`
  - `internal class InteractiveHighlight(animationScope, position: (Size, Offset)->Offset = {_,o->o}, intensity=1f, radiusMultiplier=1.5f)`，属性 `pressProgress/offset`，`val modifier: Modifier` + `val gestureModifier: Modifier`

- [ ] **Step 1: 复制三个文件并改包名**

从 `D:\GitHub\RiseDiary\app\src\main\java\com\risediary\app\ui\components\liquidglass\` 复制：
- `DampedDragAnimation.kt`（182 行）
- `DragGestureInspector.kt`（103 行）
- `InteractiveHighlight.kt`（171 行）

到 `D:\GitHub\LootArchive\app\src\main\java\com\nanji\lootarchive\ui\liquidglass\`，包名改为 `com.nanji.lootarchive.ui.liquidglass`。

这三个文件为 AndroidLiquidGlass 的 Apache-2.0 移植（文件头 SPDX 已标注），**保留完整版权头**。除包名外**不做任何改动**。

- [ ] **Step 2: 编译验证**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 3: 渐变背景与捕获层基建

**Files:**
- Modify: `ui/theme/Color.kt:24-25`（背景渐变 token）
- Modify: `ui/theme/Theme.kt`（backgroundBrush + LocalPageBackdrop 无关）
- Modify: `ui/theme/GlassEffect.kt`（删 LocalHazeState，保留 GlassTier）
- Create: `ui/liquidglass/LiquidGlassBackground.kt`（LocalPageBackdrop + ProvidePageBackdrop + 背景 Brush 辅助）
- Modify: `MainActivity.kt:48-60`（根背景渐变）

**Interfaces:**
- Produces:
  - `val LocalPageBackdrop = staticCompositionLocalOf<Backdrop?> { null }`
  - `@Composable fun ProvidePageBackdrop(backdrop: Backdrop, content: @Composable () -> Unit)`
  - `@Composable fun backgroundBrush(): Brush`（亮 `#FBF9F6→#F4E9DD`，暗 `#0C0C10→#171118` 垂直渐变）

- [ ] **Step 1: Color.kt 加渐变 token**

`ui/theme/Color.kt` 在 `_BackgroundLight/_BackgroundDark` 之后追加：

```kotlin
// ── 渐变背景（液态玻璃画布）──
val _BackgroundGradientLightStart = Color(0xFFFBF9F6)
val _BackgroundGradientLightEnd = Color(0xFFF4E9DD)   // 暖奶油
val _BackgroundGradientDarkStart = Color(0xFF0C0C10)
val _BackgroundGradientDarkEnd = Color(0xFF171118)     // 暖黑
```

- [ ] **Step 2: 新建 LiquidGlassBackground.kt**

`ui/liquidglass/LiquidGlassBackground.kt`（新文件）：

```kotlin
package com.nanji.lootarchive.ui.liquidglass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.kyant.backdrop.Backdrop
import com.nanji.lootarchive.ui.theme.*

/**
 * 页面级捕获层（Backdrop），供悬浮玻璃组件（底栏/搜索栏/FAB/弹窗）采样。
 * 页面内组件不直接采样本层（会自采样），需用局部捕获层或半透明实体。
 */
val LocalPageBackdrop = staticCompositionLocalOf<Backdrop?> { null }

@Composable
fun ProvidePageBackdrop(backdrop: Backdrop, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPageBackdrop provides backdrop, content = content)
}

/** 暖色垂直渐变背景 —— 玻璃有内容可糊的画布 */
@Composable
fun backgroundBrush(): Brush {
    val dark = LocalDarkTheme.current
    return Brush.verticalGradient(
        if (dark) listOf(_BackgroundGradientDarkStart, _BackgroundGradientDarkEnd)
        else listOf(_BackgroundGradientLightStart, _BackgroundGradientLightEnd)
    )
}

/** 降级实体玻璃底色（Android 12/12L 与无 Backdrop 作用域时使用） */
@Composable
fun liquidFallbackContainer(): Color = LocalGlassColors.current.cardBg
```

- [ ] **Step 3: GlassEffect.kt 清理**

`ui/theme/GlassEffect.kt`：删除 `import dev.chrisbanes.haze.HazeState` 与 `LocalHazeState` 定义（第 10、30 行），保留 `GlassTier` 枚举与 `Modifier.glassEffect`。

- [ ] **Step 4: MainActivity 根背景渐变**

`MainActivity.kt` 中根 Box 背景改为：

```kotlin
Box(
    Modifier
        .fillMaxSize()
        .background(backgroundBrush())
)
```

同时删除 `import dev.chrisbanes.haze.*`（若存在）。

- [ ] **Step 5: 编译验证**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL（MainScreen 仍引用 LocalHazeState 会报错，Task 5 一并处理前先临时保留 GlassEffect.kt 的 LocalHazeState 定义，Task 5 完成后再删）

> 注：若编译报 LocalHazeState 未定义（MainScreen/GlassComponents 仍用），本任务暂缓删除定义，把删除推迟到 Task 5 与 Task 4 完成之后。Step 3 按需顺延。

---

### Task 4: LiquidCard —— NeoCard 系列换实现

**Files:**
- Modify: `ui/component/GlassComponents.kt:57-99`（NeoCard）、`:37-50`（GlassSurface）、`:123-138`（HeroStatCard）
- Modify: `ui/theme/Color.kt:34-38`（玻璃 alpha 微调）

**Interfaces:**
- Consumes: `LocalGlassColors`、`LocalDarkTheme`
- Produces: 签名不变的 `NeoCard/NeoStatCard/HeroStatCard/GlassSurface/ClayCard/GlassCard/StatCard/GlassStatCard/EmptyState/NeoEmptyState`

- [ ] **Step 1: 调整玻璃 token alpha**

`Color.kt`：`_GlassLight = Color(0xAAFFFFFF)` → `Color(0x8CFFFFFF)`（55% 更通透）；`_GlassDark` 保持 `0xB21C1C24`。

- [ ] **Step 2: NeoCard 换实现（RiseCard 模式）**

删除 Haze import（`HazeStyle/HazeTint/hazeEffect`）与 `LocalHazeState` 分支，替换为半透明 + 高光描边：

```kotlin
@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val glass = LocalGlassColors.current
    val dark = LocalDarkTheme.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (dark) Modifier.border(0.5.dp, Color.White.copy(alpha = 0.07f), CardShape)
                else Modifier.border(0.5.dp, glass.glassBorder, CardShape)
            )
            .shadow(4.dp, CardShape, ambientColor = Color.White.copy(alpha = 0.3f), spotColor = glass.shadow),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = glass.glassBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick ?: {}
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}
```

- [ ] **Step 3: GlassSurface / HeroStatCard 同步**

- `GlassSurface`：`containerColor = CardBg()` → `glassBg`（半透明），其余不变
- `HeroStatCard`：`containerColor = bg` 不变（已是主色 alpha 半透明），形状/圆角不变

- [ ] **Step 4: 编译验证**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL；确认 GlassComponents.kt 无 haze import 残留

---

### Task 5: LiquidGlassButton + 首页搜索栏/FAB

**Files:**
- Create: `ui/liquidglass/LiquidGlassButton.kt`
- Modify: `ui/MainScreen.kt:186-244`（搜索栏 + FAB 换液态玻璃）

**Interfaces:**
- Consumes: `LocalPageBackdrop`（Task 3）、`InteractiveHighlight`（Task 2）
- Produces:
  - `@Composable fun LiquidGlassButton(onClick, backdrop, modifier, enabled=true, height=48.dp, horizontalPadding=16.dp, pressExpansion=4.dp, highlightIntensity=1f, highlightRadiusMultiplier=1.5f, content: @Composable RowScope.() -> Unit)`

- [ ] **Step 1: 新建 LiquidGlassButton.kt**

自研实现（参照 AndroidLiquidGlass Demo 的液态按钮模式，Apache-2.0 思路，非复制 GPL 源码）：

```kotlin
package com.nanji.lootarchive.ui.liquidglass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousCapsule
import com.nanji.lootarchive.ui.theme.LocalDarkTheme
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 48.dp,
    horizontalPadding: Dp = 16.dp,
    pressExpansion: Dp = 4.dp,
    highlightIntensity: Float = 1f,
    highlightRadiusMultiplier: Float = 1.5f,
    content: @Composable RowScope.() -> Unit
) {
    val dark = LocalDarkTheme.current
    val containerColor =
        if (dark) Color(0xFF121212).copy(alpha = 0.40f)
        else Color(0xFFFFFFFF).copy(alpha = 0.45f)
    val animationScope = rememberCoroutineScope()
    val highlight = remember(animationScope) {
        InteractiveHighlight(
            animationScope,
            intensity = highlightIntensity,
            radiusMultiplier = highlightRadiusMultiplier
        )
    }
    Row(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousCapsule },
                effects = { vibrancy(); blur(2.dp.toPx()); lens(12.dp.toPx(), 24.dp.toPx()) },
                layerBlock = {
                    val width = size.width
                    val heightPx = size.height
                    if (width > 0f && heightPx > 0f) {
                        val progress = highlight.pressProgress
                        val scale = lerp(1f, 1f + pressExpansion.toPx() / heightPx, progress)
                        val maxOffset = size.minDimension
                        val offset = highlight.offset
                        if (maxOffset > 0f) {
                            translationX = maxOffset * tanh(0.05f * offset.x / maxOffset)
                            translationY = maxOffset * tanh(0.05f * offset.y / maxOffset)
                        }
                        val maxDragScale = pressExpansion.toPx() / heightPx
                        val offsetAngle = atan2(offset.y, offset.x)
                        scaleX = scale + maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) * (width / heightPx).coerceAtMost(1f)
                        scaleY = scale + maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) * (heightPx / width).coerceAtMost(1f)
                    }
                },
                onDrawSurface = { drawRect(containerColor) }
            )
            .clickable(
                enabled = enabled,
                interactionSource = null,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .then(highlight.modifier)
            .then(highlight.gestureModifier)
            .height(height)
            .padding(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
```

- [ ] **Step 2: MainScreen 换搜索栏与 FAB**

`MainScreen.kt`：
- 搜索栏（186-223 行）：删 HazeStyle 代码，改 LiquidGlassButton：

```kotlin
            if (isHome) {
                val backdrop = LocalPageBackdrop.current ?: return@Box
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
```

- FAB（225-243 行）改 LiquidGlassButton：

```kotlin
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
```

- [ ] **Step 3: 编译验证**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 6: LiquidGlassBottomBar 底部导航

**Files:**
- Create: `ui/liquidglass/LiquidGlassBottomBar.kt`（含 LiquidBottomTabs/LiquidBottomTab/LiquidGlassBottomBar）
- Modify: `ui/MainScreen.kt:246-304`（GlassPanel → LiquidGlassBottomBar）
- Delete: `ui/component/GlassPanel.kt`

**Interfaces:**
- Consumes: `DampedDragAnimation`、`InteractiveHighlight`、`LocalPageBackdrop`
- Produces:
  - `@Composable fun LiquidGlassBottomBar(selectedTabIndex: Int, onTabSelected: (Int) -> Unit, backdrop: Backdrop, modifier: Modifier = Modifier)`
  - `@Composable fun LiquidSegmentedControl(options: List<LiquidSegmentOption>, selectedIndex: Int, onSelected: (Int) -> Unit, backdrop: Backdrop, modifier, containerHeight=64.dp, contentPadding=4.dp, showIcons=true, labelFontSize=13.sp, showSelectionShadow=true)`
  - `data class LiquidSegmentOption(val label: String, val icon: ImageVector)`

- [ ] **Step 1: 自研液态底栏**

参照 AndroidLiquidGlass 1.0.0 Demo 的公开模式（采样 Row + 选中镜头 + DampedDragAnimation）实现 `LiquidGlassBottomBar.kt`，要点：

1. **结构**（自研代码，完整实现见本步下方骨架）：
   - 外层 `BoxWithConstraints`，内三块：可见 `Row`（drawBackdrop 面板 + vibrancy/blur(8dp)/lens(24dp) + InteractiveHighlight + 按压缩放 `lerp(1, 1+16dp/size.width, pressProgress)`）；隐藏采样 `Row`（`alpha(0f)` + `layerBackdrop(tabsBackdrop)` + `drawBackdrop` 第二层 + `ColorFilter.tint(选中色)`）；选中胶囊 `Box`（`rememberCombinedBackdrop(backdrop, tabsBackdrop)` + `lens(10dp,14dp,chromaticAberration=true)` + `Highlight.Default.copy(alpha=progress)` + `Shadow(alpha=progress)` + `InnerShadow(8dp*progress)` + `drawRect(Color.Red, BlendMode.Clear)` onDrawBehind + 表面 `Black/White.copy(0.1f)*(1-progress)`）
   - `DampedDragAnimation`：initialValue=selectedIndex，valueRange=0..2，pressedScale=78/56，onDragStopped 时 round 到最近 tab 并 `currentOnTabSelected(targetIndex)` + `animateToValue` + 归位 offsetAnimation；onDrag 用 `dragAmount.x / tabWidth` 更新
   - 手势：选中胶囊 Box 挂 `dampedDragAnimation.modifier` + `interactiveHighlight.gestureModifier`
   - 采样层 Row 用 `LocalLiquidBottomTabSampling` CompositionLocal 控制图标实心/空心（`ColumnScope` 内容由调用方提供）
2. **适配 LootArchive**：
   - `LocalRiseDarkTheme` → `LocalDarkTheme`
   - accentColor（选中胶囊/图标色）→ `Primary()`（`MaterialTheme.colorScheme.primary`）
   - 面板底色：亮 `Color(0xFFFBF9F6).copy(alpha = 0.14f)` / 暗 `Color(0xFF121212).copy(alpha = 0.14f)`
   - 图标用 Material Icons（`Icons.Rounded.*` / `Icons.Outlined.*`），无需 AppIcons
   - 文字 12.sp、图标 28.dp、`ContinuousCapsule` 形状
3. **顶部接口**（`LiquidGlassBottomBar`）与 RiseDiary 同形但去掉飞行按钮：三个 tab（首页/统计/我的），`content` 参数收图标+标签

骨架（关键部分，实现时补全）：

```kotlin
@Composable
fun LiquidGlassBottomBar(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    tabs: List<Pair<ImageVector, ImageVector>> = listOf(...) // (outlined, rounded)
) {
    // visualSelectedTabIndex + pendingNavigationIndex 双状态（点击先变视觉再延迟回调）
    // → LiquidBottomTabs(selectedTabIndex = { visual }, onTabSelected = selectTab, backdrop, 3, Modifier.fillMaxWidth())
    //   内部：BoxWithConstraints + DampedDragAnimation + 可见Row(drawBackdrop面板) + 隐藏采样Row + 选中胶囊Box
}
```

- [ ] **Step 2: MainScreen 接入底栏**

替换 246-304 行的 GlassPanel 调用：

```kotlin
            if (!isSubPage) {
                val backdrop = LocalPageBackdrop.current ?: return@Box
                LiquidGlassBottomBar(
                    selectedTabIndex = currentTab,
                    onTabSelected = ::switchTab,
                    backdrop = backdrop,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp, start = 20.dp, end = 20.dp)
                )
            }
```

- [ ] **Step 3: 删除 GlassPanel.kt**

删除 `ui/component/GlassPanel.kt`（其 Haze 引用随之消失）。

- [ ] **Step 4: MainScreen 捕获层接入**

`MainScreen.kt` 结构改造（85-115 行区域）：

```kotlin
    val hostState = rememberLiquidDialogHostState()
    val backdrop = rememberLayerBackdrop {
        drawRect(backgroundBrush())
        drawContent()
    }
    ...
    Scaffold(containerColor = Color.Transparent, topBar = {}) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            ProvidePageBackdrop(backdrop) {
                Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                    AnimatedContent(...) { ... }   // 原内容
                }
                // 悬浮层：搜索栏 / FAB / 底栏（兄弟节点，采样 backdrop）
                ...searchBar/FAB/LiquidGlassBottomBar...
                LiquidDialogHost(hostState, backdrop)   // Task 8 加入
            }
        }
    }
```

- [ ] **Step 5: 编译验证**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL；`grep -r "dev.chrisbanes" app/src` 仅剩 GlassComponents 的 NeoAlertDialog 部分时属预期（Task 8 清理），MainScreen 应已无 haze 引用

---

### Task 7: LiquidToggle + 设置页液态化

**Files:**
- Create: `ui/liquidglass/LiquidToggle.kt`
- Modify: `ui/settings/SettingsScreen.kt:62-192`（两个 Switch + 主题模式 FilterChip + 卡片）

**Interfaces:**
- Consumes: `DampedDragAnimation`、`LocalPageBackdrop`（降级 null 时实体）
- Produces: `@Composable fun LiquidToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit, backdrop: Backdrop, modifier: Modifier = Modifier, enabled: Boolean = true)`

- [ ] **Step 1: 自研 LiquidToggle**

参照 AndroidLiquidGlass Demo 模式实现（RiseDiary LiquidToggle 同源但不可复制，按公开模式自研）：
- 轨道：`64x28dp` 胶囊，`layerBackdrop(trackBackdrop)` + 轨道色 `lerp(trackColor, accentColor, dampedDragAnimation.value)`；trackColor 亮 `0xFF787878.copy(0.2f)` / 暗 `0xFF787880.copy(0.36f)`；accentColor = `Primary()`
- 拇指：`40x24dp`，Android 13+ 用 `drawBackdrop(rememberCombinedBackdrop(backdrop, rememberBackdrop(trackBackdrop){...scale...}))` + `blur(8dp*(1-progress))` + `lens(5dp,10dp,chromaticAberration)` + `Highlight.Ambient`（宽/模糊半径/3 缩放）+ `Shadow(4dp, Black 0.05)` + `InnerShadow(4dp*progress)`；<13 降级 `shadow(4.dp, ContinuousCapsule) + background(White, ContinuousCapsule)`
- 拖动：`DampedDragAnimation(0..1, pressedScale=1.5f)`，onDragStopped 时 `requestedToggleState(currentChecked, fraction)`；`consumeDragChanges=true, consumeInitialDown=false`
- 语义：`toggleableState`、`role=Role.Switch`、外层 `64x48dp` 点击区
- 完整实现参考 `LiquidGlassControls.kt` 的结构（公开模式），但配色用本项目 token

- [ ] **Step 2: 设置页两行 Switch → LiquidToggle**

`SettingsScreen.kt` 中"跟随壁纸动态色"（98-109）与"备份提醒"（180-190）两行，参照 RiseDiary SettingsToggleItem 行内捕获层模式：

```kotlin
                    val rowBackdrop = rememberLayerBackdrop {
                        drawRect(if (LocalDarkTheme.current) Color(0xFF20242B) else Color(0xF7FFFFFF))
                        drawContent()
                    }
                    Box(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().layerBackdrop(rowBackdrop)
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("跟随壁纸动态色", fontSize = 15.sp, color = TextPrimary(), modifier = Modifier.weight(1f))
                            Text("Android 12+", fontSize = 11.sp, color = TextAuxiliary(), modifier = Modifier.padding(end = 8.dp))
                        }
                        LiquidToggle(
                            checked = uiState.dynamicColor,
                            onCheckedChange = viewModel::setDynamicColor,
                            backdrop = rowBackdrop,
                            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 14.dp)
                        )
                    }
```

备份提醒行同理（去掉 Switch，LiquidToggle 对齐 CenterEnd）。

- [ ] **Step 3: 主题模式 FilterChip → 液态分段控件（可选加分项）**

若 LiquidSegmentedControl 已随 Task 6 实现，将 83-94 行的三枚 FilterChip 换成行内捕获层 + LiquidSegmentedControl（选项：跟随/浅色/深色，无图标，labelFontSize=12.sp，containerHeight=40.dp）。否则保留 FilterChip。

- [ ] **Step 4: 编译验证**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 8: LiquidDialog + 全站弹窗替换

**Files:**
- Create: `ui/liquidglass/LiquidDialog.kt`（LiquidDialogHostState/LiquidDialogHost/LiquidDialog/LiquidAlertDialog）
- Modify: `ui/component/GlassComponents.kt:163-170`（NeoAlertDialog 内部换 LiquidDialog）
- Modify: `ui/MainScreen.kt`（挂 LiquidDialogHost）
- Modify（机械替换 AlertDialog → LiquidAlertDialog）：
  - `ui/MyLandingScreen.kt`（358/399/410/421/432/459/470/511/563/615 共 10 处）
  - `ui/home/HomeScreen.kt:249`
  - `ui/category/CategoryScreen.kt:102,140`
  - `ui/backup/BackupScreen.kt:171,187`
  - `ui/settings/SettingsScreen.kt:256`
  - `ui/component/WheelDatePickerDialog.kt:77`

**Interfaces:**
- Consumes: `LocalPageBackdrop`、`LocalGlassColors`
- Produces:
  - `class LiquidDialogHostState`（`rememberLiquidDialogHostState()` + `ProvideLiquidDialogHost`）
  - `@Composable fun LiquidDialogHost(state, backdrop, modifier)`
  - `@Composable fun LiquidDialog(onDismissRequest, modifier, alignment=Center, shape=ContinuousRoundedRectangle(48.dp), contentPadding, properties, content: @Composable ColumnScope.() -> Unit)`
  - `@Composable fun LiquidAlertDialog(onDismissRequest: () -> Unit, title: @Composable () -> Unit = {}, text: @Composable () -> Unit = {}, confirmButton: @Composable () -> Unit = {}, dismissButton: @Composable () -> Unit = {})` —— 镜像 Material3 AlertDialog 参数形状

- [ ] **Step 1: 自研 LiquidDialog**

参照 AndroidLiquidGlass Demo 公开模式实现（**自研**，不使用 RiseDiary GPL 源码）：
- `LiquidDialogHostState`：`currentEntry`（key + onDismissRequest + content），`show()/dismiss()`
- `LiquidDialogHost`：AnimatedVisibility + scrim（亮 `Color(0xFF29293A).copy(alpha=0.23f)` / 暗 `Color(0xFF121212).copy(alpha=0.56f)`）+ BackHandler + 弹窗缩放 `spring(0.6, 250)` 入场 / tween(140) 退场；内容回调注入 backdrop
- `LiquidDialog`：有 host 时注册 entry（DisposableEffect）；无 host 时平台 Dialog + `SolidDialogSurface` 实体降级（亮 `0xFFF8F9FA` / 暗 `0xFF202124`，28dp 圆角对齐现有 NeoAlertDialog 观感）
- `LiquidDialogSurface`：`drawBackdrop` + `colorControls(brightness=亮0.2/暗0, saturation=1.5)` + `blur(亮16dp/暗8dp)` + `lens(24dp,48dp,depthEffect=true)` + `Highlight.Plain` + 表面 `0xFFFAFAFA.copy(0.82f)` / `0xFF121212.copy(0.72f)`；`widthIn(max=520dp)`、`imePadding()`、`navigationBarsPadding()`、pointer 事件保持路径
- 文案色：title 用 `TextPrimary()`、正文 `TextSecondary()`、确认钮 `Primary()`、取消钮 `TextSecondary()`（替换 RiseDiary 的 miuix ButtonDefaults）

- [ ] **Step 2: NeoAlertDialog 换实现**

`GlassComponents.kt`：

```kotlin
@Composable
fun NeoAlertDialog(
    title: String, message: String,
    confirmText: String = "确认", dismissText: String = "取消",
    onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    LiquidAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.SemiBold, color = TextPrimary()) },
        text = { Text(message, color = TextSecondary(), fontSize = 14.sp) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText, color = Primary(), fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismissText, color = TextSecondary()) } }
    )
}
```

- [ ] **Step 3: MainScreen 挂 Host**

Task 6 Step 4 的骨架中加入：

```kotlin
            ProvidePageBackdrop(backdrop) {
                Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) { ... }
                ...
                LiquidDialogHost(hostState, backdrop)
            }
```

`hostState` 用 `rememberLiquidDialogHostState()` + `ProvideLiquidDialogHost(hostState) { ... }` 包裹整棵 UI 树（含 AnimatedContent 内各页面），使页面内 NeoAlertDialog/GlassAlertDialog 能拿到 host。

- [ ] **Step 4: 机械替换 16 处 AlertDialog**

每处按原参数逐项平移，模式示例（HomeScreen.kt:249 保修弹窗）：

```kotlin
// 原：
AlertDialog(onDismissRequest = { showWarrantyDialog = false }, shape = RoundedCornerShape(28.dp),
    containerColor = MaterialTheme.colorScheme.surface, title = { Text(...) }, text = { ... },
    confirmButton = { ... }, dismissButton = { ... })
// 新：
LiquidAlertDialog(
    onDismissRequest = { showWarrantyDialog = false },
    title = { Text(...) },
    text = { ... },
    confirmButton = { ... },
    dismissButton = { ... }
)
```

规则：删掉 `shape/containerColor/tonalElevation` 参数（LiquidAlertDialog 内置玻璃样式）；`title/text/confirmButton/dismissButton` 内容原样保留。
- MyLandingScreen 10 处、HomeScreen 1 处、CategoryScreen 2 处、BackupScreen 2 处、SettingsScreen 1 处、WheelDatePickerDialog 1 处。

- [ ] **Step 5: 编译验证 + Haze 清零**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL
Run: `rg -n "dev\.chrisbanes|HazeState|hazeEffect|hazeSource|LocalHazeState|HazeStyle|HazeTint" app/src app/build.gradle.kts`
Expected: 无匹配（全部清除）

---

### Task 9: DetailScreen 玻璃 Sheet + 收尾

**Files:**
- Modify: `ui/detail/DetailScreen.kt:54-90`（BottomSheetScaffold sheetContainerColor）、`:162`（ModalBottomSheet）
- Modify: `app/build.gradle.kts:129-131`（Haze 依赖移除）

- [ ] **Step 0: 移除 Haze 依赖**

确认 `rg -n "dev\.chrisbanes" app/src` 无匹配后，删除 `app/build.gradle.kts` 中两行 haze 依赖（保留 Task 1 新增的 kyant 两行），并将注释改为：

```kotlin
    // Liquid Glass (Kyant Backdrop — blur/lens/vibrancy RuntimeShader)
    implementation("io.github.kyant0:backdrop:1.0.0")
    implementation("io.github.kyant0:capsule:2.1.1")
```

- [ ] **Step 1: 详情页 Sheet 玻璃化**

`BottomSheetScaffold` 的 `sheetContainerColor` 与 `ModalBottomSheet` 的 `containerColor` 改为半透明玻璃色（无独立窗口捕获，采用半透明 + 圆角即可在渐变上呈现玻璃感）：

```kotlin
sheetContainerColor = if (LocalDarkTheme.current) Color(0xE61C1C24) else Color(0xF2FFFFFF),
```

（`0xE6` ≈ 90%、`0xF2` ≈ 95% 不透明度，保证文字可读性）

- [ ] **Step 2: 全局检查**

Run: `rg -n "dev\.chrisbanes|haze" app/src/main/java`
Expected: 无匹配
Run: `rg -n "GlassPanel" app/src`
Expected: 无匹配

- [ ] **Step 3: 编译验证**

Run: `.\gradlew.bat :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 10: 全量验证

**Files:** 无（验证任务）

- [ ] **Step 1: 完整编译**

Run: `.\gradlew.bat :app:assembleDebug`
Expected: BUILD SUCCESSFUL，产出 `app/build/outputs/apk/debug/LootArchive-debug-v6.7.0.apk`

- [ ] **Step 2: 单元测试**

Run: `.\gradlew.bat :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL，全部测试通过

- [ ] **Step 3: 依赖审计**

Run: `.\gradlew.bat :app:dependencies --configuration debugRuntimeClasspath | Select-String "haze|kyant"`
Expected: 仅出现 `io.github.kyant0:backdrop` 与 `io.github.kyant0:capsule`，无 `dev.chrisbanes.haze`

- [ ] **Step 4: 遗留项复核**

- `grep -r "haze"` 全仓库（含 build 缓存外源码）无残留
- GlassEffect.kt 已无 LocalHazeState（Task 3/5 顺延项确认完成）
- 文档：`docs/superpowers/specs/2026-09-01-liquid-glass-design.md` 中所有改造点均有对应落地

---

## Self-Review 结论

- **Spec 覆盖**：依赖替换(T1)、兼容策略(T1/T3 降级分支、T2 全组件)、捕获层架构(T3/T6/T8)、组件清单(T2-T8 全部)、渐变背景(T3)、暖色调保留(T4/T6/T7 token 复用)、玻璃 alpha 微调(T4)、全站弹窗(T8)、设置页(T7)、DetailSheet(T9)、验证(T10) —— 全部有对应任务
- **占位符扫描**：所有代码步骤含实际内容；LiquidGlassBottomBar 采用"骨架 + 要点清单 + 参照源路径"（自研实现无法预生成 500 行成品，但要点与公开模式引用明确，非 TBD）
- **类型一致性**：`LocalPageBackdrop`、`ProvidePageBackdrop`、`backgroundBrush()`、`LiquidGlassButton`、`LiquidGlassBottomBar`、`LiquidSegmentedControl`、`LiquidSegmentOption`、`LiquidToggle`、`LiquidDialogHostState/rememberLiquidDialogHostState/ProvideLiquidDialogHost/LiquidDialogHost/LiquidDialog/LiquidAlertDialog` 跨任务签名一致