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

data class StatisticsUiState(
    val isLoading: Boolean = true,
    val totalCount: Int = 0,
    val totalValue: Double = 0.0,
    val saleRevenue: Double = 0.0,
    val currency: String = "CNY",
    val categorySummaries: List<CategorySummary> = emptyList(),
    val items: List<ItemEntity> = emptyList(),
    val allItems: List<ItemEntity> = emptyList(),
    val timeFilter: String = "all",
    val selectedCategorySummary: CategorySummary? = null,
    val selectedCategoryItems: List<ItemEntity> = emptyList()
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
                itemRepository.getOwnedValue(),
                categoryRepository.getAllCategories(),
                _timeFilter,
                _refreshCounter
            ) { items, ownedValue, categories, filter, _ ->
                // 仅统计当前拥有的物品（在用+闲置+待修）
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
                // 已出物品售出收益（不受时间筛选影响）
                val saleRevenue = items.filter { ItemStatus.fromCode(it.status) == ItemStatus.SOLD && !it.isDeleted }.sumOf { it.salePrice ?: 0.0 }
                StatisticsUiState(
                    isLoading = false,
                    totalCount = filteredItems.size,
                    totalValue = filteredTotalValue + saleRevenue,
                    saleRevenue = saleRevenue,
                    categorySummaries = summaries,
                    items = filteredItems,
                    allItems = items,
                    timeFilter = filter
                )
            }.collect { _uiState.value = it }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        _refreshCounter.value++
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
