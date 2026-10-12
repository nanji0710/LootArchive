package com.nanji.lootarchive.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanji.lootarchive.data.local.entity.CategoryEntity
import com.nanji.lootarchive.domain.model.ItemStatus
import com.nanji.lootarchive.data.local.entity.ItemEntity
import com.nanji.lootarchive.data.repository.CategoryRepository
import com.nanji.lootarchive.data.repository.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategorySummary(
    val category: CategoryEntity,
    val itemCount: Int,
    val totalValue: Double
)

/** 客单价分布的一个档位。 */
data class PriceBucket(val label: String, val count: Int)

/** 存放位置的金额 / 件数汇总。 */
data class LocationSummary(val name: String, val itemCount: Int, val totalValue: Double)

/** 按月聚合的一个点；[yearMonth] 形如 `2026-09`。 */
data class MonthPoint(val yearMonth: String, val value: Double)

data class StatisticsUiState(
    val isLoading: Boolean = true,
    val totalCount: Int = 0,
    val totalValue: Double = 0.0,
    /** 客单价 = 总资产 / 件数。口径与资产一致，只统计拥有物品。 */
    val avgPrice: Double = 0.0,
    // v7.1 资产总览 Hero：本月 / 上月新增购入额（口径与资产一致，只算拥有物品；不受时间筛选影响）
    val monthPurchases: Double = 0.0,
    val prevMonthPurchases: Double = 0.0,
    val currency: String = "CNY",
    val categorySummaries: List<CategorySummary> = emptyList(),
    val items: List<ItemEntity> = emptyList(),
    val timeFilter: String = "all",
    val selectedCategorySummary: CategorySummary? = null,
    val selectedCategoryItems: List<ItemEntity> = emptyList(),
    // ── v7.2 新增图表的数据源 ──
    val priceBuckets: List<PriceBucket> = emptyList(),
    val topItems: List<ItemEntity> = emptyList(),
    val locationSummaries: List<LocationSummary> = emptyList(),
    /** 近 12 个自然月的购入额，**空月补 0**（否则柱状图 x 轴不等距）。 */
    val monthlyPurchases: List<MonthPoint> = emptyList(),
    /** 从最早购入月到本月的累计资产，同样补空月。 */
    val cumulativeTrend: List<MonthPoint> = emptyList()
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    private val _timeFilter = MutableStateFlow("all")
    private var _refreshCounter = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            combine(
                itemRepository.getAllItems(),
                categoryRepository.getAllCategories(),
                _timeFilter,
                _refreshCounter
            ) { items, categories, filter, _ ->
                // 金额口径：仅当前拥有（在用/闲置/待修）物品的购入价之和；已出/丢失不计入任何金额
                val ownedItems = items.filter { ItemStatus.fromCode(it.status).isOwned }
                val now = System.currentTimeMillis()
                val cutoff = when (filter) {
                    "3months" -> now - 90L * 24 * 60 * 60 * 1000
                    "6months" -> now - 180L * 24 * 60 * 60 * 1000
                    "1year" -> now - 365L * 24 * 60 * 60 * 1000
                    else -> 0L
                }
                val filteredItems = if (cutoff == 0L) ownedItems else ownedItems.filter { (it.purchaseDate ?: 0) >= cutoff }
                val summaries = categories.map { cat ->
                    val catItems = filteredItems.filter { it.categoryId == cat.id }
                    CategorySummary(cat, catItems.size, catItems.sumOf { it.purchasePrice })
                }.filter { it.totalValue > 0 } // 只展示有金额的分类
                val filteredTotalValue = filteredItems.sumOf { it.purchasePrice }
                // 本月 / 上月新增购入额的日历边界
                val cal = java.util.Calendar.getInstance()
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                val monthStart = cal.timeInMillis
                cal.add(java.util.Calendar.MONTH, -1)
                val prevMonthStart = cal.timeInMillis
                val monthPurchases = ownedItems.filter { (it.purchaseDate ?: 0L) >= monthStart }.sumOf { it.purchasePrice }
                val prevMonthPurchases = ownedItems
                    .filter { val d = it.purchaseDate ?: 0L; d in prevMonthStart until monthStart }
                    .sumOf { it.purchasePrice }

                // ── v7.2 按月序列：补空月，x 轴才等距 ──
                //
                // 改前实测：原实现直接对「有购入的月份」做 groupBy，没买的月份整段消失，
                // 于是横轴既不是等距也不代表时间（StatisticsScreen.kt 旧 324-330 行）。
                val monthFormat = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
                val thisMonth = monthStartCalendar(System.currentTimeMillis())
                val endKey = monthKey(thisMonth.timeInMillis, monthFormat)

                // 月度购入：近 12 个自然月（含本月），从窗口起点逐月推进
                val window = LinkedHashMap<String, Double>()
                val cursor = (thisMonth.clone() as java.util.Calendar).apply { add(java.util.Calendar.MONTH, -11) }
                repeat(12) {
                    window[monthKey(cursor.timeInMillis, monthFormat)] = 0.0
                    cursor.add(java.util.Calendar.MONTH, 1)
                }

                // 累计趋势：窗口内最早购入月 → 本月
                val trend = LinkedHashMap<String, Double>()
                filteredItems.mapNotNull { it.purchaseDate }.minOrNull()?.let { first ->
                    val c = monthStartCalendar(first)
                    var guard = 0
                    // 上限 240 个月：脏数据（例如 1970 年的日期）不至于把图拉成一根直线
                    while (guard++ < 240) {
                        val k = monthKey(c.timeInMillis, monthFormat)
                        if (k > endKey) break
                        trend[k] = 0.0
                        c.add(java.util.Calendar.MONTH, 1)
                    }
                }
                filteredItems.forEach { item ->
                    val d = item.purchaseDate ?: return@forEach
                    val k = monthKey(d, monthFormat)
                    window.computeIfPresent(k) { _, old -> old + item.purchasePrice }
                    trend.computeIfPresent(k) { _, old -> old + item.purchasePrice }
                }
                var running = 0.0
                val cumulativeTrend = trend.map { (k, v) -> running += v; MonthPoint(k, running) }

                StatisticsUiState(
                    isLoading = false,
                    totalCount = filteredItems.size,
                    totalValue = filteredTotalValue,
                    avgPrice = if (filteredItems.isEmpty()) 0.0 else filteredTotalValue / filteredItems.size,
                    monthPurchases = monthPurchases,
                    prevMonthPurchases = prevMonthPurchases,
                    categorySummaries = summaries,
                    items = filteredItems,
                    timeFilter = filter,
                    priceBuckets = buildPriceBuckets(filteredItems),
                    topItems = filteredItems.sortedByDescending { it.purchasePrice }.take(5),
                    locationSummaries = buildLocationSummaries(filteredItems),
                    monthlyPurchases = window.map { (k, v) -> MonthPoint(k, v) },
                    cumulativeTrend = cumulativeTrend
                )
            }.collect { _uiState.value = it }
        }
    }

    fun setTimeFilter(filter: String) {
        _uiState.update { it.copy(isLoading = true) }
        _timeFilter.value = filter
        _refreshCounter.value++ // 防止重复点击同一按钮导致 loading 卡死
    }

    fun selectCategorySummary(summary: CategorySummary) {
        val catItems = _uiState.value.items.filter { it.categoryId == summary.category.id }
        _uiState.update { it.copy(selectedCategorySummary = summary, selectedCategoryItems = catItems) }
    }

    fun dismissCategoryDetail() {
        _uiState.update { it.copy(selectedCategorySummary = null, selectedCategoryItems = emptyList()) }
    }
}

// ══════════════════════════════════════════════════════════════
//  v7.2 新图表的数据整理（纯函数，不碰 state）
// ══════════════════════════════════════════════════════════════

/** 客单价分布的五档标签。 */
private val BucketLabels = listOf("<¥500", "¥500-1K", "¥1-3K", "¥3-10K", "≥¥10K")

/** 档位下界（不含上界）；最后一档无上界。 */
private val BucketBounds = listOf(500.0, 1_000.0, 3_000.0, 10_000.0)

/** 客单价分布：五档直方图。 */
private fun buildPriceBuckets(items: List<ItemEntity>): List<PriceBucket> {
    val counts = IntArray(BucketLabels.size)
    items.forEach { item ->
        var i = 0
        while (i < BucketBounds.size && item.purchasePrice >= BucketBounds[i]) i++
        counts[i]++
    }
    return BucketLabels.mapIndexed { i, label -> PriceBucket(label, counts[i]) }
}

/** 存放位置分布：按金额降序；位置没填的并成一组，不丢件。 */
private fun buildLocationSummaries(items: List<ItemEntity>): List<LocationSummary> =
    items.groupBy { item ->
        val loc = item.storageLocation?.trim()
        if (loc.isNullOrEmpty()) "未填写位置" else loc
    }
        .map { (name, list) -> LocationSummary(name, list.size, list.sumOf { it.purchasePrice }) }
        .sortedByDescending { it.totalValue }

/** 把某个时刻归到它所在自然月的 1 号 0 点。 */
private fun monthStartCalendar(millis: Long): java.util.Calendar =
    java.util.Calendar.getInstance().apply {
        timeInMillis = millis
        set(java.util.Calendar.DAY_OF_MONTH, 1)
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }

/** `2026-09` 形式的月份键；字符串比较即时间先后。 */
private fun monthKey(millis: Long, format: java.text.SimpleDateFormat): String =
    format.format(java.util.Date(millis))
