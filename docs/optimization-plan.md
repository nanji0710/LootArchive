# 拾物集 ItemGlow — 全面优化方案

> 生成日期：2026-08-02 · 基于 UI/UX Pro Max + superpowers 深度审查
> 版本基线：v6.6.4 · 审查范围：全部 60+ Kotlin 源文件（UI 层 + 数据层 + 构建配置）

按优先级分 4 批（P0 → P3）。P0 是数据/功能/安全级别的硬伤，建议立即处理；P1 是本次优化重点（设计系统与可访问性）；P2 为架构与性能；P3 为工程化与清理。

## 执行状态（截至 2026-08-02，13 commit，25 单测，release/debug 构建通过）

| 批次 | 项 | 状态 |
|:--|:--|:--|
| P0 | 0.1 迁移链 / 0.2 Worker 调度 / 0.3 Zip Slip / 0.4 签名 / 0.5 更新校验 | ✅ 全部（0.4 keystore 文件待用户手动生成） |
| P1 | 1.1 token 统一 / 1.2 对比度 / 1.3 动态取色 / 1.4 触摸目标 / 1.7 图表语义 / 1.8 一致性 | ✅ |
| P1 | 1.5 字号下限 / 1.6 语义 | ⚠️ 主要完成（图表内部 9sp / testTag 未全覆盖） |
| P2 | 2.1 状态保存 / 2.3 N+1 / 2.5 IO+事务 / 2.6 status 枚举 / 2.7 假刷新 / 2.8 GlassSurface / 2.10 表单 / 2.12 大屏 | ✅ |
| P2 | 2.2 Navigation Compose / 2.9 反馈统一 / 2.11 设置流 | ⏳ 未做（2.2 高投入低收益；2.9/2.11 低价值） |
| P3 | 3.2 ABI / 3.3 版本号 / 3.4 去重 / 3.5 构建 / 3.7 CSV/Excel | ✅ |
| P3 | 3.1 备份/DAO 测试 / 3.6 死代码 / 3.8 依赖升级 | ⚠️ 部分（备份测试需 Robolectric；依赖升级建议独立回归） |

---

## P0 — 高风险硬伤（数据丢失 / 功能失效 / 安全漏洞）

### 0.1 DB 迁移缺口 — 升级即清库风险【数据安全】
- **证据**：`data/local/database/AppDatabase.kt:20-21` 版本 6，仅定义 `MIGRATION_1_2`、`MIGRATION_4_5`、`MIGRATION_5_6`，**缺 2→3、3→4**；`di/DatabaseModule.kt:36` 用 `.fallbackToDestructiveMigration()` 兜底。
- **后果**：从 v2/v3 schema 升级的存量用户会**静默清空全部物品数据**。个人资产管理 App 不可接受。
- **建议**：① 核对 git 历史确认 2→3、3→4 是否发布过（commit 含 schema 变更）；② 若发布过则补齐手写迁移，否则先移除 `fallbackToDestructiveMigration()`，改为显式 migration 列表 + 失败时先备份 DB。

### 0.2 保修 / 备份提醒 Worker 从未被调度【功能失效】
- **证据**：`worker/ReminderWorker.kt:19,69` 定义了 `WarrantyCheckWorker` / `BackupReminderWorker` 及 `schedule()`；但全仓库 grep 确认 **`LootArchiveApp.kt:36` 只调度了 `TrashCleanupWorker`**，两个提醒的 `schedule()` 无任何调用点。
- **后果**：README 宣传的"保修到期提醒、备份提醒"两个功能是死代码，**通知永远不会发出**。
- **建议**：在 `LootArchiveApp.onCreate`（及备份提醒开关变化时）补 `enqueueUniquePeriodicWork`（设置页已有开关与阈值配置）。

### 0.3 备份恢复 Zip Slip 路径穿越【安全】
- **证据**：`data/repository/BackupRepository.kt:73` `File(targetDir, entry.name)` 直接使用 zip entry 名，未校验 `../`。
- **后果**：恶意/损坏的备份 zip 可将文件写出应用目录（写入其他应用可访问路径）。
- **建议**：解压前校验 `entry.name` 规范化后必须以 `targetDir` 为前缀，否则跳过/报错；补单元测试覆盖。

### 0.4 release 用 debug keystore 签名【安全/发布】
- **证据**：`app/build.gradle.kts:13-20` 签名用 `~/.android/debug.keystore` 且口令硬编码 `android`。
- **后果**：任何人可用公开的 debug 证书重签覆盖发布包；正式发布无合法签名。
- **建议**：生成独立 release keystore，口令走环境变量（`System.getenv`/`gradle.properties` 不入库）。

### 0.5 APK 更新机制无签名 / 域名校验【安全】
- **证据**：`util/UpdateChecker.kt` + `util/ApkDownloadManager.kt:34-97`：`version.json` 明文托管于 GitHub raw，下载 APK 无 hash/签名校验即安装；`MyLandingScreen.kt:374` 用远端可控 `versionName` 拼文件路径。
- **后果**：DNS/网络被劫持或仓库被攻破时可下发恶意 APK。
- **建议**：固定允许域名白名单 + 校验发布签名的 SHA-256（或至少固定版本号格式白名单），versionName 做白名单清洗。

---

## P1 — 设计系统与可访问性（ui-ux-pro-max 优先规则 1/2/6 主导）

### 1.1 双主题体系 → 统一 Material 3 colorScheme tokens【核心】
- **证据**：`ui/theme/Color.kt:53-62`、`ui/theme/GlassEffect.kt:37-89` 大量 `if (LocalDarkTheme.current)` 手写主题感知色函数（`TextPrimary()/TextAuxiliary()/GlassBg()/CardBg()`），与 `Theme.kt:24-95` 的 Material `colorScheme` 并行两套体系，UI 层两套混用（`MainScreen.kt:184,249` 又在局部硬编码 `if(dark) White.copy(...)`）。
- **后果**：无法做动态取色/系统强调色；颜色无法被语义化维护；局部漂移（README 声称暖深棕卡片但 `Color.kt:26` 定义了不一致的深紫灰 `_SurfaceDark`）。
- **建议**：将 `GlassBg/CardBg/TextPrimary/TextAuxiliary` 等映射进 `ColorScheme`（`surfaceVariant/surface/onSurface/onSurfaceVariant/outline`），UI 全部改读 `MaterialTheme.colorScheme.*`；删除 `GlassEffect.kt:63-88` 中无引用的辅助函数。

### 1.2 对比度修复（WCAG AA 4.5:1）【高】
- **证据**：
  - 白字 + 琥珀主色按钮 ≈ **2.9:1**（`MainScreen.kt:227-236`、`AddItemScreen.kt:192,215-217,497-503`、`StatisticsScreen.kt:80`）
  - 辅助文字 `_TextAuxiliaryLight = #A8A29E` 在卡片上 ≈ **2.3:1**（`Color.kt:44`，广泛用于搜索占位/标签/未选中 tab）
  - 状态选中态灰字 2.4:1（`AddItemScreen.kt:279-289`）、奖牌白字压金/银 2.2-2.4:1（`StatisticsScreen.kt:225`）
- **建议**：主按钮文字改 `onPrimary` 语义色（深棕 `#3D1A00`）；辅助文字抬到 ≥4.5:1（如 `#57534E`）；奖牌改深色文字。可跑一次对比度脚本全量校验。

### 1.3 动态取色 Dynamic Color【中】
- **证据**：`Theme.kt:105-131` 始终用固定色板，未启用 `dynamicLightColorScheme()/dynamicDarkColorScheme()`。
- **建议**：Android 12+（本项目 minSdk 31 全量满足）提供动态取色选项，跟随壁纸；主色可自定义已实现（`LocalPrimaryColor`），补充"跟随壁纸"模式。

### 1.4 触摸目标 ≥48dp【高】
- **证据**：搜索栏仅 `44dp`（`MainScreen.kt:199`）、返回按钮 `42dp`（`SearchScreen.kt:60`）、照片删除按钮 `24dp`（`AddItemScreen.kt:168-174`）、分类编辑/删除 `32dp`（`CategoryScreen.kt:190-194`）、回收站操作约 `40dp`（`RecycleBinScreen.kt:191-206`）。
- **建议**：统一 `Modifier.minimumInteractiveComponentSize()` 或显式 ≥48dp；对纯视觉的 24dp 删除图标包一层 48dp 点击区域。

### 1.5 文字下限 12sp【中】
- **证据**：`HomeScreen.kt:191` "NEW" 徽章 9sp、`StatisticsScreen.kt:190,203` 柱图数值 9sp、`MyLandingScreen.kt:198` 10sp、`Type.kt:94` `labelSmall` 11sp。
- **建议**：全局抬到 ≥12sp；9sp 的仅作装饰且无信息承载。

### 1.6 语义与无障碍【高】
- **证据**：物品照片 `contentDescription = null`（`AddItemScreen.kt:163`、`SearchScreen.kt:373`、`DetailScreen.kt:137`，对比 `HomeScreen.kt:180` 用 item.name）；底部导航用 raw `clickable` 无 `selected` 语义（`MainScreen.kt:267-273`）；`MyLandingScreen.kt:159,238` 空 `Surface(onClick={})` 假按钮；全项目零 `testTag`。
- **建议**：照片带 `item.name` 替代文本；导航加 `role = Tab` + `selected`；删除假按钮；关键交互（保存/删除/底部导航）加 `Modifier.testTag()`（同时为后续 UI 测试铺路）。

### 1.7 图表可访问性【中】
- **证据**：`ui/component/RadarChart.kt`、`TrendLineChart.kt`、`StatisticsScreen.kt` 全部 Canvas 自绘，饼图/趋势线无图例、无数值标注、无数据表 fallback；图表色板 12 色中有多对低区分度（`Color.kt:98-103`：`#10B981`/`#84CC16`/`#14B8A6` 接近）。
- **建议**：饼图 ≥5% 扇区标注数值；趋势线终点标注；分类饼图提供"查看明细"数据表 fallback；色板改用色盲安全（colorblind-safe）顺序；图表色用 `ChartColors` 常量而非散落。

### 1.8 主题一致性清理【中】
- **证据**：魔法 hex 重复（`0xFFFFF8F0` 在 Home/Statistics 各一份而 `Color.kt:106` 已有 token；`0xFF10B981` 散落 5 文件；相机橙 `0xFFFF8C42` 三处）；圆角三套规格（`GlassEffect.kt:20-25` 与散落 16/20/24/28）；阴影三套配方（`GlassPanel.kt:61-66` / `NeoCard GlassComponents.kt:63` / `glassEffect`）。
- **建议**：收敛到 `GlassTier` + 主题 Shape 常量；硬编码色全部走 token；删除 Emoji 图标（`AddItemScreen.kt:140` "📝"、`MyLandingScreen.kt:86` "⭐"）。

### 1.9 字号体系走 Typography token【低】
- **证据**：`Type.kt` 定义了完整 `AppTypography`，但各屏大量 `fontSize = X.sp` 散写。
- **建议**：改用 `style = MaterialTheme.typography.*`，实现字号/行高全局可控。

---

## P2 — 架构与性能

### 2.1 路由状态保存（防旋转/进程重建丢页面）【高】
- **证据**：`MainScreen.kt:68-71` `currentTab/currentRoute/detailItemId/editItemId` 用普通 `remember`，Activity 重建（旋转、进程死亡）全部归零回首页。
- **建议**：改 `rememberSaveable`；必要时持久化 detail/edit id。这是当前最影响日常体验的架构问题。

### 2.2 手写路由 → Navigation Compose【中】
- **证据**：`MainScreen.kt:81-95` 手写 `mutableListOf<String>` 返回栈，无 SavedStateHandle、无参数建模、无深链接；`build.gradle.kts:121` 已依赖 `navigation-compose:2.8.5` 但未用。
- **建议**：渐进迁移到 Navigation Compose（类型安全路由 + deep link + 返回结果传递）。注意 Haze 全局状态与 AnimatedContent 转场的保持。

### 2.3 消除 N+1 查询【高】
- **证据**：
  - `HomeViewModel.kt:57-65`：对每件物品 `getItemPhotos(id).first()` 顺序查询（列表越大越慢）
  - `SearchViewModel.kt:140`：每搜索结果一次 `getFirstPhotoPath` 查询
  - `CategoryViewModel.kt:39-41` / `CategoryDrawerViewModel.kt:40-43`：每分类一次 `getCategoryItemCount(id).first()`，两文件重复
- **建议**：DAO 新增批量 join（一次取回 item 首图映射、`categoryId GROUP BY COUNT`），ViewModel 消费单次 Flow。

### 2.4 照片解码 OOM 风险【高】
- **证据**：`util/PhotoUtil.kt:63` `BitmapFactory.decodeStream` 无 `inSampleSize`，相册 12MP 照片一次解出 ~48MB 位图。
- **建议**：按目标尺寸（约 1280px）采样解码；`DetailScreen.kt:137` 照片改 `LazyRow` + `key`（当前一次性组合全部）。

### 2.5 文件 IO 移出主线程 + 事务【高】
- **证据**：`AddItemViewModel.kt:252-262` 保存时 `File.delete()` + 逐条 `addPhoto` 在 `viewModelScope`（Main）阻塞执行且无事务；`TrashCleanupWorker.kt:28-35` 阻塞删除无事务；`ItemRepository.kt:99-126` 删除文件与删记录无原子性。
- **建议**：整流程 `withContext(Dispatchers.IO)` + 包 `@Transaction`；删除顺序：先 DB 后文件（失败可重试，不产生孤儿 DB 记录）。

### 2.6 status 魔法字符串 → 枚举【高】
- **证据**：`"active"/"sold"/"idle"/"repair"/"lost"` 硬编码散落 12+ 处（`ItemEntity.kt:32`、`ItemDao.kt:46,60,72,78`、`HomeScreen.kt:78,80`、`AddItemScreen.kt:279`、`DetailScreen.kt:162`、`StatisticsViewModel.kt:56,71`、`Color.kt:79-93` 等），任何一处漏改口径不一致。
- **建议**：`enum class ItemStatus` + 统一 `labels/owned` 扩展；DAO/Entity 保留 String 列但读写收敛到枚举转换（含 DB 迁移兼容）。

### 2.7 假下拉刷新【高】
- **证据**：`HomeScreen.kt:91`、`StatisticsScreen.kt:55`：`onRefresh` 仅 `delay(600)` 后置 false，从不重新加载数据。
- **后果**：用户下拉刷新却无数据更新，错误反馈。
- **建议**：调用 `viewModel.refresh()`（StatisticsViewModel 已有此方法）。

### 2.8 卡片样板抽取【中】
- **证据**：`MyLandingScreen.kt` / `SettingsScreen.kt` / `BackupScreen.kt` / `StatisticsScreen.kt` 约 15 处重复 `Card(fillMaxWidth, RoundedCornerShape(20.dp), containerColor=if(dark)...)` 样板。
- **建议**：抽公共 `GlassSurface`（承接 1.8 的 token 化）。

### 2.9 反馈机制统一【中】
- **证据**：Toast（`SettingsScreen.kt:121`）、AlertDialog、内嵌 Surface、未用 Snackbar（`MyLandingScreen.kt:67` 死代码）四套并存；硬编码消失延迟 900ms-3000ms 不一（`CameraScreen.kt:86`、`RecycleBinScreen.kt:40`、`BackupScreen.kt:48`、`MyLandingScreen.kt:609`）。
- **建议**：统一 Snackbar + 进度态；延迟常量集中管理。

### 2.10 表单验证 UX【中】
- **证据**：`AddItemScreen.kt:509-516` 校验错误卡渲染在 scrollable Column 最底部，步骤 0/1 点保存时错误在屏幕外；步骤可无条件前进（`:212-218,:407-421`）；必填项拖到最后才报错。
- **建议**：每步前进前就地校验（名称/分类必填）；错误显示在提交按钮旁或 Snackbar；保存成功给一次性 Toast/触觉反馈。

### 2.11 设置流样板【低】
- **证据**：`SettingsViewModel.kt:46-90` 9 个独立 `collect` 循环。
- **建议**：合并为单个 `combine`。

### 2.12 大屏 / 横屏适配【中】
- **证据**：所有网格 `GridCells.Fixed(2)`（`HomeScreen.kt:93`、`SearchScreen.kt:353`、`CategoryScreen.kt:81`）；图表固定像素（`RadarChart.kt:134` 240f、`TrendLineChart.kt:32`、饼图 `140.dp`）。
- **建议**：网格改 `GridCells.Adaptive(minSize)` 或按 `WindowSizeClass` 切列数；图表尺寸随可用宽度缩放。

---

## P3 — 测试与工程质量

### 3.1 补核心逻辑测试【高】
- **现状**：`app/src/` 下仅 `main/`，**零测试文件**；`build.gradle.kts:161-173` 已声明 junit/turbine/mockk/coroutines-test/espresso 但全未使用。
- **优先补测**（按风险排序）：
  1. `BackupUtil.fullExport/fullImport` 往返一致性（含 status/tags/salePrice 字段全量）
  2. `BackupRepository.restorePhotos` Zip Slip 校验
  3. `ExpCalculator.getLevel/getLevelProgress` 边界
  4. `ExpService.recalculateProfile`（成就去重、streak 跨天）
  5. `ItemDao` 软删/还原/筛选及 `insertItem` REPLACE 级联删除照片问题
  6. `DateUtil/ExcelUtil` 多格式日期解析
- **注**：`ItemDao.insertItem` 用 `OnConflictStrategy.REPLACE`（`ItemDao.kt:100`），配合 `ItemPhotoEntity` 外键 CASCADE（`:15`），主键冲突会级联清光照片——建议改 `IGNORE` 或显式 upsert。

### 3.2 ABI 补 x86_64【中】
- **证据**：`build.gradle.kts:31-33` 仅 `arm64-v8a + armeabi-v7a`，x86_64 模拟器无法安装，堵死 instrumented test 本地路径。
- **建议**：加 `x86_64`（或按 buildType 区分）。

### 3.3 版本号 BuildConfig【低】
- **证据**：`MyLandingScreen.kt:347,401`、`SettingsScreen.kt:228` 硬编码 `"v6.6.4"`；`BackupUtil.kt:137` 导出 manifest 硬编码 `appVersion "5.5.0"`。
- **建议**：统一 `BuildConfig.VERSION_NAME`。

### 3.4 重复定义去重【中】
- **证据**：成就种子数据两份（`DatabaseModule.kt:61-75` 与 `MyLandingViewModel.kt:74-88`）；默认分类两份（`DatabaseModule.kt:42-51` 与 `CategoryRepository.kt:41-52`）。
- **建议**：抽共享常量/资源单源维护。

### 3.5 构建配置清理【低】
- **证据**：`gradle.properties:2` 硬编码本机 JDK 路径（换机即坏）；`packaging` 排除了 POI 资源但项目无 POI 依赖；`android.suppressUnsupportedCompileSdk=36` 压制警告。
- **建议**：移除 `org.gradle.java.home` 改用 JAVA_HOME/toolchain（本机已装 Corretto 17 可设系统 JAVA_HOME）；清理 POI exclude；核对 AGP 与 compileSdk 36 支持情况并去掉 suppress。
- **额外**：仓库根提交了过期 `LootArchive-release-v6.3.4.apk`（7.7MB），建议移除并 .gitignore。

### 3.6 死代码清理【低】
- **证据**：`ExpService.kt:92` `logAddItem` 空实现、`ExperienceLogDao/Entity` 从未写入；`ColorWheel.kt` 全文件无调用点；`GlassEffect.kt:63-88` 无引用函数；`MyLandingScreen.kt:67` 未用 SnackbarHostState。
- **建议**：删除或实现。

### 3.7 CSV 导出字段一致性【中】
- **证据**：`ExcelUtil.exportItemsToExcel`（7 列）与 `BackupUtil`（全字段）字段集不一致，Excel 导入静默丢 status/tags/salePrice/saleDate；CSV 直接拼接 `item.name` 无转义（公式注入风险）。
- **建议**：统一列定义；按 RFC 4180 转义。

### 3.8 依赖升级评估【低】
- **证据**：Room 2.6.1 → 2.7.x、CameraX 1.3.4 → 1.4+、Coil 2.7.0 → 3.x、lifecycle 2.8.7、Hilt 2.53.1。
- **建议**：小步升级并回归（尤其 Room schema 变更需迁移验证）；建议后续启用 version catalog。

---

## 建议执行顺序

1. **第一批（P0，安全/数据/功能）**：0.1 迁移 + 0.2 Worker 调度 + 0.3 Zip Slip + 0.5 更新校验 —— 风险最高，先修。
2. **第二批（P1，设计系统/可访问性）**：1.1 token 统一 → 1.2 对比度 → 1.4 触摸目标 → 1.6 语义 —— 用户可感知的视觉与可用性提升。
3. **第三批（P2，架构/性能）**：2.1 状态保存 → 2.3 N+1 → 2.4 照片解码 → 2.6 status 枚举 —— 体验与性能。
4. **第四批（P3，测试/工程化）**：3.1 补测试（建议与 0.3/2.5 一并 TDD 落地）→ 其余清理。

> 每批完成后回归构建 + 发布，避免一次性大重构引入回归。P1 设计系统重构建议以 `ui-ux-pro-max` 的 design-system 输出为基线渐进落地（当前项目 Warm Glassmorphism 语言可保留，重点是 token 化与一致性，而非换风格）。
