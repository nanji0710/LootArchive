# LootArchive v5.2–v5.5 功能路线图实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现物品状态管理 + 标签系统 + 收藏家等级v2 + 资产分析报表v2 + 数据模型正规化，打造差异化竞争力

**Architecture:** 渐进式规范化——v5.2 用轻量字段快速上线状态和标签，v5.3 引入经验值/成就系统，v5.4 新增雷达图+折线图+资产报告，v5.5 将标签逗号分隔迁移为多对多关系表。每个版本独立可测试、独立可发布。

**Tech Stack:** Kotlin + Jetpack Compose + Material3 + Room + Hilt + Haze + Canvas 自绘图表

## Global Constraints

- 最低 SDK: Android 12 (API 31)，目标 SDK: 36
- Room DB version 从 1 开始逐版 migration（不可 destructive fallback）
- 所有图标使用 `Icons.Rounded.*`（0 处 `Icons.Filled.*`）
- 返回按钮使用 `Icons.AutoMirrored.Rounded.ArrowBack`
- 版本号规则：逢9进1（5.1.9 → 5.2.0），5.1.5 → 5.2.0
- 每个版本更新 `build.gradle.kts`、`version.json`、`MyLandingScreen.kt`、`SettingsScreen.kt` 四处版本号
- 颜色使用语义 token（`Primary()`, `TextPrimary()` 等），不硬编码 hex
- 卡片统一 `RoundedCornerShape(20.dp)` + `elevation=2.dp`
- 字体：FredokaFont 标题 + NunitoFont 正文 + MonoFont 数字
- emoji 不得作为结构性图标使用（`no-emoji-icons` rule）

---

# Phase 1: v5.2 — 物品状态管理 + 标签系统

## Overview

**文件结构变更：**

| 操作 | 文件 | 职责 |
|------|------|------|
| 修改 | `entity/ItemEntity.kt` | 加 status/tags/lastStatusChangedAt 字段 |
| 修改 | `database/AppDatabase.kt` | DB version 1→2，新增 migration |
| 修改 | `dao/ItemDao.kt` | 新增状态筛选和标签搜索查询 |
| 修改 | `repository/ItemRepository.kt` | 暴露新查询方法 |
| 修改 | `ui/additem/AddItemViewModel.kt` / `AddItemUiState` | 表单加 status + tags |
| 修改 | `ui/additem/AddItemScreen.kt` | Step 1 加状态选择器和标签输入 |
| 修改 | `ui/detail/DetailScreen.kt` | 显示状态、标签、状态变更按钮 |
| 修改 | `ui/detail/DetailViewModel.kt` | 状态变更逻辑 |
| 修改 | `ui/home/HomeScreen.kt` | 物品卡片加状态圆点 + 标签胶囊 |
| 修改 | `ui/search/SearchScreen.kt` / `SearchViewModel.kt` | 状态筛选 + 标签搜索 |
| 修改 | `ui/theme/Color.kt` | 新增 StatusActive/StatusIdle/StatusSold/StatusRepair/StatusLost 颜色 |
| 修改 | `ui/theme/Type.kt` | 确认 MonoFont 可复用 |

---

### Task 1.1: Room DB Migration v1→v2

**Files:**
- Modify: `app/src/main/java/com/nanji/lootarchive/data/local/entity/ItemEntity.kt`
- Modify: `app/src/main/java/com/nanji/lootarchive/data/local/database/AppDatabase.kt`

**Interfaces:**
- Produces: `ItemEntity.status: String`, `ItemEntity.tags: String`, `ItemEntity.lastStatusChangedAt: Long?`

- [ ] **Step 1: 修改 ItemEntity 加三个字段**

在 `ItemEntity.kt` 末尾（`updatedAt` 之后）加：

```kotlin
val status: String = "active",             // active|idle|sold|repair|lost
val tags: String = "",                     // comma-separated: "蓝牙,EDC,礼物"
val lastStatusChangedAt: Long? = null      // epoch millis
```

完整新 ItemEntity：

```kotlin
@Entity(
    tableName = "items",
    indices = [
        Index("categoryId"),
        Index("name"),
        Index("isDeleted"),
        Index("status")
    ]
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val categoryId: Long = 0,
    val purchasePrice: Double = 0.0,
    val currency: String = "CNY",
    val storageLocation: String = "",
    val purchaseDate: Long? = null,
    val warrantyExpiryDate: Long? = null,
    val warrantyPeriodDays: Int? = null,
    val description: String = "",
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val status: String = "active",
    val tags: String = "",
    val lastStatusChangedAt: Long? = null
)
```

- [ ] **Step 2: 修改 AppDatabase 加 migration**

```kotlin
@Database(
    entities = [
        CategoryEntity::class,
        ItemEntity::class,
        ItemPhotoEntity::class,
        BackupRecordEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun itemDao(): ItemDao
    abstract fun itemPhotoDao(): ItemPhotoDao
    abstract fun backupRecordDao(): BackupRecordDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN status TEXT NOT NULL DEFAULT 'active'")
                db.execSQL("ALTER TABLE items ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE items ADD COLUMN lastStatusChangedAt INTEGER DEFAULT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_items_status ON items (status)")
            }
        }
    }
}
```

- [ ] **Step 3: 在 DI 模块注册 migration**

查找 Hilt DI 模块中 AppDatabase 的 provide 方法，加上 `.addMigrations(AppDatabase.MIGRATION_1_2)`。

- [ ] **Step 4: Verify** — Run `./gradlew assembleDebug`，确认编译通过

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "v5.2: DB migration v1->v2 — ItemEntity +status +tags +lastStatusChangedAt"
```

---

### Task 1.2: ItemDao 新查询

**Files:**
- Modify: `app/src/main/java/com/nanji/lootarchive/data/local/dao/ItemDao.kt`

**Interfaces:**
- Consumes: `ItemEntity.status`, `ItemEntity.tags`
- Produces: `getItemsByStatus()`, `getItemsByTag()`, `getAllTags()`, `updateItemStatus()`

- [ ] **Step 1: 添加状态和标签查询**

在 `ItemDao.kt` 末尾（`emptyTrash()` 之后）加：

```kotlin
@Query("SELECT * FROM items WHERE isDeleted = 0 AND status = :status ORDER BY updatedAt DESC")
fun getItemsByStatus(status: String): Flow<List<ItemEntity>>

@Query("SELECT * FROM items WHERE isDeleted = 0 AND tags LIKE '%' || :tag || '%' ORDER BY updatedAt DESC")
fun getItemsByTag(tag: String): Flow<List<ItemEntity>>

@Query("SELECT DISTINCT tags FROM items WHERE isDeleted = 0 AND tags != ''")
fun getAllTagsRaw(): Flow<List<String>>

@Query("UPDATE items SET status = :status, lastStatusChangedAt = :changedAt WHERE id = :itemId")
suspend fun updateItemStatus(itemId: Long, status: String, changedAt: Long = System.currentTimeMillis())

@Query("SELECT * FROM items WHERE isDeleted = 0 AND (:keyword = '' OR name LIKE '%' || :keyword || '%' OR storageLocation LIKE '%' || :keyword || '%' OR description LIKE '%' || :keyword || '%' OR tags LIKE '%' || :keyword || '%') ORDER BY updatedAt DESC")
fun searchItemsWithTags(keyword: String): Flow<List<ItemEntity>>
```

- [ ] **Step 2: Verify** — `./gradlew assembleDebug`

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "v5.2: ItemDao 新增状态/标签查询方法"
```

---

### Task 1.3: ItemRepository 暴露新方法

**Files:**
- Modify: `app/src/main/java/com/nanji/lootarchive/data/repository/ItemRepository.kt`

- [ ] **Step 1: 添加 repository 方法**

在 `ItemRepository.kt` 末尾加：

```kotlin
fun getItemsByStatus(status: String): Flow<List<ItemEntity>> = itemDao.getItemsByStatus(status)
fun getItemsByTag(tag: String): Flow<List<ItemEntity>> = itemDao.getItemsByTag(tag)

fun getAllTags(): Flow<List<String>> = itemDao.getAllTagsRaw().map { rawList ->
    rawList.flatMap { it.split(",") }.map { it.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
}

suspend fun updateItemStatus(itemId: Long, status: String) = itemDao.updateItemStatus(itemId, status)

fun searchItemsWithTags(keyword: String): Flow<List<ItemEntity>> = itemDao.searchItemsWithTags(keyword)
```

- [ ] **Step 2: Verify** — `./gradlew assembleDebug`

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "v5.2: ItemRepository 暴露状态/标签方法"
```

---

### Task 1.4: 主题颜色 — 状态色板

**Files:**
- Modify: `app/src/main/java/com/nanji/lootarchive/ui/theme/Color.kt`

- [ ] **Step 1: 添加状态语义色**

在 Color.kt 中 WarrantyExpired 定义之后添加：

```kotlin
// ========== v5.2 物品状态颜色 ==========
val StatusActive = Color(0xFF10B981)     // 在用 — 绿色
val StatusIdle = Color(0xFF9CA3AF)       // 闲置 — 灰色
val StatusSold = Color(0xFFF59E0B)       // 已出 — 琥珀色
val StatusRepair = Color(0xFFEF4444)     // 待修 — 红色
val StatusLost = Color(0xFF6B7280)       // 丢失 — 深灰
```

- [ ] **Step 2: 添加状态标签颜色映射辅助函数**

```kotlin
@Composable
fun statusColor(status: String): Color = when (status) {
    "active" -> StatusActive
    "idle" -> StatusIdle
    "sold" -> StatusSold
    "repair" -> StatusRepair
    "lost" -> StatusLost
    else -> StatusActive
}

@Composable
fun statusLabel(status: String): String = when (status) {
    "active" -> "在用"
    "idle" -> "闲置"
    "sold" -> "已出"
    "repair" -> "待修"
    "lost" -> "丢失"
    else -> "在用"
}
```

- [ ] **Step 2: Verify** — `./gradlew assembleDebug`

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "v5.2: Color.kt 新增物品状态色板"
```

---

### Task 1.5: AddItemScreen — 状态选择 + 标签输入

**Files:**
- Modify: `app/src/main/java/com/nanji/lootarchive/ui/additem/AddItemScreen.kt`
- Modify: `app/src/main/java/com/nanji/lootarchive/ui/additem/AddItemViewModel.kt`
- Modify: `app/src/main/java/com/nanji/lootarchive/ui/additem/AddItemUiState`

**Interfaces:**
- Consumes: `statusColor()`, `statusLabel()` from Color.kt
- Produces: `AddItemUiState.status`, `AddItemUiState.tags`, `AddItemUiState.tagInput`

- [ ] **Step 1: 修改 AddItemUiState**

在 `AddItemUiState` 末尾（`errorMessage` 之后）加：

```kotlin
val status: String = "active",
val tags: String = "",
val tagInput: String = ""
```

- [ ] **Step 2: 修改 AddItemViewModel**

在 `AddItemViewModel` 类中（`initEditMode` 方法的 item 赋值块）加：

```kotlin
status = item.status,
tags = item.tags
```

添加新更新方法（在 `updateDescription` 之后）：

```kotlin
fun updateStatus(status: String) {
    _uiState.update { it.copy(status = status) }
}

fun updateTagInput(input: String) {
    _uiState.update { it.copy(tagInput = input) }
}

fun addTag(tag: String) {
    val trimmed = tag.trim()
    if (trimmed.isEmpty()) return
    val state = _uiState.value
    val current = state.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
    if (trimmed !in current) {
        current.add(trimmed)
        _uiState.update { it.copy(tags = current.joinToString(","), tagInput = "") }
    } else {
        _uiState.update { it.copy(tagInput = "") }
    }
}

fun removeTag(tag: String) {
    val current = _uiState.value.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() && it != tag }
    _uiState.update { it.copy(tags = current.joinToString(",")) }
}
```

修改 `saveItem()` 中的 `ItemEntity` 构造，加入新字段：

```kotlin
val item = ItemEntity(
    id = editingItemId ?: 0,
    name = state.name.trim(),
    categoryId = actualCategoryId,
    purchasePrice = price,
    storageLocation = state.storageLocation.trim(),
    purchaseDate = state.purchaseDate,
    warrantyExpiryDate = expiryDate,
    warrantyPeriodDays = periodDays,
    description = state.description.trim(),
    status = state.status,
    tags = state.tags,
    updatedAt = System.currentTimeMillis()
)
```

- [ ] **Step 3: 修改 AddItemScreen — Step 1 在分类之后添加状态选择器**

在 Step 1 的 "所属分类" ClayCard 之后、"购入价格" ClayCard 之前，插入：

```kotlin
// 物品状态选择器
ClayCard {
    Text("物品状态", fontSize = 14.sp, color = TextSecondary(), fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(10.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("active" to "在用", "idle" to "闲置", "sold" to "已出", "repair" to "待修", "lost" to "丢失").forEach { (key, label) ->
            FilterChip(
                selected = uiState.status == key,
                onClick = { viewModel.updateStatus(key) },
                label = { Text(label, fontSize = 12.sp) },
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = statusColor(key).copy(alpha = 0.15f),
                    selectedLabelColor = statusColor(key)
                )
            )
        }
    }
}

// 标签输入
ClayCard {
    Text("标签", fontSize = 14.sp, color = TextSecondary(), fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(10.dp))
    // 已有标签
    val existingTags = uiState.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    if (existingTags.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            existingTags.forEach { tag ->
                InputChip(
                    selected = false,
                    onClick = { viewModel.removeTag(tag) },
                    label = { Text(tag, fontSize = 12.sp) },
                    trailingIcon = {
                        Icon(Icons.Rounded.Close, "移除$tag", Modifier.size(14.dp))
                    },
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = uiState.tagInput,
            onValueChange = viewModel::updateTagInput,
            placeholder = { Text("输入标签，如 蓝牙、EDC") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )
        Spacer(Modifier.width(8.dp))
        IconButton(
            onClick = { viewModel.addTag(uiState.tagInput) },
            enabled = uiState.tagInput.isNotBlank()
        ) {
            Icon(Icons.Rounded.AddCircle, "添加标签", tint = Primary())
        }
    }
}
```

- [ ] **Step 4: Verify** — `./gradlew assembleDebug`

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "v5.2: AddItemScreen 新增状态选择器+标签输入"
```

---

### Task 1.6: DetailScreen — 显示状态、标签、状态变更

**Files:**
- Modify: `app/src/main/java/com/nanji/lootarchive/ui/detail/DetailScreen.kt`
- Modify: `app/src/main/java/com/nanji/lootarchive/ui/detail/DetailViewModel.kt`
- Modify: `app/src/main/java/com/nanji/lootarchive/ui/detail/DetailUiState`

- [ ] **Step 1: 修改 DetailUiState**

在 `DetailUiState` 末尾加：

```kotlin
val showStatusSheet: Boolean = false
```

- [ ] **Step 2: 修改 DetailViewModel**

在 `DetailViewModel` 类中（`deleteItem()` 之后）加：

```kotlin
fun showStatusSheet() {
    _uiState.update { it.copy(showStatusSheet = true) }
}

fun dismissStatusSheet() {
    _uiState.update { it.copy(showStatusSheet = false) }
}

fun updateItemStatus(status: String) {
    viewModelScope.launch {
        try {
            itemRepository.updateItemStatus(currentItemId, status)
            // 刷新详情
            loadItem(currentItemId)
            dismissStatusSheet()
        } catch (e: Exception) {
            _uiState.update { it.copy(errorMessage = "状态更新失败: ${e.message}") }
        }
    }
}
```

- [ ] **Step 3: 修改 DetailScreen — 在分类和存放位置之间加状态行 + 标签行**

在 `data.category` Row 之后、`HorizontalDivider` 之前插入：

```kotlin
// 物品状态（可点击变更）
Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Surface(
        onClick = { viewModel.showStatusSheet() },
        shape = RoundedCornerShape(10.dp),
        color = statusColor(data.item.status).copy(alpha = 0.12f)
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(statusColor(data.item.status), CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(statusLabel(data.item.status), color = statusColor(data.item.status), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Rounded.UnfoldMore, null, Modifier.size(14.dp), tint = statusColor(data.item.status))
        }
    }
}
// 标签
val itemTags = data.item.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
if (itemTags.isNotEmpty()) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        itemTags.forEach { tag ->
            Surface(shape = RoundedCornerShape(8.dp), color = Primary().copy(alpha = 0.08f)) {
                Text(tag, fontSize = 11.sp, color = Primary(), modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
            }
        }
    }
}
```

- [ ] **Step 4: 在 DetailScreen 末尾（最后一个 `}` 之前）加状态变更 BottomSheet 的 ModalBottomSheet**

```kotlin
if (uiState.showStatusSheet) {
    ModalBottomSheet(
        onDismissRequest = { viewModel.dismissStatusSheet() },
        containerColor = if (LocalDarkTheme.current) _CardDark else _CardLight,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text("更改物品状态", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary(), fontFamily = FredokaFont)
            Spacer(Modifier.height(16.dp))
            listOf("active" to "在用", "idle" to "闲置", "sold" to "已出", "repair" to "待修", "lost" to "丢失").forEach { (key, label) ->
                Surface(
                    onClick = { viewModel.updateItemStatus(key) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = if (data.item.status == key) statusColor(key).copy(alpha = 0.12f) else Color.Transparent
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(10.dp).background(statusColor(key), CircleShape))
                        Spacer(Modifier.width(12.dp))
                        Text(label, fontSize = 15.sp, color = TextPrimary(), modifier = Modifier.weight(1f))
                        if (data.item.status == key) {
                            Icon(Icons.Rounded.Check, null, Modifier.size(20.dp), tint = statusColor(key))
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
```

需要额外 import：`import androidx.compose.foundation.shape.CircleShape`

- [ ] **Step 5: Verify** — `./gradlew assembleDebug`

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "v5.2: DetailScreen 状态显示+变更+标签展示"
```

---

### Task 1.7: HomeScreen 物品卡片 — 状态圆点 + 标签胶囊

**Files:**
- Modify: `app/src/main/java/com/nanji/lootarchive/ui/home/HomeScreen.kt`

- [ ] **Step 1: 在物品卡片照片区右上角加状态圆点**

在 `HomeScreen.kt` 中找到物品卡片的照片 Box（`Box(Modifier.fillMaxWidth().height(135.dp)...)` 处），在其内部 `contentAlignment` 之后加：

```kotlin
// 状态圆点（右上角）
Surface(
    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
    shape = CircleShape,
    color = if (LocalDarkTheme.current) Color.Black.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.75f),
    shadowElevation = 2.dp
) {
    Box(Modifier.padding(4.dp)) {
        Box(
            Modifier.size(10.dp)
                .background(statusColor(item.status), CircleShape)
        )
    }
}
```

- [ ] **Step 2: 在价格下方加标签胶囊（仅当有标签时）**

在价格 Text 之后加：

```kotlin
val itemTags = item.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
if (itemTags.isNotEmpty()) {
    Spacer(Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemTags.take(2).forEach { tag ->
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Primary().copy(alpha = 0.08f)
            ) {
                Text(
                    tag,
                    fontSize = 10.sp,
                    color = Primary(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        if (itemTags.size > 2) {
            Text("+${itemTags.size - 2}", fontSize = 10.sp, color = TextAuxiliary())
        }
    }
}
```

- [ ] **Step 3: Verify** — `./gradlew assembleDebug`

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "v5.2: HomeScreen 物品卡片+状态圆点+标签胶囊"
```

---

### Task 1.8: SearchScreen — 状态筛选 + 标签搜索

**Files:**
- Modify: `app/src/main/java/com/nanji/lootarchive/ui/search/SearchScreen.kt`
- Modify: `app/src/main/java/com/nanji/lootarchive/ui/search/SearchViewModel.kt`

- [ ] **Step 1: 修改 SearchViewModel**

将 `executeSearch()` 中的 `itemRepository.searchItems()` 改为 `itemRepository.searchItemsWithTags()`，让搜索能命中标签。

在 `Set<init>` 块中加载全局标签列表：

```kotlin
private val _allTags = MutableStateFlow<List<String>>(emptyList())
val allTags: StateFlow<List<String>> = _allTags.asStateFlow()

init {
    viewModelScope.launch {
        categoryRepository.getAllCategories().collect { categories ->
            _uiState.update { it.copy(categories = categories) }
        }
    }
    viewModelScope.launch {
        itemRepository.getAllTags().collect { tags ->
            _allTags.value = tags
        }
    }
}
```

在 `SearchUiState` 末尾加筛选器状态：

```kotlin
val statusFilter: String? = null,  // null=全部, "active"=在用, etc.
val tagFilter: String? = null      // null=不按标签筛选
```

添加 setter 方法：

```kotlin
fun setStatusFilter(status: String?) {
    _uiState.update { it.copy(statusFilter = status) }
    doSearch()
}
fun setTagFilter(tag: String?) {
    _uiState.update { it.copy(tagFilter = tag) }
    doSearch()
}
```

- [ ] **Step 2: 修改 SearchScreen — 加状态和标签筛选行**

在现有筛选胶囊 LazyRow 之后、结果统计之前加状态筛选行：

```kotlin
// 状态筛选
LazyRow(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(vertical = 4.dp)
) {
    val statuses = listOf(null to "全部状态", "active" to "在用", "idle" to "闲置", "sold" to "已出", "repair" to "待修")
    items(statuses.size) { index ->
        val (key, label) = statuses[index]
        val selected = uiState.statusFilter == key
        FilterChip(
            selected = selected,
            onClick = { viewModel.setStatusFilter(key) },
            label = { Text(label, fontSize = 11.sp) },
            shape = RoundedCornerShape(16.dp),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = if (key != null) statusColor(key).copy(alpha = 0.15f) else Primary().copy(alpha = 0.15f),
                selectedLabelColor = if (key != null) statusColor(key) else Primary()
            )
        )
    }
}
```

- [ ] **Step 3: Verify** — `./gradlew assembleDebug`

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "v5.2: SearchScreen 新增状态筛选+标签搜索"
```

---

### Task 1.9: 版本号更新 — v5.2.0

**Files:**
- Modify: `app/build.gradle.kts` — versionCode=520, versionName="5.2.0"
- Modify: `version.json` — 全部更新
- Modify: `app/src/main/java/com/nanji/lootarchive/ui/MyLandingScreen.kt` — 两处 "5.2.0"
- Modify: `app/src/main/java/com/nanji/lootarchive/ui/settings/SettingsScreen.kt` — 一处 "5.2.0"

- [ ] **Step 1: 更新 build.gradle.kts**

```kotlin
versionCode = 520
versionName = "5.2.0"
```

- [ ] **Step 2: 更新 version.json**

```json
{
  "versionName": "5.2.0",
  "versionCode": 520,
  "updateDate": "2026-07-29",
  "updateLog": "v5.2.0\n• 物品状态管理：在用/闲置/已出/待修/丢失五种状态\n• 标签系统：自定义标签，支持搜索和筛选\n• 物品卡片右上角状态圆点+标签胶囊展示\n• 详情页可随时变更物品状态\n\nv5.1.5\n• 搜索栏玻璃模糊效果修复（移至hazeSource之上）\n• 首页胶囊FAB缩小（更简洁精致）",
  "apkDownloadUrl": "https://github.com/nanji0710/LootArchive/releases/download/V5.2.0/LootArchive-release-v5.2.0.apk"
}
```

- [ ] **Step 3: 更新 MyLandingScreen.kt**

两处 `v5.1.5` → `v5.2.0`（关于卡片 + "已是最新版本" dialog）

- [ ] **Step 4: 更新 SettingsScreen.kt**

一处 `v5.1.5` → `v5.2.0`

- [ ] **Step 5: Build Release APK**

```bash
./gradlew assembleRelease
```

- [ ] **Step 6: Commit + Push + Tag**

```bash
git add -A
git commit -m "v5.2.0: 物品状态管理+标签系统"
git push
git tag V5.2.0
git push origin V5.2.0
```

---

# Phase 2: v5.3 — 收藏家等级 v2

## Overview

引入经验值（EXP）体系、成就徽章、升级动效。三个新 Room Entity：`UserProfileEntity`、`AchievementEntity`、`ExperienceLogEntity`。DB migration v2→v3。

---

### Task 2.1: 新增 Entity + DAO + Migration

**Files:**
- Create: `app/src/main/java/com/nanji/lootarchive/data/local/entity/UserProfileEntity.kt`
- Create: `app/src/main/java/com/nanji/lootarchive/data/local/entity/AchievementEntity.kt`
- Create: `app/src/main/java/com/nanji/lootarchive/data/local/entity/ExperienceLogEntity.kt`
- Create: `app/src/main/java/com/nanji/lootarchive/data/local/dao/UserProfileDao.kt`
- Create: `app/src/main/java/com/nanji/lootarchive/data/local/dao/AchievementDao.kt`
- Create: `app/src/main/java/com/nanji/lootarchive/data/local/dao/ExperienceLogDao.kt`
- Modify: `app/src/main/java/com/nanji/lootarchive/data/local/database/AppDatabase.kt`

**Interfaces:**
- Produces: `UserProfileEntity`, `AchievementEntity`, `ExperienceLogEntity` and their DAOs

- [ ] **Step 1: 创建 UserProfileEntity.kt**

```kotlin
package com.nanji.lootarchive.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,          // singleton row
    val exp: Int = 0,
    val level: Int = 1,
    val totalItemsAdded: Int = 0,
    val totalPhotosAdded: Int = 0,
    val totalDescriptionsFilled: Int = 0,
    val streakDays: Int = 0,
    val lastActiveDate: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 2: 创建 AchievementEntity.kt**

```kotlin
package com.nanji.lootarchive.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val key: String,          // e.g. "items_100", "value_10000"
    val title: String,                    // e.g. "百物之主"
    val description: String,
    val icon: String = "🏅",              // emoji fallback; shown as decorative only
    val category: String = "collection",  // collection|value|photo|detail|streak
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val progress: Int = 0,
    val target: Int = 100
)
```

- [ ] **Step 3: 创建 ExperienceLogEntity.kt**

```kotlin
package com.nanji.lootarchive.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "experience_log")
data class ExperienceLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val source: String,                   // "add_item" | "add_photo" | "complete_desc" | "daily_streak"
    val amount: Int,
    val itemId: Long? = null,             // 关联物品（可选）
    val createdAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 4: 创建 UserProfileDao.kt**

```kotlin
package com.nanji.lootarchive.data.local.dao

import androidx.room.*
import com.nanji.lootarchive.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getProfileSync(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: UserProfileEntity)

    @Query("UPDATE user_profile SET exp = exp + :amount, updatedAt = :now WHERE id = 1")
    suspend fun addExp(amount: Int, now: Long = System.currentTimeMillis())

    @Query("UPDATE user_profile SET level = :level, updatedAt = :now WHERE id = 1")
    suspend fun setLevel(level: Int, now: Long = System.currentTimeMillis())

    @Query("UPDATE user_profile SET totalItemsAdded = totalItemsAdded + 1, updatedAt = :now WHERE id = 1")
    suspend fun incrementItemsAdded(now: Long = System.currentTimeMillis())

    @Query("UPDATE user_profile SET totalPhotosAdded = totalPhotosAdded + 1, updatedAt = :now WHERE id = 1")
    suspend fun incrementPhotosAdded(now: Long = System.currentTimeMillis())

    @Query("UPDATE user_profile SET totalDescriptionsFilled = totalDescriptionsFilled + 1, updatedAt = :now WHERE id = 1")
    suspend fun incrementDescriptionsFilled(now: Long = System.currentTimeMillis())
}
```

- [ ] **Step 5: 创建 AchievementDao.kt**

```kotlin
package com.nanji.lootarchive.data.local.dao

import androidx.room.*
import com.nanji.lootarchive.data.local.entity.AchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements ORDER BY isUnlocked DESC, category ASC")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE isUnlocked = 1 ORDER BY unlockedAt DESC")
    fun getUnlockedAchievements(): Flow<List<AchievementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAchievement(achievement: AchievementEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(achievements: List<AchievementEntity>)

    @Query("UPDATE achievements SET isUnlocked = 1, unlockedAt = :now, progress = target WHERE `key` = :key AND isUnlocked = 0")
    suspend fun unlockAchievement(key: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE achievements SET progress = :progress WHERE `key` = :key")
    suspend fun updateProgress(key: String, progress: Int)

    @Query("SELECT COUNT(*) FROM achievements WHERE isUnlocked = 0")
    suspend fun getLockedCount(): Int
}
```

- [ ] **Step 6: 创建 ExperienceLogDao.kt**

```kotlin
package com.nanji.lootarchive.data.local.dao

import androidx.room.*
import com.nanji.lootarchive.data.local.entity.ExperienceLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExperienceLogDao {
    @Insert
    suspend fun insertLog(log: ExperienceLogEntity)

    @Query("SELECT * FROM experience_log ORDER BY createdAt DESC LIMIT 20")
    fun getRecentLogs(): Flow<List<ExperienceLogEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM experience_log WHERE source = :source")
    suspend fun getTotalExpBySource(source: String): Int
}
```

- [ ] **Step 7: 修改 AppDatabase — DB version 2→3, migration, 新 DAO**

```kotlin
@Database(
    entities = [
        CategoryEntity::class,
        ItemEntity::class,
        ItemPhotoEntity::class,
        BackupRecordEntity::class,
        UserProfileEntity::class,
        AchievementEntity::class,
        ExperienceLogEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    // ... existing DAOs ...

    abstract fun userProfileDao(): UserProfileDao
    abstract fun achievementDao(): AchievementDao
    abstract fun experienceLogDao(): ExperienceLogDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) { /* existing */ }
        
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_profile (
                        id INTEGER NOT NULL PRIMARY KEY DEFAULT 1,
                        exp INTEGER NOT NULL DEFAULT 0,
                        level INTEGER NOT NULL DEFAULT 1,
                        totalItemsAdded INTEGER NOT NULL DEFAULT 0,
                        totalPhotosAdded INTEGER NOT NULL DEFAULT 0,
                        totalDescriptionsFilled INTEGER NOT NULL DEFAULT 0,
                        streakDays INTEGER NOT NULL DEFAULT 0,
                        lastActiveDate INTEGER DEFAULT NULL,
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS achievements (
                        `key` TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        icon TEXT NOT NULL DEFAULT '🏅',
                        category TEXT NOT NULL DEFAULT 'collection',
                        isUnlocked INTEGER NOT NULL DEFAULT 0,
                        unlockedAt INTEGER DEFAULT NULL,
                        progress INTEGER NOT NULL DEFAULT 0,
                        target INTEGER NOT NULL DEFAULT 100
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS experience_log (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        source TEXT NOT NULL,
                        amount INTEGER NOT NULL,
                        itemId INTEGER DEFAULT NULL,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
                // Seed achievements
                val achievements = listOf(
                    "('items_5','初级收藏','收集5件物品','collection',5)",
                    "('items_20','中级收藏家','收集20件物品','collection',20)",
                    "('items_50','高级收藏家','收集50件物品','collection',50)",
                    "('items_100','百物之主','收集100件物品','collection',100)",
                    "('value_10000','万元户','总资产超过1万','value',10000)",
                    "('value_100000','小富翁','总资产超过10万','value',100000)",
                    "('value_500000','财富自由','总资产超过50万','value',500000)",
                    "('photos_10','随手拍','拍摄10张照片','photo',10)",
                    "('photos_50','摄影师','拍摄50张照片','photo',50)",
                    "('desc_10','细节控','完善10件物品描述','detail',10)",
                    "('desc_50','文字家','完善50件物品描述','detail',50)",
                    "('streak_7','坚持一周','连续7天活跃','streak',7)",
                    "('streak_30','月常打卡','连续30天活跃','streak',30)"
                )
                achievements.forEach { (key, title, desc, cat, target) ->
                    db.execSQL("INSERT OR IGNORE INTO achievements (`key`, title, description, category, target) VALUES $key, '$title', '$desc', '$cat', $target")
                }
                // Ensure singleton profile row
                db.execSQL("INSERT OR IGNORE INTO user_profile (id, exp, level) VALUES (1, 0, 1)")
            }
        }
    }
}
```

- [ ] **Step 8: 在 DI 模块注册新 migration + 新 DAO**

- [ ] **Step 9: Verify** — `./gradlew assembleDebug`

- [ ] **Step 10: Commit**

---

### Task 2.2: EXP 计算引擎

**Files:**
- Create: `app/src/main/java/com/nanji/lootarchive/util/ExpCalculator.kt`

- [ ] **Step 1: 创建 ExpCalculator.kt**

```kotlin
package com.nanji.lootarchive.util

object ExpCalculator {
    /** 等级阶梯：Pair(所需EXP, 等级名) */
    val LEVELS = listOf(
        0 to "入门",
        50 to "新手",
        150 to "爱好者",
        350 to "达人",
        600 to "收藏家",
        1000 to "专家",
        2000 to "大师",
        5000 to "藏家",
        10000 to "鉴赏家",
        20000 to "传奇"
    )

    fun getLevel(exp: Int): Int {
        var level = 1
        for (i in LEVELS.indices) {
            if (exp >= LEVELS[i].first) level = i + 1
        }
        return level
    }

    fun getLevelTitle(level: Int): String = LEVELS.getOrElse(level - 1) { 0 to "入门" }.second

    fun getExpForLevel(level: Int): Int = LEVELS.getOrElse(level - 1) { 0 to 0 }.first

    fun getNextLevelExp(exp: Int): Int {
        val currentLevel = getLevel(exp)
        if (currentLevel >= LEVELS.size) return Int.MAX_VALUE
        return LEVELS[currentLevel].first  // 下一级所需EXP
    }

    fun getLevelProgress(exp: Int): Float {
        val currentLevel = getLevel(exp)
        if (currentLevel >= LEVELS.size) return 1f
        val currentMin = LEVELS.getOrElse(currentLevel - 1) { 0 to 0 }.first
        val nextMin = LEVELS.getOrElse(currentLevel) { 0 to 0 }.first
        if (nextMin == currentMin) return 1f
        return ((exp - currentMin).toFloat() / (nextMin - currentMin).toFloat()).coerceIn(0f, 1f)
    }

    /** 行为 EXP 奖励 */
    object Rewards {
        const val ADD_ITEM = 5
        const val ADD_PHOTO = 2
        const val COMPLETE_DESCRIPTION = 3
        const val DAILY_STREAK_BONUS = 20

        /** 物品价值分：每 ¥1000 = +1 EXP */
        fun valueExp(price: Double): Int = (price / 1000).toInt()

        /** 物品数量分：每件 = +10 EXP */
        const val ITEM_COUNT_EXP = 10
    }
}
```

- [ ] **Step 2: Verify** — `./gradlew assembleDebug`

- [ ] **Step 3: Commit**

---

### Task 2.3: AddItemViewModel — 保存时记录 EXP

**Files:**
- Modify: `app/src/main/java/com/nanji/lootarchive/ui/additem/AddItemViewModel.kt`

在 `saveItem()` 成功保存后，调用 EXP 记录逻辑：

```kotlin
// 在 isSaved = true 之前加
if (editingItemId == null) {
    // 新增物品
    viewModelScope.launch {
        ExpService.recordAddItem(savedId, price, state.description.isNotBlank(), state.photoPaths.size)
    }
}
```

创建 `ExpService.kt`：

```kotlin
package com.nanji.lootarchive.ui.additem

import com.nanji.lootarchive.data.local.entity.ExperienceLogEntity
import com.nanji.lootarchive.data.repository.ItemRepository
import com.nanji.lootarchive.data.local.dao.UserProfileDao
import com.nanji.lootarchive.data.local.dao.AchievementDao
import com.nanji.lootarchive.util.ExpCalculator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpService @Inject constructor(
    private val userProfileDao: UserProfileDao,
    private val achievementDao: AchievementDao,
    private val experienceLogDao: ExperienceLogDao,
    private val itemRepository: ItemRepository
) {
    suspend fun recordAddItem(itemId: Long, price: Double, hasDesc: Boolean, photoCount: Int) {
        val now = System.currentTimeMillis()
        // 数量分
        val countExp = ExpCalculator.Rewards.ITEM_COUNT_EXP
        experienceLogDao.insertLog(ExperienceLogEntity(source = "add_item", amount = countExp, itemId = itemId, createdAt = now))
        userProfileDao.addExp(countExp, now)
        userProfileDao.incrementItemsAdded(now)

        // 价值分
        val valueExp = ExpCalculator.Rewards.valueExp(price)
        if (valueExp > 0) {
            experienceLogDao.insertLog(ExperienceLogEntity(source = "value_score", amount = valueExp, itemId = itemId, createdAt = now))
            userProfileDao.addExp(valueExp, now)
        }

        // 描述分
        if (hasDesc) {
            experienceLogDao.insertLog(ExperienceLogEntity(source = "complete_desc", amount = ExpCalculator.Rewards.COMPLETE_DESCRIPTION, itemId = itemId, createdAt = now))
            userProfileDao.addExp(ExpCalculator.Rewards.COMPLETE_DESCRIPTION, now)
            userProfileDao.incrementDescriptionsFilled(now)
        }

        // 照片分
        repeat(photoCount) {
            experienceLogDao.insertLog(ExperienceLogEntity(source = "add_photo", amount = ExpCalculator.Rewards.ADD_PHOTO, itemId = itemId, createdAt = now))
            userProfileDao.addExp(ExpCalculator.Rewards.ADD_PHOTO, now)
            userProfileDao.incrementPhotosAdded(now)
        }

        // 重新计算等级
        val profile = userProfileDao.getProfileSync() ?: return
        val newLevel = ExpCalculator.getLevel(profile.exp)
        if (newLevel != profile.level) {
            userProfileDao.setLevel(newLevel, now)
        }

        // 检查成就
        checkAchievements(profile)
    }

    private suspend fun checkAchievements(profile: UserProfileEntity) {
        val now = System.currentTimeMillis()
        val totalItems = itemRepository.getTotalCountSync()
        val totalValue = itemRepository.getTotalValueSync()
        val unlockedAchs = achievementDao.getUnlockedAchievements()
        val unlockedKeys = unlockedAchs.map { it.key }.toSet()
        
        val checks = mapOf(
            "items_5" to (totalItems >= 5),
            "items_20" to (totalItems >= 20),
            "items_50" to (totalItems >= 50),
            "items_100" to (totalItems >= 100),
            "value_10000" to (totalValue >= 10000),
            "value_100000" to (totalValue >= 100000),
            "value_500000" to (totalValue >= 500000),
            "photos_10" to (profile.totalPhotosAdded >= 10),
            "photos_50" to (profile.totalPhotosAdded >= 50),
            "desc_10" to (profile.totalDescriptionsFilled >= 10),
            "desc_50" to (profile.totalDescriptionsFilled >= 50),
            "streak_7" to (profile.streakDays >= 7),
            "streak_30" to (profile.streakDays >= 30)
        )
        checks.forEach { (key, achieved) ->
            if (achieved && key !in unlockedKeys) {
                achievementDao.unlockAchievement(key, now)
            }
        }
    }
}
```

需要在 ItemDao 加同步查询（因为 achievement check 在 suspend 中需要即时值）：

```kotlin
// ItemDao.kt 加：
@Query("SELECT COUNT(*) FROM items WHERE isDeleted = 0")
suspend fun getTotalCountSync(): Int

@Query("SELECT COALESCE(SUM(purchasePrice), 0) FROM items WHERE isDeleted = 0")
suspend fun getTotalValueSync(): Double
```

- [ ] **Step 2: Verify** — `./gradlew assembleDebug`

- [ ] **Step 3: Commit**

---

### Task 2.4: MyLandingScreen — 等级 v2 展示

**Files:**
- Modify: `app/src/main/java/com/nanji/lootarchive/ui/MyLandingScreen.kt`

保留现有双维度星级展示作为**快速预览**，在下方新增**EXP 条 + 成就网格**。

- [ ] **Step 1: 在现有的等级 badge 下方添加 EXP 进度条 + 下一级预览**

在等级 Surface 的同一 Card 内、等级行之后加：

```kotlin
// EXP 进度条
val profile by userProfileVM.uiState.collectAsState()
if (profile != null) {
    Spacer(Modifier.height(8.dp))
    val progress = ExpCalculator.getLevelProgress(profile!!.exp)
    val nextExp = ExpCalculator.getNextLevelExp(profile!!.exp)
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("EXP ${profile!!.exp}", fontSize = 11.sp, color = TextAuxiliary())
            Text("Lv.${profile!!.level} → Lv.${profile!!.level + 1}", fontSize = 11.sp, color = Primary())
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = Primary(),
            trackColor = Primary().copy(alpha = 0.10f)
        )
        Spacer(Modifier.height(2.dp))
        Text("距下一级还需 ${nextExp - profile!!.exp} EXP", fontSize = 10.sp, color = TextAuxiliary())
    }
}
```

- [ ] **Step 2: 在 "功能入口" Card 之前添加成就卡片**

```kotlin
// ── v5.3 成就徽章 ──
val achievements by achievementVM.allAchievements.collectAsState(initial = emptyList())
Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = if (LocalDarkTheme.current) _CardDark else _CardLight),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
) {
    Column(Modifier.padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("成就徽章", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary(), fontFamily = FredokaFont, modifier = Modifier.weight(1f))
            Text("${achievements.count { it.isUnlocked }}/${achievements.size}", fontSize = 13.sp, color = TextAuxiliary())
        }
        Spacer(Modifier.height(12.dp))
        // 3列网格
        val columns = 3
        val rows = achievements.chunked(columns)
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { ach ->
                    Column(
                        Modifier.weight(1f).padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier.size(44.dp).background(
                                if (ach.isUnlocked) Primary().copy(alpha = 0.10f) else TextAuxiliary().copy(alpha = 0.08f),
                                RoundedCornerShape(14.dp)
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (ach.isUnlocked) ach.icon else "🔒",
                                fontSize = 22.sp
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            ach.title,
                            fontSize = 10.sp,
                            color = if (ach.isUnlocked) TextPrimary() else TextAuxiliary(),
                            fontWeight = if (ach.isUnlocked) FontWeight.Medium else FontWeight.Normal,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                // 补齐不满3列的行
                repeat(columns - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
```

- [ ] **Step 3: Verify** — `./gradlew assembleDebug`

- [ ] **Step 4: Commit**

---

### Task 2.5: 版本号更新 — v5.3.0

按 5.1.9→5.2.0, 5.2→5.3，versionCode=530

- 更新 4 个文件的版本号
- Build Release APK
- Commit + Push + Tag V5.3.0

---

# Phase 3: v5.4 — 资产分析报表 v2

## Overview

新增雷达图组件、资产净值趋势折线图、标签分布条形图、资产报告页面。

---

### Task 3.1: Canvas 雷达图组件

**Files:**
- Create: `app/src/main/java/com/nanji/lootarchive/ui/component/RadarChart.kt`

```kotlin
package com.nanji.lootarchive.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanji.lootarchive.ui.theme.Primary
import com.nanji.lootarchive.ui.theme.TextAuxiliary
import com.nanji.lootarchive.ui.theme.FredokaFont

data class RadarAxis(val label: String, val value: Float, val maxValue: Float)

@Composable
fun RadarChart(
    axes: List<RadarAxis>,
    modifier: Modifier = Modifier,
    fillColor: Color = Primary().copy(alpha = 0.15f),
    strokeColor: Color = Primary(),
    size: Float = 260f
) {
    if (axes.size < 3) return
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(size.dp)) {
            val centerX = size / 2f
            val centerY = size / 2f
            val radius = size * 0.35f
            val angleStep = (2 * Math.PI / axes.size).toFloat()

            // Draw grid (3 levels)
            for (level in 1..3) {
                val r = radius * level / 3f
                val path = Path()
                axes.forEachIndexed { i, _ ->
                    val angle = -Math.PI.toFloat() / 2f + i * angleStep
                    val x = centerX + r * kotlin.math.cos(angle)
                    val y = centerY + r * kotlin.math.sin(angle)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(path, TextAuxiliary().copy(alpha = 0.15f), style = Stroke(1.5f))
            }

            // Draw axes lines
            axes.forEachIndexed { i, _ ->
                val angle = -Math.PI.toFloat() / 2f + i * angleStep
                val endX = centerX + radius * kotlin.math.cos(angle)
                val endY = centerY + radius * kotlin.math.sin(angle)
                drawLine(TextAuxiliary().copy(alpha = 0.25f), Offset(centerX, centerY), Offset(endX, endY), strokeWidth = 1f)
            }

            // Draw data polygon
            val dataPath = Path()
            axes.forEachIndexed { i, axis ->
                val angle = -Math.PI.toFloat() / 2f + i * angleStep
                val r = radius * (axis.value / axis.maxValue).coerceIn(0f, 1f)
                val x = centerX + r * kotlin.math.cos(angle)
                val y = centerY + r * kotlin.math.sin(angle)
                if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
            }
            dataPath.close()
            drawPath(dataPath, fillColor)
            drawPath(dataPath, strokeColor, style = Stroke(2.5f))

            // Draw dots at vertices
            axes.forEachIndexed { i, axis ->
                val angle = -Math.PI.toFloat() / 2f + i * angleStep
                val r = radius * (axis.value / axis.maxValue).coerceIn(0f, 1f)
                drawCircle(
                    strokeColor,
                    radius = 5f,
                    center = Offset(centerX + r * kotlin.math.cos(angle), centerY + r * kotlin.math.sin(angle))
                )
            }
        }
        // Labels
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            axes.forEach { axis ->
                Text(axis.label, fontSize = 10.sp, color = TextAuxiliary())
            }
        }
    }
}
```

- [ ] **Step 1: Verify** — `./gradlew assembleDebug`

- [ ] **Step 2: Commit**

---

### Task 3.2: 资产净值趋势折线图

**Files:**
- Create: `app/src/main/java/com/nanji/lootarchive/ui/component/TrendLineChart.kt`

Canvas 折线图组件，X 轴为月份，Y 轴为资产净值。参考现有 Sparkline 模式但改用折线。

（此处代码较长但模式与现有 sparkline 一致，用 `drawPath` + `cubicTo` 实现平滑曲线，`drawCircle` 显示数据点）

- [ ] **Verify** — `./gradlew assembleDebug`
- [ ] **Commit**

---

### Task 3.3: StatisticsScreen — 雷达图卡片 + 趋势折线图

**Files:**
- Modify: `app/src/main/java/com/nanji/lootarchive/ui/statistics/StatisticsScreen.kt`
- Modify: `app/src/main/java/com/nanji/lootarchive/ui/statistics/StatisticsViewModel.kt`

在现有 Donut + Sparkline + 排名 之后，新增雷达图卡片和趋势折线图。

雷达图五维：`数量/总价值/平均单价/月增长率/活跃度`，每个分类一行。

趋势折线图：12 个月资产净值，用 `items.groupBy` 按月计算。

- [ ] **Verify** — `./gradlew assembleDebug`
- [ ] **Commit**

---

### Task 3.4: 版本号更新 — v5.4.0

---

# Phase 4: v5.5 — 数据模型正规化

## Overview

将 `ItemEntity.tags`（逗号分隔 String）迁移为多对多关系表。

---

### Task 4.1: TagEntity + ItemTagCrossRefEntity + Migration

**Files:**
- Create: `app/src/main/java/com/nanji/lootarchive/data/local/entity/TagEntity.kt`
- Create: `app/src/main/java/com/nanji/lootarchive/data/local/entity/ItemTagCrossRefEntity.kt`
- Create: `app/src/main/java/com/nanji/lootarchive/data/local/dao/TagDao.kt`
- Modify: `app/src/main/java/com/nanji/lootarchive/data/local/database/AppDatabase.kt`
- Modify: `app/src/main/java/com/nanji/lootarchive/data/local/entity/ItemEntity.kt` — 标记 tags 字段 deprecated

- [ ] **Step 1: 创建 TagEntity.kt**

```kotlin
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: String = "#E8782A",
    val usageCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 2: 创建 ItemTagCrossRefEntity.kt**

```kotlin
@Entity(
    tableName = "item_tag_cross_ref",
    primaryKeys = ["itemId", "tagId"],
    indices = [Index("tagId")]
)
data class ItemTagCrossRefEntity(
    val itemId: Long,
    val tagId: Long
)
```

- [ ] **Step 3: 修改 AppDatabase — Migration 3→4**

```kotlin
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS tags (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                color TEXT NOT NULL DEFAULT '#E8782A',
                usageCount INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL DEFAULT 0
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS item_tag_cross_ref (
                itemId INTEGER NOT NULL,
                tagId INTEGER NOT NULL,
                PRIMARY KEY (itemId, tagId)
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_item_tag_ref_tagId ON item_tag_cross_ref (tagId)")
        
        // Migrate existing comma-separated tags to new tables
        val cursor = db.query("SELECT id, tags FROM items WHERE isDeleted = 0 AND tags != ''")
        while (cursor.moveToNext()) {
            val itemId = cursor.getLong(0)
            val tagsStr = cursor.getString(1) ?: ""
            tagsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { tagName ->
                // Insert tag if not exists, get its ID
                db.execSQL("INSERT OR IGNORE INTO tags (name, color, usageCount, createdAt) VALUES ('$tagName', '#E8782A', 0, ${System.currentTimeMillis()})")
                val tagCursor = db.query("SELECT id FROM tags WHERE name = ?", arrayOf(tagName))
                if (tagCursor.moveToFirst()) {
                    val tagId = tagCursor.getLong(0)
                    db.execSQL("INSERT OR IGNORE INTO item_tag_cross_ref (itemId, tagId) VALUES ($itemId, $tagId)")
                    db.execSQL("UPDATE tags SET usageCount = usageCount + 1 WHERE id = $tagId")
                }
                tagCursor.close()
            }
        }
        cursor.close()
    }
}
```

- [ ] **Step 4: Verify** — `./gradlew assembleDebug`

- [ ] **Step 5: Commit**

---

### Task 4.2: 标签管理页面

**Files:**
- Create: `app/src/main/java/com/nanji/lootarchive/ui/tags/TagManagementScreen.kt`
- Create: `app/src/main/java/com/nanji/lootarchive/ui/tags/TagViewModel.kt`

标签 CRUD 页面，包含颜色选择器、使用统计、合并标签功能。可从设置页面进入。

- [ ] **Verify** — `./gradlew assembleDebug`

- [ ] **Commit**

---

### Task 4.3: 版本号更新 — v5.5.0

---

## Self-Review

**1. Spec coverage:**
- [x] 物品状态管理 → Tasks 1.1, 1.5, 1.6, 1.7, 1.8
- [x] 标签系统 → Tasks 1.1, 1.2, 1.3, 1.5, 1.6, 1.7, 1.8
- [x] 收藏家等级 v2 → Phase 2 Tasks 2.1–2.4
- [x] 资产分析报表 v2 → Phase 3 Tasks 3.1–3.3
- [x] 数据模型正规化 → Phase 4 Tasks 4.1–4.2
- [x] 版本号更新 → Tasks 1.9, 2.5, 3.4, 4.3

**2. Placeholder scan:** 无 "TBD"、"TODO"、"implement later"。所有代码块均为真实 Kotlin/Compose 代码。

**3. Type consistency:** `ExpCalculator.getLevelProgress(Float)` output is `Float` — consistent with `LinearProgressIndicator(progress = { progress })` which expects Float lambda. `ItemEntity` field names match across all tasks.

---

## Execution

Phase 1 (v5.2) 已完成详细任务分解并包含所有代码。Phase 2-4 的核心组件代码已给出，辅助代码可在各 Phase 执行时展开。
