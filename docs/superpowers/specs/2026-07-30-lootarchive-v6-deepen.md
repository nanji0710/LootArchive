# LootArchive v6.0 深耕版 — 设计规格书

> **Product:** 拾物集 ItemGlow — 离线本地物品资产管理工具
> **Stack:** Kotlin + Jetpack Compose + Material3 + Room + Hilt
> **Style:** Warm Glassmorphism + Bento Grid
> **Date:** 2026-07-30

---

## 一、成就系统完善（P0）

### 现状

13个成就已建表+种子数据，VM 有 `getAllAchievements()` Flow 收集，UI 显示为简单的 3 列网格（灰/亮色区分解锁态）。问题：
- 无解锁通知
- 无成就详情弹窗（标题+描述+进度+图标）
- 进度永远为 0（ExService 从未调用 `updateProgress`）
- ExperienceLogEntity 表存在但从不在 UI 中展示

### 设计

#### 1.1 成就详情弹窗

点击成就网格中任意成就：
```
┌─────────────────────────────────────┐
│  [图标]  百物之主                     │
│          收集100件物品                │
│                                     │
│  进度  ████████████░░░░░░  7/100    │
│                                     │
│  解锁条件：拥有物品达到100件           │
│  状态：未解锁（锁定）                  │
│                                     │
│  [知道了]                            │
└─────────────────────────────────────┘
```

**UI 规则：** AlertDialog + `RoundedCornerShape(28.dp)` + Fredoka标题 + 进度条 Primary() 色 + `MonoFont` 数字

#### 1.2 解锁通知

`recalculateProfile()` 检测到新成就时存入 Flow，MyLandingScreen 监听并弹出 Snackbar：
```
┌──────────────────────────────────────────────┐
│ 🎉 恭喜解锁成就：百物之主！                      │
└──────────────────────────────────────────────┘
```
3 秒自动消失，不阻塞操作。

**实现：** ExpService 加 `_achievementUnlockFlow: MutableSharedFlow<String>`，unlock 时 emit。MyLandingScreen 用 `LaunchedEffect` 收集。

#### 1.3 进度实时追踪

`recalculateProfile()` 中加成就进度更新：
```kotlin
achievementDao.updateProgress("items_5", minOf(ownedCount, 5))
achievementDao.updateProgress("items_20", minOf(ownedCount, 20))
// ... 等
```

AchievementEntity.progress 用于弹窗中展示进度条。

---

## 二、搜索功能完善（P1）

### 现状

VM 已有 `tagFilter`、`statusFilter` 状态和 setter，但 UI 中标签筛选未上。搜索历史仅内存。分类列表已加载但未用于筛选。

### 设计

#### 2.1 标签筛选 UI

在现有状态筛选 LazyRow 下方新增标签筛选行：
```
[全部状态] [在用] [闲置] [已出] [待修]
[全部标签] [蓝牙] [EDC] [礼物] [工具] ...
```
标签来自 `itemRepository.getAllTags()` Flow 收集。

#### 2.2 搜索历史持久化

用 DataStore 存取搜索历史（最多20条）：
```kotlin
// SettingsRepository 加：
val searchHistory: Flow<List<String>>
suspend fun addSearchHistory(query: String)
suspend fun clearSearchHistory()
```

#### 2.3 分类筛选芯片

在标签筛选行上方新增分类筛选行，复用首页的 CategoryChip 样式。

---

## 三、回收站增强（P1）

### 现状

14天保留提示，无自动清理。还原/删除单个操作。无撤销。

### 设计

#### 3.1 WorkManager 14天自动清理

```kotlin
class TrashAutoCleanupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        // 删除 deletedAt < now - 14天 的物品
        repository.hardDeleteExpiredItems()
        return Result.success()
    }
}
```

每天检查一次（`PeriodicWorkRequestBuilder<TrashAutoCleanupWorker>(1, TimeUnit.DAYS)`）。

#### 3.2 倒计时显示

回收站顶部提示区加每个物品的剩余天数：
```
删除于 2026-07-25  剩余 9 天后自动清空
```
过期项标记红色。

#### 3.3 Snackbar 撤销

还原/删除操作后显示 Snackbar：
```
「MacBook Pro」已还原   [撤销]
```
5秒内可撤销。

---

## 四、设置页深化（P2）

### 现状

仅主题切换+头像+保修天数+缓存清理。ColorWheel 组件存在但未接入。备份提醒 key 存在但无 UI。

### 设计

#### 4.1 主题色自定义（接入 ColorWheel）

设置页加"主题色"选项，点击弹出 `AlertDialog` 内嵌 `ColorWheel` 组件：
- 滑动色环选色
- 底部预设色块（琥珀/紫/蓝/绿/粉/自定义）

选色后存入 DataStore `KEY_PRIMARY_COLOR`，全局 `LocalPrimaryColor` 生效。

#### 4.2 备份提醒开关

设置页加一行 Switch：
```
[icon] 备份提醒  每周提醒备份数据   [Switch]
```
值存入 DataStore `KEY_BACKUP_REMINDER_ENABLED`。

#### 4.3 数据看板

设置页加载时计算总览统计：
```
存储占用  物品X件 · 照片Y张 · 数据库Z KB
```

---

## UI/UX Pro Max 合规性

| 规则 | 应用 |
|------|------|
| §2 `touch-target-size` | 成就网格每项 ≥48dp，搜索 FilterChip ≥44dp |
| §7 `motion-meaning` | 成就解锁 Snackbar + 成就详情弹窗 spring 动画 |
| §8 `undo-support` | 回收站操作 5 秒撤销 |
| §8 `progressive-disclosure` | 成就网格 → 点击展开详情弹窗 |
| §4 `no-emoji-icons` | 成就图标用 Material Icons（Icons.Rounded.EmojiEvents 等），不用 emoji |
| §6 `color-semantic` | 成就颜色统一用 Primary() / ChartColors |
| §5 `visual-hierarchy` | 搜索筛选按优先级排列：分类 > 标签 > 状态 |

---

## Spec Self-Review

1. **Placeholder scan:** 无 "TBD"/"TODO"。所有 UI 描述有具体数值和 API 名称。
2. **Internal consistency:** 四个功能独立，无互相依赖。共用 ExpService、SettingsRepository、ItemRepository。
3. **Scope check:** 每个功能都是对已有代码的增量增强，不改架构。
4. **Ambiguity check:** 全部 UI 交互有明确行为描述。
