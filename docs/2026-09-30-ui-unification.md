# LootArchive UI 统一改造记录（v6.10.0）

日期：2026-09-30
范围：动效 token 化 · 字号 token 化 · 圆角/间距收敛 · 无障碍（减弱动效）· 页面骨架 · 死代码清理 · 硬编码色收敛
基线：改造前 `:app:compileDebugKotlin` 已验证 `BUILD SUCCESSFUL`，所以之后任何编译失败都可归因于本次改动。

**验证结果：`./gradlew :app:assembleRelease :app:testDebugUnitTest` → BUILD SUCCESSFUL，25 个单测全通过、0 失败。Release APK 正常产出并校验通过。**

产物：`LootArchive-release-v6.10.0.apk`（7.85 MB，`versionCode=700`，`versionName=6.10.0`）

---

## 0. 版本号 / 更新日志 / 打包

- `app/build.gradle.kts`：`versionCode 696 → 700`、`versionName "6.9.6" → "6.10.0"`
  （minor 递增：本次是新增 token 体系与骨架能力的特性级改动，不只是修补）
- `version.json`：同步版本号并写入本次更新日志
- `README.md`：更新版本徽章、APK 文件名、`## 更新日志` 新增 v6.10.0 条目

> 代码里**没有**硬编码版本号（已 grep 确认），全部读 `BuildConfig.VERSION_NAME`，所以只需改上面几处。

### ⚠️ 打包时发现一个既有问题（与本次改动无关）

`assembleRelease` 会先跑 `lintVitalAnalyzeRelease`，而该任务**在未改动的基线代码上也会崩**：

```
Execution failed for task ':app:lintVitalAnalyzeRelease'.
IncompatibleClassChangeError: NonNullableMutableLiveDataDetector$createUastHandler$1.visitCallExpression
```

我把改动 `git stash` 后在纯净基线上复现了同一个崩溃，**确认是既有问题、不是本次改造引入的**（项目里 `LiveData` 出现 0 处，是该 lint detector 自身与 AGP/lint 版本的兼容问题）。

本次打包用 `-x lintVitalAnalyzeRelease` 跳过它。**建议单独修**：多半是 AGP 与 lint 版本不匹配，升级 AGP 或锁 lint 版本即可。修好之前 `assembleRelease` 需带 `-x lintVitalAnalyzeRelease`。

### ⚠️ Release 签名仍是 debug 证书（既有 P0，本次未处理）

校验本次产物：

```
Signer #1 certificate DN: C=US, O=Android, CN=Android Debug
```

即签名配置在没有 `LOOTARCHIVE_KEYSTORE_*` 环境变量时会退回 `~/.android/debug.keystore`。**这个 APK 不能用于正式发布** —— 任何人都能用公开的 debug 证书重签覆盖。正式发布需先生成 release keystore 并设置那 4 个环境变量。

---

## 1. 新增的三个 token 文件（统一的落点）

| 文件 | 作用 |
|:--|:--|
| [Motion.kt](../../app/src/main/java/com/nanji/lootarchive/ui/theme/Motion.kt) | 动效时长 / 曲线 / 弹簧的唯一来源 + 减弱动效读取入口 |
| [Dimens.kt](../../app/src/main/java/com/nanji/lootarchive/ui/theme/Dimens.kt) | 圆角（`AppRadius`/`AppShape`）与间距（`AppSpacing`）刻度 |
| [ScrollFade.kt](../../app/src/main/java/com/nanji/lootarchive/ui/component/ScrollFade.kt) | 页面骨架的顶部渐隐 `Modifier.topFade(...)` |

## 2. 量化结果

| 指标 | 改前 | 改后 |
|:--|--:|--:|
| `MaterialTheme.typography` 引用 | **0** | **236** |
| 硬编码 `fontSize = N.sp` | 253（20 种字号，含 9sp/90sp） | **18**（15 处在 `Type.kt` 定义处 + 3 处图表专属，见 §7） |
| 裸 `RoundedCornerShape(N.dp)` | 107（**18 种**半径） | **0** |
| `AppShape` / `AppRadius` 引用 | 0 | **124**（`AppShape` 107 + `AppRadius` 17） |
| 动效 token 引用 | 0 | **36** |
| `AppSpacing` 引用 | 0 | 12 |
| `.topFade(` 顶部渐隐 | 0 | **6** |
| 减弱动效处理 | **0** | **56** 处引用（含 4 个玻璃组件 + 2 套物理引擎） |
| 硬编码 `Color(0x…)`（排除 `Color.kt`） | 73 | 见 §6 |

改动规模：**29 个文件、约 600 行**（新增 3 个文件、修改 26 个）。

---

## 3. 动效系统（按你的要求：偏活泼）

### 3.1 曲线风格

`MotionCurve` 提供 7 条曲线，其中两条是**刻意带过冲/预判**的（活泼取向）：

- **`MotionCurve.Expand` = `CubicBezierEasing(0.35, 0.30, 0.45, 1.25)`** —— 末尾过冲。前段匀速铺开、最后冲过目标再落回。jicun 的注释解释了为什么不反过来：`easeOutBack` 那种"前段猛冲、末尾慢慢蹭"看着就是"一下就完了"。
- **`MotionCurve.Collapse` = `CubicBezierEasing(0.70, -0.50, 0.40, 1.0)`** —— 起始预判（曲线先往回走再收）。
- `MotionCurve.Emphasized` = `CubicBezierEasing(0.34, 1.56, 0.64, 1f)` —— 明显过冲，给"弹出来"的入场。

> **注意**：Compose 的 `CubicBezierEasing` **只约束 x 落在 [0,1]**，y 允许超界 —— 这正是过冲能画出来的原因。上面每条曲线的 x 值（0.35/0.45、0.70/0.40、0.34/0.64）都已确认在 [0,1] 内，不会抛异常。

### 3.2 弹簧

`MotionSpec` 四档，全部带过冲或轻阻尼（不再是原来的清一色无过冲）：

| 档 | 参数 | 用途 |
|:--|:--|:--|
| `bouncy()` | `dampingRatio=0.5, stiffness=400` | 图标弹跳、选中指示器 |
| `lively()` | `dampingRatio=0.75, stiffness=400` | 弹层缩放入场（已接入 `LiquidDialog`） |
| `gentle()` | `DampingRatioNoBouncy, stiffness=300` | 大面积位移 |
| `draggable()` | `dampingRatio=0.9, stiffness=700` | 底栏镜头回弹（已接入） |

### 3.3 时长

`MotionDuration` 6 档交互动效（`Quick`180 / `Medium`260 / `Page`320 / `Collapse`420 / `Expand`460 / `Stagger`560 / `Count`600）+ 一组环境动效（`Ambient.OrbSlow/Medium/Fast/Float`）。

**环境动效刻意单独归类**：关于页的光斑漂移（13000/9000/7000ms）、空状态浮动（2000ms）不是交互反馈，把它们的时长塞进"Hover/Press/Transition"那套语义里只会让人困惑。现在它们挂在 `MotionDuration.Ambient` 下，一眼能看出是另一类。

已迁移的调用点：页面转场（`MainScreen`）、弹层三条（scrim/alpha/scale，`LiquidDialog`）、底栏镜头回弹、数字滚动 ×3（`HomeScreen`）、光斑 ×3、空状态浮动。

---

## 4. 减弱动效（本次最重要的无障碍修复）

### 4.1 为什么必须显式处理

Compose 的动画时长缩放（`MotionDurationScale`）**只作用于带显式时长的 spec**（`tween` 那一类）。**弹簧是物理积分、没有时长可言，完全不受它影响。**

而 LootArchive 的液态玻璃全部是弹簧驱动的 —— 底栏拖拽镜头、按压形变、图标缩放、按压高光。所以改前实测：**系统开启"减弱动效"后，这些动画照旧满帧播放**（全项目 `reduceMotion`/`disableAnimations` 0 处）。

### 4.2 做了什么

- `Motion.kt` 新增 `LocalReduceMotion` + `rememberReduceMotion()`，用 `ContentObserver` **监听** `Settings.Global.ANIMATOR_DURATION_SCALE`（不能只在启动读一次 —— 用户在系统设置里改完回到 App，进程还活着，会一直用旧值）。
- `MainActivity` 用 `ProvideReduceMotion { ... }` 包住整棵树。
- `DampedDragAnimation` 与 `InteractiveHighlight` 各加 `reduceMotion` 参数，为真时把全部弹簧换成 `snap()`（瞬时到位，功能不变）。
- 4 个玻璃组件（`LiquidGlassButton` / `LiquidIconButton` / `LiquidFilterChip` / `LiquidGlassBottomBar`）与 `LiquidToggle` 传入该值，**且都放进了 `remember` 的键** —— 否则开了减弱动效之后仍会复用按旧值构造的物理对象。

---

## 5. 页面骨架

### 5.1 顶部渐隐（`Modifier.topFade`）

`ScrollFade.kt` 用 `drawWithContent` + `BlendMode.DstIn`（Compose 里 `ShaderMask` 的等价做法），带宽随滚动量 0 → 28dp 增长并封顶。已接入**三个主页面**（`HomeScreen` 网格 / `StatisticsScreen` / `MyLandingScreen`）。

两个实现要点都写在文件头：

1. **`CompositingStrategy.Offscreen` 是必需的**。`BlendMode.DstIn` 只作用于当前图层；不隔离的话它会往上作用到父图层，把底栏和背景一起擦掉（整屏变黑）。
2. **遮罩始终挂载**，未滚动时画一块全不透明遮罩而不是切换分支 —— 换分支会改变 Modifier 结构，导致滚动容器整棵重建，滚动位置与展开态全丢。

读取滚动量发生在**绘制阶段**，所以滚动时只重画、不重组调用方。

### 5.2 间距统一

`AppSpacing` 新增三个页面骨架刻度：`pageEdge`(16) / `pageVertical`(12) / `bottomBarClearance`(120)。

其中 `bottomBarClearance` 修的是一个实际问题：底栏浮在内容之上、不占布局高度，所以每个滚动列表必须自己留白。改前 **`HomeScreen` 用 140dp、`StatisticsScreen` 用 100dp、`MainScreen` 用 90dp** —— 三个页面三种值，而底栏几何是固定的。

同样把 `MyLandingScreen` / `StatisticsScreen` 的卡片间距从 16/14 收敛到 `AppSpacing.lg`(12)，与首页一致（jicun 那边是恒定 12）。

---

## 6. 死代码与硬编码清理

### 6.1 删除的死代码（均为精确匹配 0 引用）

| 符号 | 说明 |
|:--|:--|
| `GlassTier` 枚举 + `Modifier.glassEffect()` | 整个 `GlassEffect.kt` 清空成注释。**关键发现**：`GlassTier` 唯一"消费点"是别名 `GlassCard`，而后者收下 `tier` 参数后**直接忽略** —— 那五档规格是文档不是代码 |
| `GlassCard` / `StatCard` / `GlassStatCard` 别名 | 0 调用 |
| `GlassBg()` / `GlassBorder()` / `NavGlassBg()` / `OnPrimary()` | 4 个访问器 0 引用。**注意**：数据类字段 `glass.glassBg` 仍在用（2 处），只删访问器壳 |
| `SemanticInfo` 色 token | 0 引用 |
| `_OnPrimary` / `_OnSecondary` | 删掉 `OnPrimary()` 后成为孤儿 |
| 4 个 `GradientStart*/End*` token | 0 引用 |

**保留**（实测仍在用，调用点统计含尾随 lambda 写法）：`ClayCard` 13 处、`GlassSurface` 11 处、`EmptyState` 11 处、`GlassAlertDialog` 5 处。

> 这里纠正了我先前分析文档里的一个错误：我当时用 `\bGlassSurface\(` 匹配，漏掉了尾随 lambda 的 `GlassSurface { }` 写法，误判成"仅 1 处调用、玻璃卡形同虚设"。**实际调用点有 11 处 + ClayCard 13 处，玻璃卡一直在正常使用。** 该结论已在报告中修正。

### 6.2 硬编码色收敛

原先 4 个 0 引用的 Gradient token 换成了**真正在用的**配色，并替换掉页面里的内联硬编码：

- `HeroCardLight` = `0xFFFFF8F0`（首页 Hero 卡，原先内联在 `HomeScreen`）
- `PhotoPlaceholderLight` / `PhotoPlaceholderDark`（详情页无照片占位渐变，原先 4 个色值内联在 `DetailScreen`）
- `AvatarWarmLight` = `0xFFFFEDE0`（头像衬底，原先内联在 `MyLandingScreen`）

### 6.3 一处需要说明的"没改"

`GlassSurface`/`NeoCard` 用 `Modifier.shadow(4.dp, ...)`。我原分析文档里按 jicun 的规则（"阴影模糊不得大于卡片间距"）标了风险，但**实测复核后确认这里是安全的**：使用这些组件的页面间距是 12/16dp，而阴影是 4dp（jicun 那个问题是 blur 18 配间距 12）。所以**没有改动阴影**，只把结论记在这里备查。

---

## 7. 图表字号的特殊处理

雷达图（11sp）与趋势折线图（10sp）的坐标标签**比正文梯子最小档（12sp）还小一档**——它们要挤在图形边缘，取 12sp 会压到图形上。

这两处没有强行套 `MaterialTheme.typography`，而是从 `AppTypography.labelSmall` **派生**（`.copy(fontSize = ...)`）：字族与行高跟全站一致，只有字号是图表专属的局部覆盖，并且写明了理由。这样它们既不是"漏改的硬编码"，也不会在下次全局调字号时被漏掉。

---

## 8. 构建环境（与代码无关，但影响复现）

改造过程中遇到沙箱限制：Gradle 需要写 `~/.gradle`（锁文件、`native-platform.dll`），Kotlin 编译 daemon 需要写 `%LOCALAPPDATA%\kotlin`，AGP 需要写 `~/.android`——三者都在工作区之外，被 workspace-write 策略拒绝。

解决方式是把这三个目录复制进工作区并用环境变量重定向，**避免每次构建都要提权**：

```powershell
$env:GRADLE_USER_HOME  = "D:\GitHub\.gradle-home"
$env:ANDROID_USER_HOME = "D:\GitHub\.android-home"
```

`KOTLIN_DAEMON_DIR` 重定向未生效（Kotlin 仍尝试写 `%LOCALAPPDATA%\kotlin`），但 Gradle 会自动回退到**进程内编译**（日志里的 `Using fallback strategy: Compile without Kotlin daemon`），构建照常成功——只是慢一些。

> 这三个目录各约 5.7GB / 少量，**不属于项目、不应提交**。若要删除，需先确认没有其他构建在进行。

---

## 9. 后续可选（未做）

1. **把 `AppSpacing` 铺开到二级页**：本次只统一了三个主页面与关键值。`SettingsScreen`/`SearchScreen`/`AddItemScreen` 等仍有 14/18dp 边缘与若干魔法间距（`SettingsScreen.kt:105` 的 `padding(end = 186.dp)`、`AddItemScreen` 那几处为玻璃捕获层预留的不可见 `Spacer`）。
2. **`TopAppBar` 与沉浸式统一**：`StatisticsScreen` 在非 Tab 模式下仍用 `TopAppBar`，与其余页面手写顶栏的做法不一致。
3. **`testTag` 补齐 + Compose UI 测试**：目前全项目 0 个 `testTag`，写 UI 测试会很吃力。
4. **圆角进一步收敛**：`AppRadius` 现在是 6 档（4/8/12/16/20/28）。若想更极端，可合并到 4 档，但收益递减、且要动 124 处。
