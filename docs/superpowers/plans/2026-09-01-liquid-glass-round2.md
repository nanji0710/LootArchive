# LootArchive 第二轮：全站按钮玻璃化 + 页面重构 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development. Steps use checkbox (`- [ ]`) syntax.

**Goal:** 我的页重构（RiseDiary 设置页布局）、新关于页、检查更新迁入美化、设置页个性化重构、以及全站剩余按钮/芯片/分段控件玻璃化，修复统计页浅色灰圈。

**Architecture:** 沿用现有液态玻璃体系（Kyant Backdrop 捕获层 + LiquidGlass* 组件）。页面内按钮一律局部捕获层（画渐变底）；主操作按钮加主色 tint；新组件 LiquidIconButton / LiquidFilterChip；GlassSurface 灰圈修复。

**Tech Stack:** 既有（backdrop 1.0.0 + capsule 2.1.1 + Compose M3）；无新依赖。

## Global Constraints

- 质量规范（设计文档 F 节，每条 brief 必须携带）：①易读性——玻璃上文字对比度达标，主色 tint 按钮用白字，禁止文字背后高模糊；②统一规格——胶囊按钮 48dp、图标钮 40dp、chips 32dp、参数与已落地组件一致；③捕获层纪律——页面内按钮用局部捕获层画渐变，禁止采样页面层（自采样）；④性能——不叠多层模糊，静态卡片不模糊；⑤一致性——琥珀主色 + 0xFF121212 深色系 + 玻璃白亮色系
- 公开组件 API 向后兼容：`LiquidGlassButton` 新参数默认值不破坏现有调用
- 数据层/逻辑层零改动（更新检查/下载逻辑迁移时行为不变）
- 弹窗内 M3 TextButton 保持不动
- 每个任务以 `.\gradlew.bat :app:compileDebugKotlin` 通过为完成标准；末尾任务全量 `assembleDebug` + `testDebugUnitTest`
- git commit 已授权；提交信息风格参考 `git log --oneline -5`

---

### Task 1: 组件增强（LiquidGlassButton tint + LiquidIconButton + LiquidFilterChip）

**Files:**
- Modify: `ui/liquidglass/LiquidGlassButton.kt`（加 tint/surfaceColor）
- Create: `ui/liquidglass/LiquidIconButton.kt`
- Create: `ui/liquidglass/LiquidFilterChip.kt`

**Interfaces:**
- Produces:
  - `LiquidGlassButton(..., tint: Color = Color.Unspecified, surfaceColor: Color = Color.Unspecified, ...)`——tint 用 `BlendMode.Hue` 叠加 + alpha 0.75 实染（onDrawSurface 内），surfaceColor 直接覆盖；二者 Unspecified 时行为与现状完全一致
  - `@Composable fun LiquidIconButton(onClick, backdrop, modifier, size: Dp = 40.dp, tint: Color = Color.Unspecified, enabled = true, content: @Composable () -> Unit)`——圆形玻璃图标钮（ContinuousCapsule + 40dp = 圆），vibrancy+blur(2dp)+lens(12,24)+按压高光（复用 InteractiveHighlight）
  - `@Composable fun LiquidFilterChip(selected: Boolean, onClick, backdrop, modifier, label: String, labelColor: Color? = null)`——32dp 高玻璃胶囊，选中=主色淡底(alpha 0.15)+主色字加粗，未选中=玻璃透明底+TextSecondary 字；vibrancy+blur(2dp)+lens(12,24)

- [ ] Step 1: LiquidGlassButton 加参数（onDrawSurface 内先 tint 后 surfaceColor，参考已有实现结构）
- [ ] Step 2: 新建 LiquidIconButton.kt（结构参考 LiquidGlassButton，圆形）
- [ ] Step 3: 新建 LiquidFilterChip.kt
- [ ] Step 4: 编译验证 + 提交（3 文件）

### Task 2: 我的页重构

**Files:** Modify: `ui/MyLandingScreen.kt`

- [ ] Step 1: 顶部加大标题「我的」（Fredoka 30sp SemiBold，参考 RiseDiary settings_title）
- [ ] Step 2: 功能入口卡加「关于」行（Info 图标 + 副标题「版本信息、检查更新」+ 箭头），onClick 调新增的 `onNavigateToAbout`
- [ ] Step 3: 删除「关于」Card（拾物集 ItemGlow + 版本）与「检查更新」液态按钮块及其局部捕获层代码
- [ ] Step 4: 删除 MyLandingScreen 全部更新相关状态与弹窗（updateInfo/showUpdateDialog/showNoUpdate/checkError/isChecking/isDownloading/downloadProgress/downloadError/scope/downloader 相关）——迁移到 Task 3 的关于页
- [ ] Step 5: 函数签名加 `onNavigateToAbout: () -> Unit`
- [ ] Step 6: 编译（需同步 Task 3 或在 Task 3 前临时保留？——**控制器裁决：Task 2 与 Task 3 合并派发**，避免中间态编译失败）
- [ ] 提交

> 控制器裁决：Task 2+3 合并为一个任务「我的页重构 + 关于页新建」（迁移原子性）。

### Task 3: 关于页新建 + ABOUT 路由 + 更新逻辑迁入

**Files:**
- Create: `ui/about/AboutScreen.kt`
- Modify: `ui/MainScreen.kt`（Route.ABOUT + when 分支）
- Modify: `ui/MyLandingScreen.kt`（配合 Task 2 的移除）

- [ ] Step 1: 新建 AboutScreen.kt：
  - 局部捕获层（backgroundBrush + drawContent）
  - 顶部行：LiquidIconButton 返回 + 标题「关于」（20sp Bold Fredoka）
  - Logo 区：painterResource(R.mipmap.ic_launcher) 100dp + 「拾物集 ItemGlow」35sp Bold + 「v${BuildConfig.VERSION_NAME}」14sp
  - 链接卡（GlassSurface）：GitHub 仓库行（图标+标题+副标题+箭头，UriHandler 打开 github.com/nanji0710/LootArchive）
  - 信息卡（GlassSurface）：使用说明（纯本地记录/备份导出） + 数据隐私（数据仅存本机，不联网上传）
  - 底部：全宽 LiquidGlassButton（52dp、Refresh 图标、tint = Primary().copy(alpha = 0.075f)、检查中显示「正在检查...」+ 禁用）→ 触发更新检查
  - 更新弹窗全家桶：从 MyLandingScreen 原样迁移（发现新版本含下载安装、已是最新、检查失败、检查中、下载中）——状态/逻辑/`ApkDownloadManager`/`UpdateChecker` 引用零改动
- [ ] Step 2: MainScreen 加 `const val ABOUT="about"` + when 分支 `Route.ABOUT -> AboutScreen(onNavigateBack={goBack()})`
- [ ] Step 3: MyLandingScreen 移除更新相关全部代码 + 加 onNavigateToAbout 参数 + 「关于」行（与 Task 2 合并执行）
- [ ] Step 4: 编译验证 + 提交

### Task 4: 设置页个性化模块重构

**Files:** Modify: `ui/settings/SettingsScreen.kt`

- [ ] Step 1: 「个性化」卡 4 行补图标块（38dp 圆角 12 主色 10% 底 + 20dp 图标，复用 MyMenuItem 图标块样式）：Palette/AutoAwesome/AccountCircle/School
- [ ] Step 2: 各行加副标题：显示模式（"跟随系统/浅色/深色"）、动态色（"Android 12+ 跟随壁纸"）、自定义头像（"从相册选择"）、重新查看引导（"下次启动重新展示"）
- [ ] Step 3: 保持液态分段控件/LiquidToggle/头像选择/Toast 逻辑不动；行布局调整为 图标块+文字列（weight 1f）+控件
- [ ] Step 4: 编译验证 + 提交

### Task 5: 新增/编辑向导按钮玻璃化

**Files:** Modify: `ui/additem/AddItemScreen.kt`

- [ ] Step 1: 三步底部按钮行：每行局部捕获层（backgroundBrush）+「← 上一步」= LiquidGlassButton 中性（无 tint）+「下一步 →」/「完成保存 ✓」= LiquidGlassButton tint=Primary() 白字
- [ ] Step 2: 顶部「保存」TextButton → 小号玻璃按钮（tint=Primary()，白字）
- [ ] Step 3: 编译验证 + 提交

### Task 6: 首页分类 Chips 玻璃化

**Files:** Modify: `ui/home/HomeScreen.kt`（131-140 行区域）

- [ ] Step 1: 分类行加行级捕获层（backgroundBrush）+ FilterChip 三处 → LiquidFilterChip（全部/分类名），选中逻辑/布局（LazyRow 或 Row+horizontalScroll）不变
- [ ] Step 2: 编译验证 + 提交

### Task 7: 统计页（筛选分段 + 导出按钮 + 灰圈修复）

**Files:** Modify: `ui/statistics/StatisticsScreen.kt`、`ui/component/GlassComponents.kt`

- [ ] Step 1: GlassSurface 灰圈修复：`elevation = 1.dp` → `0.dp` + NeoCard 同款柔和 shadow（ambientColor 白 0.3 / spotColor glass.shadow）
- [ ] Step 2: 资产总览卡（66 行）→ GlassSurface（glassBg，与其余模块统一）；时间筛选内嵌 Surface 分段（81-88 行）→ 行内捕获层 + LiquidSegmentedControl（4 选项无图标，containerHeight 40dp，contentPadding 3dp，showSelectionShadow false）
- [ ] Step 3: 导出 CSV Button（290 行）→ 行内捕获层 + LiquidGlassButton（tint=Primary() 白字）
- [ ] Step 4: 编译验证 + 提交（2 文件）

### Task 8: 详情页编辑/删除圆钮玻璃化

**Files:** Modify: `ui/detail/DetailScreen.kt`（150-154 行）

- [ ] Step 1: 顶部操作行加局部捕获层（backgroundBrush）；三个 40dp Surface 圆钮（返回/编辑/删除）→ LiquidIconButton（返回白玻璃+白图标、编辑白玻璃+主色图标、删除白玻璃+WarrantyExpired 图标）
- [ ] Step 2: 编译验证 + 提交

### Task 9: 回收站按钮玻璃化

**Files:** Modify: `ui/recyclebin/RecycleBinScreen.kt`

- [ ] Step 1: 顶部「清空」TextButton → 小号玻璃胶囊（红色系：tint = WarrantyExpired.copy(alpha=0.15f) + 红色文字）
- [ ] Step 2: TrashItemCard 内「还原/删除」按钮（现状行内小按钮）→ 小型 LiquidGlassButton / LiquidIconButton（还原=主色图标、删除=红色图标），卡片行局部捕获层
- [ ] Step 3: 编译验证 + 提交

### Task 10: 分类管理 FAB + 卡片按钮玻璃化

**Files:** Modify: `ui/category/CategoryScreen.kt`

- [ ] Step 1: FloatingActionButton（55 行添加）→ 液态玻璃胶囊 FAB（LiquidGlassButton tint=Primary() 白字「添加分类」或「+」图标，局部捕获层或复用页面局部层——按布局实际处理）
- [ ] Step 2: 分类卡片「编辑/删除」IconButton（187/190 行）→ LiquidIconButton（局部捕获层）
- [ ] Step 3: 编译验证 + 提交

### Task 11: 备份页一键导出/导入玻璃化

**Files:** Modify: `ui/backup/BackupScreen.kt`（92/126 行区域）

- [ ] Step 1: 「一键导出」「一键导入」行 → 全宽 LiquidGlassButton（导出=主色 tint 白字 + Upload 图标；导入=中性玻璃 + Download 图标），行局部捕获层
- [ ] Step 2: 编译验证 + 提交

### Task 12: 全量验证

- [ ] Step 1: `.\gradlew.bat :app:assembleDebug` BUILD SUCCESSFUL
- [ ] Step 2: `.\gradlew.bat :app:testDebugUnitTest` 通过
- [ ] Step 3: `rg "FilterChip|FloatingActionButton|OutlinedButton|Button\(" app/src/main/java` 残留盘点（仅允许 TextButton 用于弹窗内 + 合理例外，逐条说明）
- [ ] Step 4: 最终审查 + 收尾

---

## Self-Review 结论

- 设计文档 A-F 节全覆盖：A=Task2, B=Task3, C=Task4, D=Task5-11, E=Task1, F=全部任务携带
- 类型一致性：LiquidGlassButton 新参数、LiquidIconButton(onClick, backdrop, modifier, size, tint, enabled, content)、LiquidFilterChip(selected, onClick, backdrop, modifier, label, labelColor) 跨任务一致
- 无占位符：每任务含具体文件/行/组件与验证方式