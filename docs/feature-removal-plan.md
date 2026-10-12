# 剔除项技术方案：A-3 / A-5 / A-6 / A-9 / A-8

> 基线：v6.11.0（Kotlin 2.0 + Compose M3 + Room 2.6.1 + Hilt，93 个 .kt / 11,881 行）
> 前置：本方案只做**删除与收缩**，**本轮无新增项**。审查清单里带"改造/下沉/兜底"性质的收尾动作已统一移出，见 §6。
> 依赖关系：`A-3 → A-5(部分) → A-6 → A-8`，`A-9` 独立。批次切分见 §3。

---

## 0. 一屏摘要

| 剔除项 | 净效果（估） | 关键决策 |
|---|---|---|
| A-3 游戏化 | 删 4 个文件（ExpService / ExpCalculator / AchievementSeeds / MyLandingViewModel），MyLandingScreen 由 487 行降至约 130 行，6 个 ViewModel 摘除注入与 11 处调用点 | **DB 不升版本、不删表**（§1.1 方案 A）；"3 枚价值里程碑下沉统计页"不做 |
| A-5 交互装饰 | 删 pager 同步状态机、2 处下拉刷新、回收站倒计时、4 项外观链路（其中 3 项已被证实是**死设置**）、引导 5 页→1 页 | 保留主题三态 + 默认主色；「我的」Tab 与功能入口**保留** |
| A-6 搜索筛选层 | 删 4 个 Chip + 3 个展开行 + 1 个排序菜单 ≈ 200 行 UI 与整条筛选管线 | **复核更正**：状态筛选并非失效，删除理由改为"维度与价值核对无关"；"默认按价值排序"不做 |
| A-9 分类包装层 | 仅删分类排名三色奖牌（1 处） | 图标按名称硬编码**本期不动**（`iconName` 本就无 UI 读取点，改动得不偿失）；"按 id 匹配 + 自定义兜底"属新增能力，不做 |
| A-8 标签录入与聚合 | 删录入控件 + 统计页标签资产分布块 + 2 个 DAO 方法 | `tags` 字段读写路径**必须保留**（备份协议 manifest.json:137/272 依赖它）；只读展示保留 |

合计净减约 **1,150 行**，整体删除 **4 个文件**，**零 schema 变更、零数据删除**。

> **时效性说明（2026-10-12 追加）**：本文档所述批次（A-3/A-5/A-6/A-9/A-8）本身确实零 schema 变更，结论仍然成立。但同一发布周期内另有一次改动——按用户要求移除「售出金额/售出日期」机制——把 DB 升到了 **v7**（表重建，见 `docs/db-migration-notes.md`）。因此 §1.1 与 §4 里「装回 v6.11 APK 即可回退 / 双方 `version = 6` 可互装」的说法**只对本文档批次自身成立**：当前代码库已是 v7，降级安装会被 Room 拒绝（`A downgrade from version 7 to version 6 was required but not found`），回退需用 `git revert` 并保持 DB 为 v7，或卸载重装后从备份恢复。

---

## 1. 受影响的接口与数据结构

### 1.1 持久层（Room）

三张与 A-3 相关的表：`user_profile`、`achievements`、`experience_log`（`AppDatabase.kt:11-31`，`version = 6`，`exportSchema = false`）。三种处置对比：

| 方案 | schema/版本 | 残留 | 回滚代价 |
|---|---|---|---|
| **A（推荐，本期）** | 完全不动：7 个 entity、7 个 DAO 声明、`version = 6`、迁移链 `MIGRATION_1_2…5_6`（`DatabaseModule.kt:37-41`）全部保留 | 3 个 Entity + 3 个 DAO 死声明（约 200 行）+ 3 张空/旧表 | **`git revert` 即回到原状，旧数据完好**；v6.11 ↔ 本轮版本可互装（Room 身份哈希与版本都一致） |
| B：删表 | 删 entity/DAO 声明 + `version = 7` + `MIGRATION_6_7` 内 `DROP TABLE` | 无 | 不可回滚安装：v7 库对 v6 触发 Room 降级保护（`A downgrade from version 7 to version 6 was required but not found`），且表已 DROP，v6 首查即 `no such table`。回滚必须「卸载 v7 → 装 v6 → 恢复剔除前的备份」 |
| C：只删声明不删表 | 删 entity/DAO 声明 + `version = 7` + 空体 `MIGRATION_6_7` | 无（表仍在磁盘，Room 不校验未声明表） | 与 B 相同：版本号单向往上，降级保护同样阻断互装 |

**决策：走 A。** 本轮收益是"删掉与价值无关的代码"，不是"清理三张空表"；B/C 把 200 行死声明的清理换成"回滚必须依赖备份 + 重装"，代价明显更大。若要彻底清理，应在下一版单独立项（届时与"备份恢复"一起发布）。

> 定量证据（方案 A 的安全性来源）：数据库备份是**原始文件覆盖**——`BackupRepository.kt:128-147` `backupDatabase()` 复制 `lootarchive.db`，`:149-158` `restoreDatabase()` 用 `FileInputStream/FileOutputStream` 直接覆盖；因此任何版本号变化都会立刻变成向下不兼容。版本号不动，备份文件就是双向可用的。

### 1.2 持久层（DataStore）

`SettingsRepository.kt` 的 5 个外观键，本轮处置：

| 键 | 定义 | 读取方 | 处置 | 备注 |
|---|---|---|---|---|
| `avatar_uri` | `:21`, flow `:87` | `SettingsViewModel.kt:24/88-89`、`MainScreen.kt:76/205`、`MyLandingScreen.kt:39/104-105` | **删** | 有真实 UI（`SettingsScreen.kt:62-65/171-200`，`avatarPicker` 于 `:173` 触发） |
| `custom_background_uri` | `:20`, flow `:81-82` | `SettingsViewModel.kt:134-139` | **删** | **死设置**：`SettingsScreen.kt:55-59` 的 `bgImageLauncher` 声明后全仓库无 `.launch()` 调用，且无任何渲染侧读取该值 |
| `app_name` | `:22`, flow `:105` | `SettingsViewModel.kt:25/73-74`、`HomeViewModel.kt:28/60-61/71` | **删** | **死设置**：无任何 UI 入口，`HomeUiState.appName` 也无任何渲染读取 |
| `dynamic_color` | `:24`, flow `:99` | `SettingsViewModel.kt:23/83-84/125`、`SettingsScreen.kt:138-166`（Switch `:164-165`）、`Theme.kt:111/133`、`MainActivity.kt:52/59` | **删** | 唯一有真实 UI 的外观删除项 |
| `primary_color` | `:23`, flow `:93` | `MainActivity.kt:51/59`、`Theme.kt:110/129` | **保留** | 设置页无取色 UI（grep 无 `setPrimaryColor` 调用方），值恒为默认 `0xFFFFA500`；`SettingsViewModel.kt:119-121` 的死 setter 随批次清理 |

存量键值**不做清理**：无迁移、无副作用（DataStore 里多几个无人读的键不影响任何行为）。

### 1.3 备份/导出协议（只读约束，本轮不得破坏）

`BackupUtil.kt` 的 `manifest.json`（`version = 4`，`:156-163`）字段清单：`version / exportDate / appVersion / itemCount / categoryCount / categories(id,name,sortOrder,iconName) / items(id,name,categoryId,purchasePrice,storageLocation,purchaseDate,warrantyExpiryDate,warrantyPeriodDays,description,status,salePrice,saleDate,tags,lastStatusChangedAt,photos)`。

- **不含成就 / EXP / profile** → A-3 对 ZIP 备份与恢复**零影响**（README 里"备份含成就与 EXP"的说法与代码不符）。
- **含 `tags`（`:137` 写、`:272` 读）** → A-8 只能删 UI 与聚合，`ItemEntity.tags` 的读写必须原样保留。
- **含 `iconName`（`:151` 写、`:248` 读）** → A-9 不动图标种子，避免协议字段语义变化。
- CSV 表头 `名称,分类,价格,位置,购入日期,状态,标签`（`StatisticsScreen.kt:365`）本轮不变。

### 1.4 应用接口：删除/收缩清单

**A-3（服务与数据）**

| 符号 | 位置 | 处置 | 受影响调用方 |
|---|---|---|---|
| `ExpService`（`recalculateProfile()`、`unlockFlow`、成就检查、连击计算） | `data/ExpService.kt`（约 130 行） | 整文件删除 | 6 个 ViewModel 的构造注入 + 11 处调用 |
| `ExpCalculator`（10 级表 / 称号） | `util/ExpCalculator.kt`（约 42 行） | 整文件删除 | `MyLandingScreen.kt:357`、`ExpService` |
| `AchievementSeeds.all` | `data/AchievementSeeds.kt`（约 28 行） | 整文件删除 | `MyLandingViewModel.kt:41`、`DatabaseModule` 种子 |
| `MyLandingViewModel` | `ui/MyLandingViewModel.kt`（约 72 行） | 整文件删除 | `MyLandingScreen.kt:52-53` |
| `UserProfileDao.addExp` | `data/local/dao/UserProfileDao.kt:19` | 保留（本就无调用方） | — |
| `ExperienceLogDao.getRecentLogs` | `data/local/dao/ExperienceLogDao.kt:13` | 保留（本就无调用方） | — |
| `DatabaseModule` 三个 provider | `di/DatabaseModule.kt:80`（userProfileDao）、`:83`（achievementDao）、`:86`（experienceLogDao） | 删 | 无消费方后为纯死代码 |
| 成就种子调用 | `di/DatabaseModule.kt:59` 附近（与 `seedDefaultCategories` 同一 `onCreate` 回调；原生种子 SQL 见 `:49`） | 只删成就种子，**保留分类种子** | — |

**A-3（UI）**

| 符号 | 位置 | 处置 |
|---|---|---|
| `MyLandingScreen(avatarUri, onNavigateTo…×5)` | `ui/MyLandingScreen.kt:38-45`（487 行） | 删 `avatarUri` 参数；保留 5 个导航回调 |
| 弹窗状态 `showLevelDialog / showExpDialog / showAchievementDetail` | `:46-49` | 删 |
| `profileVM / profileState` | `:52-53` | 删（`homeVM` `:50-51` 保留） |
| 双维度收藏家等级（数量线/价值线星级） | `:54-83` | 删 |
| 收藏家卡片（头像 + Lv + EXP 行） | `:85-196`（EXP 行 `:160-196`） | 删 |
| 成就徽章墙 | `:198-253` | 删 |
| 收藏亮点（最贵/最老） | `:255-283` | **保留** |
| 功能入口（设置/分类/备份/回收站/关于） | `:285-344` | **保留** |
| EXP 详情弹窗 | `:346`-约 `:400`（`:357/:367/:382/:385`） | 删 |
| `MyMenuItem` | `:459` | 保留 |
| `LevelRule` | `:481` | 删（仅弹窗使用） |

**A-5（导航/状态/外观）**

| 符号 | 位置 | 处置 |
|---|---|---|
| `HorizontalPager` + pager↔tab 同步状态机 | `MainScreen.kt:9` import、`:90` `tabRequestTarget`、`:109` `pagerState`、`:113-148`（含 `:129` `isPagerDragged`、`:136-139` `animateScrollToPage/scrollToPage`、`:144-147` `settledPage` 回写）、`:181-182`、`:298` `selectedTabIndex` | 删；`currentTab`(`:67`)/`switchTab`(`:98`)/`isSubPage`/`AnimatedContent`/`BackHandler` 保留 |
| 下拉刷新 | `HomeScreen.kt:23/75/105`、`StatisticsScreen.kt:15/67`（含 `delay(600)` 假刷新）、`HomeViewModel.refresh()` `:85`、`StatisticsViewModel.refresh()` | 删；**必须在 A-3 之后**（`HomeViewModel.refresh()` 删掉 EXP 后是空壳） |
| 回收站到期倒计时 | `RecycleBinScreen.kt:194`（`it + 14L*24*60*60*1000` 与到期文案） | 删展示；`:89` 策略文案、`TrashCleanupWorker`（14 天阈值）、`deletedAt`、还原/彻底删除保留 |
| 引导 5 页 | `OnboardingScreen.kt:35-48`（`:36` 欢迎 / `:39` 记录物品 / `:42` 资产一目了然 / `:45` 收藏家成长体系 / `:48` 开始收藏之旅） | 保留 `:39` 一页，删 `:42/:45/:48`，页码 `"1 / 5"` → `"1 / 1"`，清理仅被删页引用的图标 import（`BarChart`/`EmojiEvents`/`RocketLaunch`）；`MainActivity.kt:57` 门控与 `:71` `onComplete` 保留 |

**A-6（搜索）**

| 符号 | 位置 | 处置 |
|---|---|---|
| `SearchUiState.categories / activeFilter / statusFilter / tagFilter / allTags` | `SearchViewModel.kt:24/25/29/30/31` | 删字段 |
| `SearchUiState.sort` | `:26`（默认 `"date_new"`） | **保留字段**，仅删 UI（零行为变化） |
| `setActiveFilter / setSort / setStatusFilter / setTagFilter` | `:94-97` | 删 |
| `executeSearch` 过滤管线 | `:105-138`（`:112-126` 范围、`:127-128` 状态、`:130-131` 标签、`:132-137` 排序） | 收缩为「SQL 查询 → 首图映射 → 结果」 |
| `CategoryRepository` 注入与其订阅 | `:38` + `init` | 删（仅服务分类筛选 Chip） |
| `SearchScreen` 折叠式筛选行 | `:97-254`（chip 行 `:103-174` 含 `:168-173` "N 项筛选"计数、范围行 `:176-197`、状态行 `:199-220`、标签行 `:222-254`）、`statusLabels :53`、4 个 `show*Row/showSortMenu` 状态 | 删 |
| 结果统计 + 排序菜单 | `:256-297`；`:259`/`:300` 条件里的筛选判断 | 删排序菜单，保留「找到 N 件物品」文案 |
| 查询输入 / 最近搜索 / 结果卡 | `:60-95` 一带、`:299-330`、`:340-386` | **保留** |
| `ItemDao.searchItemsWithTags` | `ItemDao.kt:139-144` | **原样不动**（`name/storageLocation/description/tags` LIKE 全保留） |

> **复核更正（必须记入）**：审查清单里"状态筛选是失效的（MainScreen.kt:113-150 传 key 与 SearchViewModel.kt:85-121 期望值不一致）"**不成立**。证据：`SearchScreen.kt:53` 的 `statusLabels` 键取自 `ItemStatus.entries…it.code`，`:206-210` 点击即以该 code 调 `setStatusFilter(k)`；`SearchViewModel.kt:127-128` 比对 `it.status == statusF`；`ItemStatus.kt:8-12` 的 code 正是 `"active"/"idle"/"sold"/"repair"/"lost"`，与库内存储一致；`MainScreen.kt:228` 只传导航回调，根本不传筛选参数。删除 A-6 的理由改为：4 个 Chip + 3 个展开行 + 1 个排序菜单约 200 行 UI，与「核对某件物品的价值」无关。

**A-8（标签）**

| 符号 | 位置 | 处置 |
|---|---|---|
| 标签 Chip 行 + 输入行 | `AddItemScreen.kt:408-430` | 删 |
| `addTag / removeTag` | `AddItemViewModel.kt:171-181` | 删 |
| 编辑态读 tags / 保存写 tags | `AddItemViewModel.kt:89` / `:240` | **保留** |
| 标签资产分布块 | `StatisticsScreen.kt:314-345`（`tagData` 计算 `:315-319`） | 删 |
| `ItemRepository.getAllTags()` / `ItemDao.getAllTags()` / `getItemsByTag()` | `ItemDao.kt:133-134` / `:130-131` | 删（A-6 删掉标签 Chip 后已无消费方） |
| 详情页 / 首页标签只读展示 | `DetailScreen.kt:88`、`HomeScreen.kt:267` | **保留**（空标签不渲染，零改动） |
| 备份读写 tags | `BackupUtil.kt:137` / `:272` | **保留（不可删）** |

> 行为后果（需确认后执行）：删掉录入控件后，**App 内不再有任何新增标签的入口**，只有历史数据与导入备份里已有的标签会在详情页/首页显示。

**A-9（分类）**

| 符号 | 位置 | 处置 |
|---|---|---|
| 分类排名三色奖牌 | `StatisticsScreen.kt:305`（`when (i) { 0 -> 0xFFF59E0B; 1 -> 0xFFA8A29E; 2 -> 0xFFCDA87B; else -> Transparent }`） | 删底色，保留序号文本与金额/件数（`:306-308`） |
| `CategoryIcons` 按名称映射 + 兜底 | `CategoryScreen.kt:36-46` / `:184`（`?: Icons.Rounded.Folder`） | **不动** |
| 分类图标种子 | `DefaultCategories.kt:7-18`、`CategoryRepository.kt:47-49`、`DatabaseModule.kt:49` | **不动** |

> 不动图标的理由：`CategoryEntity.iconName`（`CategoryEntity.kt:11`）全仓库**只有写路径**（种子 `:49`、`createCategory` `:24-27`、备份写出 `:151`）与读备份路径（`:248`），**没有任何 UI 读取它**——分类图标实际由 `CategoryScreen` 的名称映射决定。删种子只减少一行死数据，却要动备份字段语义与两处种子入口，收益为负。

---

## 2. 实现步骤（按剔除项）

> 公共前置：每项开工前 `git switch -c removal/<item>`；每项完成即 `assembleDebug` + 冒烟，再合并；每个提交对应 §4 的回滚单元。

### A-3 游戏化

1. **摘写入方**（先做，保证中途可编译）：在 `AddItemViewModel.kt:250`、`BackupViewModel.kt:170`、`HomeViewModel.kt:41/82/85`、`DetailViewModel.kt:68/88`、`RecycleBinViewModel.kt:41/49/65/81` 删除 `expService.recalculateProfile()`，同步删除各自构造函数里的 `expService` 参数与 import。
2. **删服务与数据源**：删 `data/ExpService.kt`、`util/ExpCalculator.kt`、`data/AchievementSeeds.kt`。
3. **摘 DI 与种子**：`di/DatabaseModule.kt` 删 `:80/:83/:86` 三个 provider 与 `:59` 附近的成就种子调用（保留分类种子与 `:49` 的分类 SQL）。
4. **重建「我的」页**：按 §1.4 的 UI 表删 `MyLandingScreen.kt` 的 6 个区块与 `LevelRule`，删 `ui/MyLandingViewModel.kt`。
5. **收尾核对**：`grep -n "recalculateProfile\|unlockFlow\|ExpService\|ExpCalculator\|AchievementSeeds\|MyLandingViewModel" app/src/main` 应为 **0 命中**；`grep -n "Lv\.\|EXP\|成就\|连击" app/src/main/java/com/nanji/lootarchive/ui/MyLandingScreen.kt` 应为 0。

- **改动范围**：4 个文件整体删除；`MyLandingScreen.kt`（487 → 约 130 行）；6 个 ViewModel；`di/DatabaseModule.kt`。DB 与 Entity/DAO 声明不动。
- **关联依赖的清理点**：`ExpService.unlockFlow` 唯一消费者 `MyLandingViewModel.kt:64`；`AchievementDao` 的 6 个方法消费者（`MyLandingViewModel.kt:40/41/56`、`ExpService.kt:100/104-116/141`）随服务消失而全部归零，DAO 声明保留待下一版清理；`HomeViewModel.refresh()` 与 `StatisticsViewModel.refresh()` 在本项后变为空壳 → 移交 A-5 的 S2 处理。
- **「我的」Tab 保护性决策**：**不删整个 Tab**，只删成长区块。该页同时承载「收藏亮点（最贵/最老，`:255-283`）」与「功能入口（`:285-344`）」，后者是设置/分类/备份/回收站/关于的**唯一入口**，删页会连带砍掉备份与回收站。
- **回滚方式**：分两个提交（服务层 / UI 层）各自 `git revert`。DB 未变、数据未删 → 回滚后旧功能与原数据立即恢复，无需备份或重装。

### A-5 交互装饰（5 个子任务，各自独立回滚）

- **S1 三主页面横滑**：`MainScreen.kt` 删 `HorizontalPager`、`pagerState`、`:113-148` 同步状态机，把 `:181-182` 的包裹改为 `when (currentTab) { 0 -> HomeScreen…; 1 -> StatisticsScreen…; 2 -> MyLandingScreen… }`，`:298` 改回 `selectedTabIndex = currentTab`。
  *实现细节（不算新增功能）*：用 `rememberSaveableStateHolder().SaveableStateProvider(currentTab)` 包住三分支，保住各页滚动位置；若不接受这一点，替代方案是接受切 Tab 后列表回到顶部。
  - 改动范围：`MainScreen.kt` 约 45 行；调用点 `MainScreen.kt:205`（与 A-3 同文件，注意合并冲突）。
  - 依赖清理点：`collectIsDraggedAsState`、`animateScrollToPage/scrollToPage`、`settledPage` 回写、`tabRequestTarget` 全部随状态机删除；`switchTab`/`BackHandler` 保留。
  - 回滚：单提交 revert；无状态、无数据影响。
- **S2 下拉刷新**：删 `HomeScreen.kt:23/75/105` 与 `StatisticsScreen.kt:15/67` 的 `PullToRefreshBox`（含 `delay(600)`）、`isRefreshing/refreshing` 状态，删 `HomeViewModel.refresh()`（`:85`）与 `StatisticsViewModel.refresh()`。
  - 依赖清理点：**顺序强依赖 A-3** —— `HomeViewModel.refresh()` 的唯一动作就是 `recalculateProfile()`，S2 必须先于或紧随 A-3。
  - 回滚：revert。
- **S3 回收站倒计时**：`RecycleBinScreen.kt:194` 起删到期计算与倒计时文案。
  - 依赖清理点：`TrashCleanupWorker`（14 天阈值）与 `deletedAt` **不得改动**，否则从"删展示"变成"删策略"。
  - 回滚：revert，独立无依赖，可任意插入。
- **S4 外观四项**：按 §1.2 表逐条删 `avatar_uri` / `custom_background_uri` / `app_name` / `dynamic_color` 的定义、flow、ViewModel 字段与 setter、UI 行；`primary_color` 保留，仅删死 setter `SettingsViewModel.kt:119-121`。
  - 改动文件：`SettingsRepository.kt`、`SettingsViewModel.kt`、`SettingsScreen.kt`（`:55-59`、`:62-65`、`:138-166`、`:171-200`）、`MainScreen.kt:76/205`、`MyLandingScreen.kt:38-45/104-105`、`HomeViewModel.kt:28/56-71`、`MainActivity.kt:52/59`、`Theme.kt:110-133`（签名保留 `primaryColor`）。
  - 依赖清理点：`MyLandingScreen` 参数与 A-3 同文件；`HomeViewModel` 的 5 源 `combine`（`:56-71`）收缩为 4 源。
  - 回滚：revert；DataStore 存量键不清理，回滚后旧值仍在。
- **S5 引导压到 1 屏**：`OnboardingScreen.kt:35-48` 保留 1 页、删 3 页、页码改 `"1 / 1"`、清理无用 import；`MainActivity.kt:57/71` 不动。
  - 依赖清理点：被删的 `:45` 页内容讲成长体系，**必须在 A-3 之后**；`onboarding_completed` 已置位的用户不会重看引导（无需清键）。
  - 回滚：revert。

### A-6 搜索三级 Chip 筛选层

1. `SearchScreen.kt`：删 `:97-254` 筛选行整块与 `:256-297` 排序菜单，收敛 `:259`/`:300` 的条件为 `uiState.query.isEmpty()`（保留最近搜索区与空状态）。
2. `SearchViewModel.kt`：删 5 个筛选字段与 4 个 setter，把 `executeSearch`（`:105-138`）收缩为「`itemRepository.searchItemsWithTags(q)` → `getAllFirstPhotos()` 映射」；删 `categoryRepository` 注入与 `init` 订阅。
3. `ItemDao.kt:139-144` **不动**。

- **改动范围**：`SearchScreen.kt` 约 200 行、`SearchViewModel.kt` 约 60 行。
- **关联依赖的清理点**：`SearchUiState.sort` 保留字段与默认值（UI 已无入口，行为零变化）；`allTags` 的数据源 `ItemRepository.getAllTags()` 移交 A-8 删除（顺序：先 A-6 后 A-8，避免中间态无消费者编译告警）。
- **回滚方式**：单提交 revert；无数据影响。

### A-8 标签的独立录入与聚合

1. `AddItemScreen.kt:408-430` 删标签控件；`AddItemViewModel.kt:171-181` 删 `addTag/removeTag`（`:89` 与 `:240` 的 tags 读写保留）。
2. `StatisticsScreen.kt:314-345` 删「标签资产分布」整块。
3. `SearchViewModel.kt:31/130-131`、`ItemRepository.getAllTags()`、`ItemDao.kt:130-134` 删标签查询与 `getItemsByTag`。
4. 保留：`DetailScreen.kt:88`、`HomeScreen.kt:267`（只读展示）、`BackupUtil.kt:137/272`、CSV 表头「标签」。

- **改动范围**：4 个文件，约 90 行。
- **关联依赖的清理点**：`tags` 列与 `ItemEntity.tags` 字段保留（备份与 CSV 往返依赖）；`AddItemViewModel` 的编辑态回填 `:89` 保留，否则编辑历史数据会**静默清空标签**——这是本项最容易被漏掉的回归点。
- **回滚方式**：单提交 revert；标签数据从未修改。

### A-9 分类包装层

- 只改一处：`StatisticsScreen.kt:305` 删三色 `Surface` 底色，改为与 `else` 分支一致的普通序号文本；`:302` 排序、`:306-308` 名称/金额/件数保留。
- **改动范围**：1 个文件 1 行（去 `Surface` 后约 ±3 行）。
- **关联依赖的清理点**：无。图标链路按 §1.4 结论不动。
- **回滚方式**：可并入 A-6 提交；如需独立回滚则单独成一个提交，`git revert` 即可。

---

## 3. 建议提交顺序（每批一个回滚单元）

| 批次 | 内容 | 强依赖 |
|---|---|---|
| C1 | A-3 服务层（写方摘除 + 4 文件删除 + DI/种子） | — |
| C2 | A-3 UI 层（`MyLandingScreen` 收缩 + 删 `MyLandingViewModel`） | C1 |
| C3 | A-5 S2 下拉刷新 + S5 引导 | C1（`refresh()` 已空壳） |
| C4 | A-5 S4 外观四项（含 `MyLandingScreen` 参数） | C2（同文件） |
| C5 | A-5 S1 横滑 + S3 回收站倒计时 | 与 C3/C4 无冲突，可并行 |
| C6 | A-6 搜索筛选层 → A-8 标签 → A-9 奖牌 | C3 之后（`SearchScreen`/`StatisticsScreen` 无重叠，但建议串联以便冒烟） |

---

## 4. 回滚方式总表

| 场景 | 操作 | 备注 |
|---|---|---|
| 任一批次回滚 | `git revert <sha>`（或还原分支） | 本轮**无 schema、无数据变更**，回滚即刻生效，不需要迁移 |
| 回滚后的数据 | 无需处理 | 成就/EXP/profile 行仍完整留在表中；标签、分类、图标字段未动 |
| 已发布版本回退安装 | 直接装回 v6.11 即可 | 双方 `version = 6`、实体集合一致 → Room 校验通过 |
| 设备级兜底 | 保留"剔除前导出的一份 ZIP 备份" | 仅在后续做了方案 B/C（升版本删表）时才真正需要；本轮不依赖它 |
| 明确不做 | DB 文件级"部分回滚" | `restoreDatabase()` 是整库覆盖，不存在按表回退 |

---

## 5. 验收与回归清单

- **编译与静态**：`./gradlew assembleDebug` 通过；反例 grep：A-3 后 `recalculateProfile|unlockFlow|ExpService|ExpCalculator|AchievementSeeds|MyLandingViewModel` = 0 命中；A-6 后 `activeFilter|statusFilter|tagFilter|allTags` 在 `ui/` 下 = 0 命中；S4 后 `avatarUri|backgroundUri|dynamicColor|appName` = 0 命中。
- **升级安装（关键）**：用 v6.11 覆盖安装本轮构建 → 冷启动无 Room 异常（版本/实体未变），物品数、总金额与升级前一致。
- **首页**：Hero 金额、保修提示、便当盒排序不变；下拉刷新已消失；不再有 600ms 假延迟。
- **统计页**：环形图/雷达图/净值趋势/月度柱状图照旧；分类排名无三色徽章但序号与金额完整；「标签资产分布」不再出现；CSV 导出表头仍为 `名称,分类,价格,位置,购入日期,状态,标签`。
- **搜索**：输入 → 300ms 防抖 → 结果卡；最近搜索与清空可用；无 Chip 行与排序菜单；`storageLocation/description/tags` 仍可命中。
- **「我的」页**：只剩收藏亮点（最贵/最老）+ 功能入口，设置/分类/备份/回收站/关于五个入口逐一点开可跳转。
- **回收站**：列表项无倒计时文案；删除 14 天仍被 `TrashCleanupWorker` 清空；还原与彻底删除可用。
- **备份往返（A-8 重点）**：导出 ZIP → 清库 → 恢复 → 物品/分类/照片/标签完整，`manifest.json` 的 `tags` 与 `iconName` 不报错；编辑一件带标签的历史物品并保存后，标签**未被清空**。
- **引导**：清数据后首启只出现 1 页，走完不再出现。

---

## 6. 本期明确不做（防止"无新增项"被破坏）

| 来源建议 | 性质 | 处置 |
|---|---|---|
| A-3「保留 3 枚价值里程碑，落到统计页」 | 新增展示 | 移入后续迭代（B 清单） |
| A-5「引导文案改写为『录入价格与数量』」 | 内容改写 | 本期保留现成 1 页，零文案改动 |
| A-6「搜索默认按价值排序（高→低）」 | 行为改造（1 行常量） | 本期保留 `sort = "date_new"`；若要做只改 `SearchUiState.sort` 默认值并单独记录验收差异 |
| A-8「改为从已有标签点选」 | 新增交互 | 本期直接删录入控件 |
| A-9「图标按 id 匹配 + 自定义兜底」 | 新增能力 | 本期不动图标链路（§1.4） |
| A-3 收尾的「DB 升 v7 删三张表」 | 存储层变更 | 下一版单独立项，与"备份恢复"一起发布（§1.1 方案 B/C） |
