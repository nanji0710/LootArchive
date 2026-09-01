# LootArchive 液态玻璃（Liquid Glass）改造设计文档

日期：2026-09-01
状态：已确认（用户审批通过）

## 1. 背景与目标

LootArchive（拾物集）当前使用 Haze 1.5.2 实现暖色玻璃拟态（Warm Glassmorphism）。参考 RiseDiary 项目的玻璃效果，将全站 UI 升级为 Kyant Backdrop 驱动的"液态玻璃"（Liquid Glass）：模糊 + 折射（lens）+ 活力（vibrancy）+ 按压高光 + 弹簧物理交互。

目标：
- 全站玻璃质感升级为液态玻璃（底栏、搜索栏、FAB、卡片、弹窗、按钮、设置控件）
- 彻底移除 Haze 依赖
- 保留 LootArchive 的暖琥珀视觉基调、字体体系、用户可选主色/动态取色功能
- 不改动任何数据层、逻辑层、业务代码

## 2. 现状分析

- 技术栈：Compose + Material 3，BOM 2025.06.00，Kotlin 2.1.0，minSdk 31 / targetSdk 36
- Haze 使用点（4 处文件）：
  - `MainScreen.kt`：`hazeState` + `hazeSource` 容器 + `LocalHazeState` + 悬浮搜索栏（HazeStyle blur 20dp）+ 胶囊 FAB（纯色 Primary）
  - `ui/theme/GlassEffect.kt`：`LocalHazeState` 定义 + `GlassTier` 规格
  - `ui/component/GlassPanel.kt`：底部导航玻璃面板（hazeEffect blur 24dp）
  - `ui/component/GlassComponents.kt`：`NeoCard` 内部 hazeEffect（blur 20dp）
- 玻璃组件调用点：28 处（NeoCard/GlassSurface/NeoStatCard/HeroStatCard/NeoAlertDialog/EmptyState 等），保持 API 不变即可零改动
- 色板：琥珀主色 `#E8782A`，玻璃 token 集中在 `Color.kt` 的 `GlassColorScheme`（Light/Dark）
- 构建环境：JDK 17 Corretto，Android SDK 就绪，依赖走阿里云镜像 + google + mavenCentral

## 3. 技术路线

### 3.1 依赖替换（app/build.gradle.kts）

- 移除：`dev.chrisbanes.haze:haze:1.5.2`、`dev.chrisbanes.haze:haze-materials:1.5.2`
- 新增：`io.github.kyant0:backdrop:1.0.0`、`io.github.kyant0:capsule:2.1.1`
- 不引入 miuix（Material 3 项目不混用整套控件库）
- backdrop/capsule 均在 mavenCentral，现有仓库配置（含阿里云镜像代理）可解析；若阿里云镜像拉取失败，在 dependencyResolutionManagement 中把 mavenCentral() 提前

### 3.2 兼容策略（minSdk 31 = Android 12）

沿用 RiseDiary 模式：`useFullGlass = Build.VERSION.SDK_INT >= TIRAMISU`。
- Android 13+：完整液态玻璃（AGSL RuntimeShader：blur/lens/vibrancy/Highlight/Shadow/InnerShadow）
- Android 12 / 12L：自动降级为半透明实体样式（透明色 + 白描边 + 阴影），功能不受影响

## 4. 核心架构：捕获层模式（替代 Haze）

```
MainActivity 根背景 → 暖色垂直渐变 backgroundBrush()
  └─ MainScreen: rememberLayerBackdrop { drawRect(渐变); drawContent() }  ← 整页捕获层
       ├─ 页面内容（AnimatedContent）
       ├─ 悬浮搜索栏 / FAB → drawBackdrop 采样模糊
       ├─ 底部导航 → LiquidGlassBottomBar（拖拽镜头 + 按压形变）
       └─ LocalPageBackdrop 下传给子组件（卡片/弹窗/设置行各自采样）
```

- 删除 `hazeSource` / `LocalHazeState`（GlassEffect.kt 重写）
- 新增 `LocalPageBackdrop`（CompositionLocal，类型为 backdrop 库的 BackdropState 或 null，null 时组件走实体降级样式）

## 5. 组件清单（新包 `ui/liquidglass/`）

从 RiseDiary 移植并适配琥珀色调、Material 3 控件语义：

| 新文件 | 作用 | 关键参数 |
|---|---|---|
| `DampedDragAnimation.kt` | 弹簧物理（按压/拖拽/缩放/速度） | 移植自 RiseDiary |
| `DragGestureInspector.kt` | 底栏拖拽手势检测 | 移植自 RiseDiary |
| `InteractiveHighlight.kt` | RuntimeShader 圆形按压高光（API<33 降级 Color+BlendMode.Plus） | 移植自 RiseDiary |
| `LiquidGlassBottomBar.kt` | 3 Tab 悬浮胶囊底栏：可拖拽选中镜头 + 按压形变 + 高光 | vibrancy + blur(8dp) + lens(24dp) + 选中胶囊 lens(10dp,14dp,chromaticAberration) + Shadow/InnerShadow |
| `LiquidGlassButton.kt` | 液态玻璃按钮（搜索栏、FAB、返回/新增按钮） | vibrancy + blur(2dp) + lens(12dp,24dp) + InteractiveHighlight |
| `LiquidCard.kt` | 液态玻璃卡片（替换 NeoCard 内部实现） | vibrancy + blur(8dp) + lens(12dp,24dp) + 玻璃边框 + 阴影；<13 降级 CardBg 实体 |
| `LiquidDialog.kt` | 液态玻璃对话框（替换 NeoAlertDialog） | colorControls(brightness, saturation=1.5) + blur(8/16dp) + lens(24dp,48dp) + Highlight.Plain + scrim；<13 降级实体 |
| `LiquidToggle.kt` | 设置页液态开关 | blur(8dp×(1-progress)) + lens + Highlight.Ambient + Shadow/InnerShadow；<13 降级 |
| `LiquidSlider.kt` | 设置页液态滑杆 | 同上 |
| `LiquidGlassBackground.kt` | backgroundBrush() 渐变 + 捕获层辅助（rememberLayerBackdrop + layerBackdrop 封装） | 暖色垂直渐变 |

组件 API 对齐原则：`NeoCard`/`NeoStatCard`/`HeroStatCard`/`NeoAlertDialog`/`EmptyState`/`GlassSurface` 等公开函数签名不变，仅内部实现切换，28 处调用点零改动。

## 6. 主题与视觉（保留暖琥珀调）

- **渐变背景**（参考 RiseDiary backgroundBrush，暖色化）：
  - 浅色：`#FBF9F6` → `#F4E9DD`（暖奶油）
  - 深色：`#0C0C10` → `#171118`（暖黑）
  - 应用于 MainActivity 根背景 + 捕获层 drawRect
- **玻璃色板**：沿用 `GlassColorScheme` token，微调 alpha（卡片 glassBg 0.65→0.55 更通透），边框保持白色半透明高光
- **不变**：主色琥珀、动态取色、亮暗三模式、Fredoka/Nunito 字体、`Primary()`/`TextAuxiliary()` 等便捷函数
- **形状**：底栏/按钮用 capsule 库 `ContinuousCapsule`；卡片维持 20dp 圆角

## 7. 界面改造点

| 文件 | 改动 |
|---|---|
| `app/build.gradle.kts` | 依赖替换 |
| `ui/MainScreen.kt` | 删 hazeSource → 捕获层 + LocalPageBackdrop；搜索栏/FAB → LiquidGlassButton；GlassPanel → LiquidGlassBottomBar |
| `ui/component/GlassComponents.kt` | NeoCard/NeoAlertDialog 内部换实现（签名不变） |
| `ui/component/GlassPanel.kt` | 删除 |
| `ui/theme/GlassEffect.kt` | 重写：LocalPageBackdrop + 玻璃规格 token |
| `ui/theme/Color.kt` | 渐变背景色 + alpha 微调 |
| `ui/theme/Theme.kt` | backgroundBrush() |
| `MainActivity.kt` | 根背景换渐变 |
| `ui/settings/SettingsScreen.kt` | 开关/滑杆/设置行 → 液态玻璃控件 |
| `ui/detail/DetailScreen.kt` | BottomSheet 底部面板 → 玻璃 Sheet |

## 8. 验证

- `./gradlew :app:assembleDebug` 编译通过
- `./gradlew test` 现有单元测试通过
- 数据层/逻辑层零改动

## 9. 风险与对策

| 风险 | 对策 |
|---|---|
| backdrop 库与 Compose BOM 2025.06.00 编译兼容性 | 若有冲突，微调 backdrop 版本或升级 BOM 小版本 |
| 阿里云镜像拉取 kyant0 构件失败 | mavenCentral() 提前/直接解析 |
| 物理交互代码复杂度高 | 先移植底栏（核心视觉）跑通，再铺开其余组件 |
| R8 minify（release）与 backdrop AGSL | 以 debug 验证为主；必要时补 proguard 规则 |