# jicun（即存）→ LootArchive UI / 动效参考改造分析

日期：2026-09-30
对比对象：`D:\GitHub\jicun`（Flutter，v3.2.2+12） vs `D:\GitHub\LootArchive`（原生 Compose，v6.9.6）
范围：动效系统 · 玻璃组件落地 · 视觉 token 收敛 · 无障碍 · 页面结构与导航

> 本文只做分析与建议，**未改动任何代码**。所有数字均为实测（读源码 / grep 统计），非估计。

---

## 0. 结论摘要

**先说最重要的一句：两个项目的 UI 地基不同，不存在"照搬"这条路。** jicun 是 Flutter/Dart，LootArchive 是 Kotlin + Jetpack Compose，一个组件都拿不过来。能借的只有**设计决策、具体数值和实现思路**。

**第二个反直觉的结论：LootArchive 的玻璃体系比 jicun 更完整。** jicun 只有底栏是真玻璃，卡片是"假玻璃"（半透明、不模糊）；LootArchive 有 10 个自研液态玻璃组件、9 个玻璃语义 token、5 份设计文档。**所以这不是"落后项改造"，而是选择性吸收。**

那么真正值得参考的是什么？按性价比排序：

| 优先级 | 项 | 现状差距 | 成本 |
|:--|:--|:--|:--|
| **1** | 动效 token 化 | LootArchive **无任何动效常量文件**，14 处 `tween` + 9 处 `spring` 全散写 | 低（新建 1 个文件） |
| **2** | 无障碍（减弱动效） | `reduceMotion`/`disableAnimations` **全项目 0 处** | 低 |
| **3** | 字号 token 化 | `MaterialTheme.typography` **0 处**，硬编码 `fontSize` **260 处** | 中 |
| **4** | 玻璃组件落地 | `GlassSurface` 全项目**仅 1 处**调用，各页面直接用原生 `Card(` | 中 |
| **5** | 页面骨架统一 | `ShaderMask`/`stickyHeader` **全项目 0 处**，14 个页面各滚各的 | 中 |
| **6** | 圆角/间距收敛 | **18 种**不同圆角值；卡片间距 **5 种**（6/8/12/14/16）；`GlassTier` 枚举实际是死代码 | 中 |
| **7** | 阴影策略反转 | jicun **零阴影**；LootArchive **15 处 `defaultElevation` + 5 处 `.shadow(4.dp)`**，而卡片间距仅 12 | 低（需目视确认） |

**注意：LootArchive 的 `docs/optimization-plan.md` 执行状态表已过时。** 该文档记录的 P0 硬伤（DB 迁移缺口、提醒 Worker 未调度）**现已修复**——已验证 `DatabaseModule.kt` 补全了 1→2 到 5→6 完整迁移链，`LootArchiveApp.kt:38-40` 调度了全部 3 个 Worker。判断现状时别引用那张表。

---

## 1. 技术栈对比

### 1.1 根本差异

| 维度 | jicun | LootArchive |
|:--|:--|:--|
| 框架 | Flutter / Dart SDK ^3.13.3 | Kotlin 2.0 + Jetpack Compose |
| UI 体系 | Cupertino（iOS 风）为主 + 局部 Material 3 | Material 3（BOM 2025.06.00） |
| 玻璃实现 | 第三方包 `liquid_glass_widgets`（锁定 1.7.2） | 自研 `ui/liquidglass/`（Kyant backdrop 1.0.0 + capsule 2.1.1） |
| 状态管理 | 根 `State` + `ShellController` 抽象接口 | Hilt + ViewModel + `StateFlow` |
| 导航 | Cupertino 路由 + 自绘底栏 | 手写 `currentRoute`/`backStack` + `AnimatedContent` |
| 数据库 | 无（JSON 落盘 `history_store`） | Room 2.6.1（7 实体 / 7 DAO / 4 仓库） |
| DI | 无 | Hilt 2.53.1 |
| 图标 | SVG 资源（`flutter_svg`，`BlendMode.srcIn` 染色） | Material Icons Rounded（`ImageVector`） |
| 代码量 | 34 文件 / **13,765 行** | 89 文件 / **10,959 行** |
| 测试 | 23 单测 + 3 集成测试 | 5 单测 / 184 行 |

### 1.2 一个关键的文化差异

jicun 的注释密度极高，且**记录的是实测数据、被否决的方案和帧预算推理**。例如：

- `glass.dart:302-309` 解释为什么卡片**故意不挂** `BackdropFilter`（背景是平滑渐变，模糊它像素几乎不变，但每帧要为每张卡片跑一次全宽模糊）；
- `glass.dart:307-309` 解释为什么卡片**故意不画投影**（间距只有 12，投影会越过间隙盖到下一张卡，深色下连成一条黑带）；
- `main.dart:99-103` 记录底栏切 tab 走根 `setState` 实测 **build 尖峰 40~47ms**，120Hz 上掉 5~6 帧，因此改用 `ValueNotifier`；
- `preview.dart:605-611` 记录 8K / 34.7 Mbps 码流**打爆 Java 堆**，因此强制取最低码率。

LootArchive 也有这个传统（`docs/` 下的设计文档、代码里的实测注释如"386×63 逻辑px"）。**这类"为什么不那样做"的记录是本次参考里最值钱的部分**，比色号本身重要。

---

## 2. 差距逐一拆解

> 下面 2.1–2.5 展开上表前五项；第 6/7 项（圆角间距、阴影）在 §3.2–§3.3 展开。

### 2.1 动效系统 —— 差距最大，成本最低

**jicun 的做法：集中 token + 记录曲线意图**

`lib/ui/motion.dart`（227 行）把动效规格收在一处：

| 常量 | 值 | 意图 |
|:--|:--|:--|
| `kRevealExpand` | 460ms | 展开比收起慢：一次滑出一整块内容，太快像被弹开 |
| `kRevealCollapse` | 420ms | 收起稍快更利落，但同样留回弹 |
| `kRevealExpandCurve` | `Cubic(0.35, 0.30, 0.45, 1.25)` | **回弹留在末尾**：前段匀速铺开，最后冲过目标高度再落回 |
| `kRevealCollapseCurve` | `Cubic(0.70, -0.50, 0.40, 1.0)` | **anticipation**：前 1/3 下探（箱子先涨 ~9%）再收到 0 |

其他具名规格：

- `StaggerIn`：560ms 总时长，`Interval((index * 0.22).clamp(0, 0.7), 1, easeOutCubic)`（每张约错开 120ms），上浮 16px + 淡入
- `SubPageRoute`：**320ms**（对比 Cupertino 默认 500ms），且 `barrierColor => null` 干掉压在下层的 9% 黑罩
- `GlassDialogRoute`：180ms 淡入 `Curves.easeOut`，`barrierCurve = Interval(0, 0.4, easeOut)`（遮蔽在前 40% 铺满，不跟面板慢慢爬）
- `ShortBounceScrollPhysics`：`frictionFactor × 0.5`（正常档 0.52 → 0.26）

**LootArchive 的现状：**

- **没有任何动效常量文件**（已验证：无 `Motion.kt`/`Durations.kt`/`Tokens.kt`）
- 散写：`tween(` 14 处、`spring(` 9 处、`Animatable(` 8 处
- 实测散落值（部分）：

| 位置 | 值 |
|:--|:--|
| `MainScreen.kt:125-126` | `tween(220)` / `tween(180)`（页面转场） |
| `AboutScreen.kt:301,306,311` | `tween(9000)` / `tween(13000)` / `tween(7000)`（光斑背景） |
| `GlassComponents.kt:127` | `tween(2000, EaseInOutCubic)`（空状态浮动） |
| `HomeScreen.kt:93-95` | `tween(600, EaseOutCubic)` ×3（数字滚动） |
| `LiquidDialog.kt:132-147` | `tween(180)/tween(140)/tween(160)/tween(120)` + `spring(0.6f, 250f)` |
| `LiquidGlassBottomBar.kt:186` | `spring(1f, 300f, 0.5f)` |
| `InteractiveHighlight.kt:47,49` | `spring(0.5f, 300f, 0.001f)` |
| `DampedDragAnimation.kt:51-59` | 5 条 `spring(...)` |

同一类动作在不同页面用了不同时长（例如转场 220ms vs 弹窗 180/160/140ms vs 数字 600ms），**没有统一意图**。

**建议：**
新建 `ui/theme/Motion.kt`，把时长与曲线收口。可以先只收"有意图"的那几条（交互动效），把 `AboutScreen` 的光斑（9000/13000/7000ms）这类装饰性动画单独归为"环境动效"不混进来。jicun 的 overshoot / anticipation 两条曲线值得直接借用数值。

#### jicun 全量动效规格（已逐条核对源码）

**A. 命名常量（可直接借值）**

| 常量 | 值 | 声明处 | 记载的设计意图 |
|:--|:--|:--|:--|
| `kRevealExpand` | **460ms** | `motion.dart:115` | 展开比收起慢：一次滑出一整块，太快像被弹开 |
| `kRevealCollapse` | **420ms** | `motion.dart:118` | 收起稍快更利落，但同样留回弹 |
| `kRevealExpandCurve` | `Cubic(0.35, 0.30, 0.45, 1.25)` | `motion.dart:122` | 回弹留在**末尾**；`y3=1.25` 是过冲量 |
| `kRevealCollapseCurve` | `Cubic(0.70, -0.50, 0.40, 1.0)` | `motion.dart:126` | `x2=-0.50` 是 anticipation（先涨 ~9% 再收） |
| `StaggerIn._motion` | **560ms** | `motion.dart:206` | 总时间线；每张 `Interval(index×0.22)` ≈ **123ms** |
| `AnimatedTabIcon.duration` | **300ms** | `animated_tab_icon.dart:55` | 对齐玻璃库主力规格 |
| `AnimatedTabIcon.from` / `.overshoot` | **0.80** / **1.06** | `:57` / `:67` | 24px 上约 5px 收缩 / 过冲 1.4px（"再多会显得浮夸"） |
| `SubPageRoute.transitionDuration` | **320ms** | `glass.dart:169` | 对比 Cupertino 默认 500ms（"返回时总觉得慢半拍"） |
| `kGlassDialogFade` | **180ms** | `popup.dart:1360` | 点一下就开的东西不该等半拍 |
| `GlassDialogRoute.barrierLead` | **0.4** | `popup.dart:1387` | 遮蔽前 40%（≈70ms）铺满 |
| `ProgressRing._pop` | **520ms** `elasticOut` | `popup.dart:802` | 完成/失败徽章弹入 |
| `ProgressRing._wave` | **1000ms** | `popup.dart:809` | 1 秒 = 浪前进一个波长（对齐 Google `waveSpeed` 默认值） |
| `ProgressRing` tween | **260ms** `easeOut` | `popup.dart:852` | 平滑逐字节进度回调的跳变 |
| `ProgressRing` `AnimatedSwitcher` | **260ms** | `popup.dart:892` | 中心百分比 ↔ 徽章切换 |
| `_plainTabBar` `AnimatedAlign` | **220ms** `easeOut` | `main.dart:805` | 纯底栏指示器滑动 |
| `CoverSlot` 淡入 | **240ms** `easeOut` | `history.dart:361` | 已同步命中缓存时跳过 |
| `VideoStage` 海报淡出 | **320ms** `easeOut` | `preview.dart:1637` | "正好留给首帧解码，不然按下播放会先闪一下黑" |
| `ShortBounceScrollPhysics` | `frictionFactor × 0.5` | `motion.dart:107` | 基准 0.52 → **0.26**，同样位移下越界走得更近 |

**B. 三个复合动画的实现要点**

1. **`Reveal`（展开/收起）**：`TweenAnimationBuilder` → `ClipRect` → `Align(heightFactor: t < 0 ? 0 : t)`。四个易漏点：① 内容**从不卸载**（否则收起瞬间内容消失，"啪一下贴到底"）；② `t < 0` 必须钳零（收起曲线会走负值，负 `heightFactor` 非法）；③ 必须 `ClipRect` 遮过冲；④ 收起时同时 `ExcludeSemantics` + `IgnorePointer`。
2. **`StaggerIn`（错峰入场）**：单条时间线 + `Interval` 偏移实现错峰——**不用定时器，因此不存在"谁先谁后"的帧间抖动**。构造时 `value: show ? 1 : 0`，已可见则直接终态不补播。收起时只 `value = 0` 不播放（外层 `Reveal` 正在收高度，再淡出会重影）。
3. **`AnimatedTabIcon`（一次性弹跳）**：`TweenSequence` 权重 **55/45**：`0.80→1.06`（`easeOutCubic`）+ `1.06→1.0`（`easeOut`）。妙处在于放在"仅选中时构建"的槽位里，**构建即播放一次**，无需任何播放状态标志位。

**C. 可迁移的"意图"而非数值**

jicun 有三处曲线选择值得当作决策原则，而不是抄数字：

- **过冲要在末尾**：`easeOutBack` 那种"前段猛冲、末尾慢慢蹭"看着像"一下就完了"；jicun 用前段匀速铺开、末尾过冲再落回。
- **anticipation 只能做"先涨再缩"**：高度没法缩得比标题行还短，所以收起曲线前 1/3 下探。
- **遮蔽不与面板绑同一条曲线**：系统的 `RawDialogRoute` 把两者绑在一起，面板落地时背景还在变暗——"遮蔽慢半拍"。

---

### 2.2 玻璃组件落地 —— 有库但没被消费

**jicun 的做法：只有一个玻璃面板，但全站统一用它**

- 真玻璃**只有底栏**（`GlassScaffold` + `GlassTabBar.bottom`，来自 `liquid_glass_widgets`）
- 列表卡片是**假玻璃**：`GlassPanel` 用超椭圆转角 + 半透明底（浅色 `0x8CFFFFFF` / 深色 `0x26FFFFFF`），**不挂 `BackdropFilter`、不画投影**
- 这个"假"是**刻意且有记载的**（见 1.2 的两条注释）
- `GlassPanel` 被所有板块（解析/历史/设置）与弹层骨架 `PopupShell` 复用，**一处定义全局一致**

**LootArchive 的现状：组件很全，但页面绕过去了**

组件调用点实测：

| 组件 | 调用处数 | 落地的页面 |
|:--|--:|:--|
| `LiquidAlertDialog` | 19 | 9 个页面（覆盖好） |
| `LiquidGlassButton` | 20 | 8 个页面（覆盖好） |
| `EmptyState` | 10 | 6 个页面 |
| `LiquidIconButton` | 7 | 3 个页面 |
| `LiquidFilterChip` | 3 | `HomeScreen` |
| `LiquidToggle` | 3 | `SettingsScreen` |
| `LiquidSegmentedControl` | 3 | 2 个页面 |
| **`NeoCard`** | **4** | — |
| **`NeoStatCard`** | **3** | — |
| **`HeroStatCard`** | **1** | — |
| **`GlassSurface`** | **1** | — ⚠️ |

而各页面**直接用原生 `Card(`** 的文件：`MyLandingScreen` 5 处、`SettingsScreen` 3 处、`HomeScreen` 2 处、`AddItemScreen`/`BackupScreen`/`CategoryScreen`/`RecycleBinScreen` 各 1 处。

**两个真实问题：**

1. **卡片路径分叉**：`GlassSurface`/`NeoCard` 是设计好的玻璃卡（`GlassComponents.kt:34-77`，含 0 海拔 + 柔和阴影 + 深色微光描边），但页面基本不用，自己写 `Card(colors = CardDefaults.cardColors(containerColor = CardBg()), elevation = 1.dp)`（`MyLandingScreen.kt:83-89` 等）。于是**同一 App 里两种卡片配方并存**。
2. **`LiquidCard.kt` 从未存在**：设计文档 `2026-09-01-liquid-glass-design.md:68` 计划新建 `LiquidCard.kt`（用真 `drawBackdrop` 替换 `NeoCard` 内部），实测该文件不存在，`LiquidCard` 全项目出现 1 次（仅在别名 `GlassCard` 的参数里）。**说明当时试过把卡片做成真玻璃，后来退回了半透明**——这个结论与 jicun 的判断一致，可以互相印证，不必再试。

**建议：** 不新增组件，而是**把已有组件用起来**——把 9 个文件里的原生 `Card(` 收敛到 `GlassSurface`/`NeoCard`。这是低风险改动，纯替换，不动布局。

---

### 2.3 视觉 token 收敛

**jicun：单一 `Palette` 类，18 token × 2 档**

`lib/ui/palette.dart`（131 行）。关键设计：**按 App 自己手选的主题档取色，不读系统亮度**（`palette.dart:12-14` 有注释解释：用户手动选深色、系统是浅色时会取错）。

但要注意，jicun 的收敛**并不彻底**：

- `lib/` 里仍有 **135 处** `Color(0x...)` 字面量，排除 `palette.dart` 后 **99 处**
- **18 个 token 里有 9 个在 `palette.dart` 之外零引用**：`accentSoft` / `surfaceClear` / `playerGradient` / `tileUnselected` / `avatar` / `skeleton` / `onScrim` / `barBackground` / `pageBackground`
- 强调色对 `0xFF5AA9FF`/`0xFF1257C9` 在 7 处重复声明；危险色对在 3 处重复
- 旧的 `settingsPalette()` 转发函数（`palette.dart:128-131`）仍在被大量调用，**新代码不该再用**

**LootArchive：两套并行体系**

| 体系 | 位置 | 内容 |
|:--|:--|:--|
| `GlassColorScheme` | `Color.kt:62-98` | 9 个玻璃语义 token × 浅深两档 + `LocalGlassColors` |
| Material `ColorScheme` | `Theme.kt:22-97` | 完整 M3 色板，支持动态取色（`Theme.kt:133-135`） |
| 便捷函数 | `Color.kt:101-112` | `Primary()` / `TextPrimary()` / `GlassBg()` / `CardBg()` … |

现状问题（实测）：

- **硬编码 `Color(0x...)` 73 处**（排除 `Color.kt`）：`Theme.kt` 19、`OnboardingScreen` 9、`DetailScreen` 8、`LiquidDialog` 6、`CameraScreen` 5…
- **圆角 18 种**：`14dp`×25、`20dp`×17、`12dp`×14、`10dp`×10、`6dp`×7、`16dp`×6、`8dp`×5、`22dp`×3、`5dp`×3、`4dp`×3、`24dp`×3、`18dp`×3、`28dp`×2、`3dp`×2、`9dp`/`15dp`/`2dp`/`1dp` 各 1
- **`GlassTier` 枚举是死代码**：定义了 NAV/CARD/DIALOG/FAB/SHEET 五档（`GlassEffect.kt:13-23`），但只在 `GlassEffect.kt` 自身与一个别名 `GlassCard` 的参数里出现，**没有任何实际页面使用**
- **间距分散**：`padding` 数值 `16dp`×42、`8dp`×21、`12dp`×21、`14dp`×20、`10dp`×17、`4dp`×16、`6dp`×15、`20dp`×11、`40dp`×3、`24dp`×3…
- **字号 token 使用率 0%**：`MaterialTheme.typography` **0 处**，硬编码 `fontSize =` **260 处**。`MyLandingScreen` 44 处、`StatisticsScreen` 35 处、`AddItemScreen` 30 处最严重。`Type.kt` 精心定义的 Fredoka/Nunito 完整阶梯**基本没被消费**

**建议：** 三件事按序做——① 圆角收敛到一套刻度（建议 8/12/16/20/24/28，把 18 种砍到 6 种）；② 字号改走 `MaterialTheme.typography.*`（`Type.kt` 已就绪，改调用点即可）；③ 硬编码色替换为 token，优先 `Theme.kt` 内的 19 处。

jicun 在这一点上**不值得全盘照抄**（它自己也还有 99 处硬编码 + 9 个空转 token），但它的"**单一来源 + 按 App 主题档而非系统亮度取色**"这个决策是对的。

---

### 2.4 无障碍 —— 明确的缺口

**jicun 做了的：**

- 减弱动效：`motion.dart:245-247`（`StaggerIn` 直接给终态）、`animated_tab_icon.dart:111,126`（跳过缩放动画）
- 有一处**刻意的例外**：彩蛋动画不响应减弱动效（`tap_easter_egg.dart:247-248`），因为有注释说明理由
- 语义：底栏 `semanticLabel`、弹层 `barrierLabel: '关闭'`（`popup.dart:1378`）
- 有对应的测试：`test/animated_tab_icon_test.dart:11`

**LootArchive 的现状（实测，全为 0）：**

| 项 | 计数 |
|:--|--:|
| `reduceMotion` / `reduce_motion` | **0** |
| `disableAnimations` | **0** |
| `LocalAccessibilityManager` / `AccessibilityManager` | **0** |
| `testTag` | **0** |
| `minimumInteractiveComponentSize` | **0** |

已有的：`semantics { }` 23 处、`contentDescription` 10 处（`LiquidGlassBottomBar.kt:427` 有 `selected` 语义，做得不错）。

**这意味着：系统开启"减弱动效"时，LootArchive 的液态玻璃弹簧物理、拖拽镜头、`InteractiveHighlight` 全部照常播放。** 对晕动症用户是实际影响，且这是 Compose 里有标准做法的一件事。

**建议：** 在 `Motion.kt` 里同时提供一个"是否允许动效"的读取入口（Compose 可用 `LocalContext.current` 读 `Settings.Global.ANIMATOR_DURATION_SCALE`，或接入 `LocalAccessibilityManager`），让 `LiquidGlass*` 组件在减弱动效时退化到终态。这同时也是 `testTag` 补齐的时机（为 UI 测试铺路——现在 0 个 testTag，写 Compose UI 测试会很痛苦）。

---

### 2.5 页面结构与导航

**jicun 的做法：一个骨架覆盖三个板块**

`BoardScrollView`（`glass.dart:601-686`）解决的问题是：三个板块的标题、以及历史页那排「选择/全选/删除」原来都是列表第一项，一滑就跟着走。做法：

- **标题行钉在滚动区外面**（`Column`：`Padding(标题行)` + `SizedBox(18)` + `Expanded(列表)`），行高定死 `kBoardHeaderHeight = 32`（注释说明：不定死的话历史页三颗 32 高的按钮会把标题挤下去 25 设备px）
- **顶部渐隐遮罩**用 `ShaderMask` + `BlendMode.dstIn`，淡出带宽度跟着滚动量长（0 → `fadeHeight = 28`）
- **遮罩从头到尾都挂着**（不在 offset==0 时换 widget 类型）：注释说明换类型会让 `ListView` 整棵重建，滚动位置和展开态全丢

`SubPage`（`glass.dart:20-151`）是二级页统一外壳：背景铺满全屏 + 导航栏透明 + 可选顶栏图（不占位、靠 `headerLift` 抬高）。

**LootArchive 的现状：**

- **`ShaderMask` / `BlendMode.dstIn` 全项目 0 处**
- **`stickyHeader` 全项目 0 处**
- 14 个 Screen 各写各的滚动容器：`LazyColumn` / `LazyVerticalGrid` / `verticalScroll` / `HorizontalPager` 混用
- 唯一的重叠处理是 `MainScreen.kt:122-128` 的 `AnimatedContent` 转场（`fadeIn(tween(220)) + slideInHorizontally { it/10 }`）

导航方面：`MainScreen.kt` 用自定义 `Route` object + `currentRoute`/`backStack` 手写状态路由（`:54-97`），**已依赖 `navigation-compose:2.8.5` 但全项目 `NavHost`/`rememberNavController` 0 处**（`optimization-plan.md` 的 P2 2.2 项，标记为"未做"）。

**建议（按风险排序）：**

1. **低风险高收益**：给三主页面加统一顶部渐隐遮罩 + 钉住标题。`HomeScreen`/`StatisticsScreen`/`MyLandingScreen` 目前标题都随滚动走，加一层 `ShaderMask` 就有明显质感提升，且不动数据流。
2. **中风险**：抽一个 `BoardScaffold` 等价物，统一"钉住标题 + 内容滚动 + 底部给底栏让位"。
3. **暂不建议**：迁移到 Navigation Compose。`optimization-plan.md` 自己也评估为"高投入低收益"，且手写路由目前工作正常、已用 `rememberSaveable` 保住状态。

---

## 3. 值得直接借用的数值（Tier A）

这些是 jicun 里**已验证过、有意图记录**的规格，可原样落到 `Motion.kt`：

| 规格 | 值 | 用途 |
|:--|:--|:--|
| 展开/收起时长 | 460ms / 420ms | 卡片块伸缩 |
| 展开曲线 | `Cubic(0.35, 0.30, 0.45, 1.25)` | 末尾回弹（冲过目标再落回） |
| 收起曲线 | `Cubic(0.70, -0.50, 0.40, 1.0)` | 起始 anticipation（先涨 ~9% 再收） |
| 错峰入场 | 560ms，每张 +22%（≈120ms），上浮 16px | 列表批量出现 |
| 二级页转场 | 320ms（对比默认 500ms） | 返回不该等半拍 |
| 弹窗淡入 | 180ms，遮蔽前 40% 铺满 | 遮蔽别慢半拍 |
| 顶部渐隐带 | 28px | 滚动遮罩 |
| 选中图标弹跳 | 300ms：55% 从 0.80→1.06（easeOutCubic）+ 45% 1.06→1.0 | 底栏图标 |

**"卡片间距 12 时不要画投影"** 这条规则尤其值得采纳——jicun 记录了具体后果（投影越过间隙把两张卡连成一条黑带，卡片越多越明显），LootArchive 的 `GlassSurface`/`NeoCard` 用的是 `shadow(4.dp, ...)`，值得复核在 12dp 间距下是否也有这个问题。

### 3.1 两个方向一致的结论（可互相印证）

- **卡片不做真模糊**：jicun 明确记载（性能 + 深色观感），LootArchive 的 `LiquidCard.kt` 计划未落地也说明试过并退回。**两边结论一致，不必再试。**
- **捕获层不能进绘制期缩放**：jicun 记录 `UiZoom` 包住底栏会让 `BackdropFilter` 采样被裁到父级边界、玻璃面板整体画偏（`main.dart:586-594`）。LootArchive 的 `MainScreen.kt:78-81` 捕获层同样在最外层、页面内组件用局部捕获层——**纪律一致**。

---

### 3.2 核验补充：三个可直接对齐的实测差异

这三条是我在核验子代理报告时发现的，**比前面六项更具体、更容易落地**：

#### (1) 阴影策略完全相反

| | jicun | LootArchive |
|:--|:--|:--|
| `BoxShadow` | **0 处** | 0 处 |
| `elevation:` | **0 处** | `defaultElevation` **15 处** |
| `shadowColor` | **0 处** | — |
| `.shadow(` | **0 处** | **5 处** |

jicun **全项目零阴影**——深度完全由"半透明白底 + 页面渐变"承担。LootArchive 有 15 处 `defaultElevation`（多为 `1.dp`）+ 5 处 `.shadow(4.dp, ...)`。

**jicun 明确禁止卡片阴影的理由**（`glass.dart:306-309`）：卡片间距 12，而阴影是 blur 18 / 下移 8，**模糊半径超过间距**，会盖到下一张卡；深色模式下连成一条黑带，卡片越多越明显。

**这条值得直接查**：LootArchive 的 `GlassSurface`/`NeoCard` 用的是 `shadow(4.dp, ...)`（`GlassComponents.kt:41,69`），而卡片间距实测是 **12/14/16/8dp 不等**（见下）。间距 12 的地方（`AddItemScreen`、`BackupScreen`、`CategoryScreen`、`HomeScreen`）值得用深色模式肉眼确认是否已有"连成黑带"现象。

#### (2) 深色卡片底色：两者是同一种思路的两种做法

| | jicun | LootArchive |
|:--|:--|:--|
| 浅色玻璃底 | `0x8CFFFFFF`（**55% 白**） | `0x8CFFFFFF`（**55% 白**）✅ 完全相同 |
| 深色玻璃底 | `0x26FFFFFF`（**15% 白**） | `0xB21C1C24`（**70% 不透明深色**） |

**浅色档两边完全一致（`0x8CFFFFFF`），深色档分道扬镳。**

jicun 深色用的是**白色微叠加**（15% 白，让卡片比背景"亮"一点）；LootArchive 用的是**高不透明深色实底**（70% 的 `#1C1C24`）。

jicun 的做法在深色下会让卡片呈现"毛玻璃浮起"感（背景是 `#000000→#434343` 渐变，15% 白叠上去后卡片偏亮）；LootArchive 的 70% 实底更接近"实心卡片"，玻璃感更弱但对比度更稳。**这不是谁对谁错，而是要不要更通透的取舍**——若要更"玻璃"，可试把 `_GlassDark` 从 `0xB21C1C24` 降到 `0x26FFFFFF` 一档（但需重新校验文字对比度，因为背景变了）。

#### (3) 卡片间距：jicun 恒定 12，LootArchive 五种值

| jicun | LootArchive |
|:--|:--|
| 所有板块卡片间距**恒定 12**（`parse.dart:55`、`history.dart:142`、`settings.dart:57`，最后一项取 0） | `AddItemScreen`/`BackupScreen`/`CategoryScreen`/`HomeScreen` **12** · `SettingsScreen`/`StatisticsScreen` **14** · `MyLandingScreen`/`AboutScreen` **16** · `RecycleBinScreen` **8** · `SearchScreen` **6** |

jicun 的恒定值不是巧合——`kBoardHeaderHeight = 32`、`kBoardHeaderTop = 18`、间距 12、列表内边距 20 是一套**互相校验过的数字**（注释记录了 24→18 是因为标题在 32 高行里居中下移了 6，要保持墨迹位置不变）。

LootArchive 的 6/8/12/14/16 更像是各页面独立决定的。**收敛到 12（或 12/16 两档）是低成本、高一致性收益的改动。**

---

### 3.3 其他核验后的精确规格

以下数值均已在 jicun 源码中逐条核对通过：

**玻璃面板（`glass.dart:310-335`）**
- 形状：`LiquidRoundedSuperellipse(borderRadius: 20)`（超椭圆，非圆弧）
- 形状被**应用三次**（外层 `ShapeDecoration` + `ClipPath` + 内层 `ShapeDecoration`），保证填充与裁切共用同一几何，无半像素缝
- 填充：浅 `0x8CFFFFFF` / 深 `0x26FFFFFF`
- **不挂 `BackdropFilter`、不画阴影**
- 内部再声明一次 `Material(type: transparency)`，让涟漪画在半透明底**之上**（否则被压暗）

**板块骨架（`glass.dart:601-686`）**
- `kBoardHeaderHeight = 32`、`kBoardHeaderTop = 18`、头部→列表间隙 `18`、列表内边距 `(20, 0, 20, 120)`、卡片间距 `12`
- 顶部渐隐：`ShaderMask` + `BlendMode.dstIn`，带宽随滚动量 0→**28** 增长并封顶
- **遮罩始终挂载**（offset==0 时返回全不透明渐变，而不是换 widget 类型）——换类型会让 `ListView` 整棵重建，滚动位置与展开态全丢
- 性能细节：`ListView` 作为 `AnimatedBuilder` 的 `child` 传入，**滚动时只有遮罩层重建**

**二级页（`glass.dart:20-170`）**
- `SubPageRoute`：`barrierColor => null`（去掉 `0x18000000` 的 9% 黑罩）、转场 **320ms**（对比 Cupertino 默认 500ms）
- 顶栏手写 44 高（26px 返回箭头 + 17 w600 居中标题），**刻意不用 `CupertinoNavigationBar`**——后者底色非全不透明时会挂整宽 `BackdropFilter(blur 10)`，转场每帧重跑

**弹层（`popup.dart:1360-1391`）**
- `kGlassDialogFade = 180ms`，`Curves.easeOut`
- `barrierLead = 0.4`：遮蔽在前 40%（约 70ms）铺满，用 `Interval(0, 0.4, easeOut)`，**不与面板绑同一条曲线**（系统那条会让"遮蔽慢半拍"）
- `PopupShell` 的 `BackdropFilter` sigma = **12**；根节点还有一个 **1×1** 的同参数预热件（避免首次弹窗现编译着色器卡一下）

**两个背景（`motion.dart:5-81`）——玻璃采样的就是它**

| 档 | 实现 |
|:--|:--|
| 深色 | 2 段垂直 `LinearGradient`：`#000000` → `#434343` |
| 浅色 | `CustomPainter` **三次全屏 `drawRect`**：① 实底 `#CDDCDC`；② 垂直 `0x40FFFFFF→0x40000000` + `BlendMode.overlay`；③ 以下方中心为原点、半径 `sqrt(w²/4 + h²)` 的径向 `0x80FFFFFF→0x80000000` + `BlendMode.screen` |

`shouldRepaint => false`。**浅色那三层正是弹层作者称之为"白花帧"的成本来源**——它叠在 12 sigma 全屏模糊底下，弹层每帧都要重算。

对照 LootArchive：`backgroundBrush()` 是单层垂直渐变（`#FBF9F6→#F4E9DD` / `#0C0C10→#171118`）。**若要让玻璃有更多层次可采样，jicun 的多层画法（尤其叠加 `BlendMode.overlay`/`screen` 的径向高光）是具体可借的配方**，但要注意别整页加模糊。

### 3.4 反编译核验：Kyant Backdrop 的真实 API 降级矩阵

这一节是**反编译 `backdrop-1.0.0.aar` 得到的硬事实**（`javap -c`，jar 共 46 个类，逐个扫描 `SDK_INT` 判定）。它修正了一个流传中的误解。

LootArchive 的 `GlassEffect.kt` / README 都写"Android 13+ 全效果，12/12L 自动降级"。**这个说法不够准确**——降级是**逐效应**发生的，而且几乎都在库内部，不在 App 代码里。

**库内各效应的真实门限（字节码证据）：**

| 效应 | 所在类 | 字节码门限 | 在 minSdk 31 上 |
|:--|:--|:--|:--|
| `blur()` | `effects.BlurKt` | `getstatic SDK_INT` → `bipush 31` → `if_icmpge` | ✅ 可用 |
| `vibrancy()` | `effects.ColorFilterKt` | `bipush 31` | ✅ 可用 |
| `colorControls()` | `effects.ColorFilterKt` | 经 `colorFilter()` → `bipush 31` | ✅ 可用 |
| `RenderEffect` | `effects.RenderEffectKt` | `bipush 31` | ✅ 可用 |
| `InnerShadow` | `shadow.InnerShadowNode` | `bipush 31` | ✅ 可用 |
| `Highlight` | `highlight.HighlightNode` | `bipush 31` | ✅ 可用 |
| **`lens()`（折射）** | **`effects.LensKt`** | **`bipush 33` → 直接 `return`** | ❌ **静默丢弃** |

`LensKt` 的字节码开头就是：

```
6: getstatic  Field android/os/Build$VERSION.SDK_INT:I
9: bipush     33
11: if_icmpge   15
14: return            ← API < 33 时整个 lens() 空返回
```

**结论：`lens`（折射 / 透镜 / `chromaticAberration`）是唯一需要 API 33 的效应。** 其余（模糊、活力、高光、内阴影）从 API 31 起就工作——而那正好是 LootArchive 的 `minSdk`。

**这意味着：**

1. **Android 12/12L 上丢掉的正是"液态"身份本身**——折射、透镜、色散全部消失，只剩 vibrancy + blur + `onDrawSurface` 的半透明实底。**那个实底才是 12/12L 上"看得见玻璃"的唯一原因**（`LiquidGlassButton.kt:49-50` 的 `White.copy(0.45f)` 等）。
2. **但降级不是"白屏"也不是"崩坏"**——仍有模糊 + 活力 + 圆角 + 高光，观感是"磨砂玻璃"而非"液态玻璃"。这个降级是**可接受的**。
3. **`LiquidToggle` 是唯一显式分支的组件**（`LiquidToggle.kt:125` 的 `useFullGlass`，替换整个滑块）。其余组件**没有显式分支**，降级完全由库内部完成——所以 App 代码里那几处 `Build.VERSION.SDK_INT` 检查（`InteractiveHighlight.kt:61,83`、`LiquidToggle.kt:125`、`Theme.kt:133`、`NotificationUtil.kt:119`）里，**只有两处与玻璃相关**。

**可操作的建议**：若要在 Android 12 上改善观感，**不应去改 App 的降级分支**（它们没用），而应该**提高 12/12L 上 `onDrawSurface` 实底的不透明度**——那是唯一还能控制的变量。

---

### 3.5 核验后的死代码清单（可直接删除）

以下均为大小写敏感精确匹配、排除定义文件后的实测结果：

| 符号 | 定义处 | 定义外引用 | 说明 |
|:--|:--|--:|:--|
| `GlassBg()` | `Color.kt:109` | **0** | 访问器函数；但数据字段 `glass.glassBg` 有 2 处真实使用——**别删字段** |
| `GlassBorder()` | `Color.kt:110` | **0** | 同上；字段 `glass.glassBorder` 有 1 处使用 |
| `NavGlassBg()` | `Color.kt:111` | **0** | 字段亦无使用 |
| `OnPrimary()` | `Color.kt:106` | **0** | — |
| `SemanticInfo` | `Color.kt:118` | **0** | 整个 token 无人使用 |
| `glassEffect` Modifier | `GlassEffect.kt:31-42` | **0** | 含 `GlassTier` 的唯一"用法"，本身也无人调用 |
| `GradientStartLight/EndLight/StartDark/EndDark` | `Color.kt:150-153` | **0** | 4 个梯度 token 全无人用；`0xFFFFF8F0` 反而在 `HomeScreen.kt:106` 被内联硬编码 |

**注意 `GlassTier` 的陷阱**：它唯一的消费点是别名 `GlassCard`（`GlassComponents.kt:157`），而后者**接受 `tier` 参数后直接忽略**，转手调用 `NeoCard(modifier, onClick, content = content)`。所以 `GlassTier` 的 5 档规格（NAV/CARD/DIALOG/FAB/SHEET）**全部是文档而非代码**。

**安全删法**：`glassEffect` + 4 个 Gradient token + `SemanticInfo` 可直删；`GlassBg()`/`GlassBorder()`/`NavGlassBg()`/`OnPrimary()` 是访问器壳，删函数前先确认没有反射/测试引用（实测为 0）。`GlassTier` 要么删，要么**真正接上**——后者才是修复本意。

---

### 3.6 实测规模（用于排期估算）

| 指标 | 实测值 |
|:--|:--|
| Kotlin 源文件 | **89** 个（`main`）/ 5 个（`test`） |
| 总行数 | **10,959**（main） / 184（test） |
| `ui/` 占比 | 46 文件 / 8,310 行 ≈ **76%** |
| 硬编码 `Color(0x` | **73** 处（排除 `Color.kt`） |
| `RoundedCornerShape` | **107** 处，**18** 种半径（14dp×25 最多） |
| 硬编码 `fontSize` | **253** 处，**20** 种字号（含 9sp 与 **90sp**） |
| `MaterialTheme.typography` | **0** 处 —— `Type.kt` 的 14 档阶梯完全未接入 |
| 间距 dp 值 | **34** 种，含 `90/104/120/140/186/210/216/224dp` 等魔法值 |
| `tween(` / `spring(` / `Animatable(` | 14 / 9 / 8 处，无集中常量文件 |
| 玻璃捕获层 `rememberLayerBackdrop` | **27** 处 / 13 文件 |
| `drawBackdrop` 调用 | **19** 处 / 6 文件 |
| 无障碍（`reduceMotion`/`disableAnimations`） | **0** 处 |
| `testTag` | **0** 处 |
| 单测 | 5 文件 / 184 行 |

其中几个魔法间距值得点名（都是为绕开具体控件而硬留的空间，是无障碍与响应式布局的隐患）：

- `SettingsScreen.kt:105` — `padding(end = 186.dp)`
- `RecycleBinScreen.kt:68` — `Spacer(Modifier.width(90.dp))`
- `AddItemScreen.kt:246,279,480,567` — 不可见 `Spacer(width = 224/104/210/216.dp).height(48.dp)`，**专门为玻璃捕获层预留**

---


## 4. 明确不要抄的部分

| 项 | 原因 |
|:--|:--|
| `Palette` 的 99 处硬编码色 / 9 个空转 token | jicun 自己也没收拾干净，别把债务搬过来 |
| `settingsPalette()` 转发垫片 | 已标注"新代码别用"，LootArchive 无对应历史包袱 |
| `ShellController`（24 成员的大接口） | 为解 Flutter 的 import 环而设；LootArchive 的 ViewModel + StateFlow 分层更干净 |
| `SubPageRoute` 手改 320ms/去 barrier | Compose 侧有 `AnimatedContent` 与 Navigation 的正规做法 |
| `UiZoom` 虚拟画布缩放 | 是为绕开 BackdropFilter 缩放缺陷的 hack；Compose 无此问题 |
| `NoKeyboardInset` | Compose 用 `imePadding()` / `WindowInsets` 正规处理 |
| 1400 行自定义进度环 painter | 领域特化（下载进度+波浪），与 LootArchive 无关 |
| 扁平中文目录名的资源组织 | LootArchive 走 `res/` + `ImageVector`，体系不同 |
| Flutter 的排版默认值（字号 10~28 散写） | LootArchive 的 `Type.kt` 阶梯更规范，应反向收敛 |

---

## 5. 建议的改造顺序

分三批，每批独立可发布、可回滚：

**第一批（低风险，纯增量）**
1. 新建 `ui/theme/Motion.kt`，收口交互动效时长/曲线（借 Tier A 数值）
2. 加"减弱动效"读取入口，让 `LiquidGlass*` 组件与 `AnimatedContent` 支持退化
3. 给三主页面加统一顶部渐隐遮罩（`ShaderMask` + `dstIn`，28px 带）

**第二批（中风险，替换为主）**
4. 圆角收敛：18 种 → 6 种刻度
5. 9 个文件里的原生 `Card(` → `GlassSurface`/`NeoCard`
6. 字号改走 `MaterialTheme.typography.*`（优先 `MyLandingScreen`/`StatisticsScreen`/`AddItemScreen`）

**第三批（需回归验证）**
7. 硬编码色替换（优先 `Theme.kt` 19 处、`OnboardingScreen` 9 处）
8. 抽 `BoardScaffold` 统一页面骨架
9. 补 `testTag` + Compose UI 测试

每批完成后跑 `./gradlew :app:assembleDebug` + `testDebugUnitTest` 回归。

---

## 6. 待决策的点

1. **动效风格取向**：jicun 的曲线偏"活泼"（回弹、anticipation、上浮）。LootArchive 现在的调性更"稳"（`spring(1f, 300f, 0.5f)` 这类偏阻尼）。是想要 jicun 那种灵动感，还是保持克制、只做 token 化不作风格迁移？
2. **字号 token 化是否接受视觉回归**：`Type.kt` 的阶梯与当前 260 处硬编码 `fontSize` 必然有出入，改完所有页面的字号会有肉眼可见的变化。是否接受一次性对齐，还是只在新代码遵守？
3. **圆角刻度取值**：建议 8/12/16/20/24/28，但需确认 `14dp`（用了 25 次，最多）的归属——是收到 12 还是 16？
4. **是否引入 Compose 版的 motion 编译期检查**：可选加一个 lint 规则禁止裸写 `tween(` 时长，防止再次散掉。

---

## 附：数据出处

**jicun**：`lib/ui/palette.dart`、`lib/ui/motion.dart`、`lib/ui/glass.dart`、`lib/ui/widgets.dart`、`lib/ui/popup.dart`、`lib/ui/icons.dart`、`lib/main.dart`、`lib/pages/*.dart`、`lib/widgets/animated_tab_icon.dart`、`lib/shell_controller.dart`、`lib/bootstrap.dart`、`pubspec.yaml`、`README.md`

**LootArchive**：`app/src/main/java/com/nanji/lootarchive/ui/theme/{Color,Theme,Type,GlassEffect}.kt`、`ui/liquidglass/*`（10 文件）、`ui/component/GlassComponents.kt`、`ui/MainScreen.kt`、`ui/MyLandingScreen.kt`、`ui/home/HomeScreen.kt`、`ui/about/AboutScreen.kt`、`docs/*`

统计口径：硬编码色 = `Color(0x` 字面量计数；圆角 = `RoundedCornerShape(N.dp)` 匹配；字号 = `fontSize =` 出现次数；动效 = `tween(`/`spring(`/`Animatable(` 匹配数。
